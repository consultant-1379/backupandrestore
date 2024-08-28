/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.action;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.notification.NotificationService;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;

/**
 * Responsible for persisting and retrieving actions.
 */
@Service
public class ActionRepository {

    private static final Logger logger = LogManager.getLogger(ActionRepository.class);

    private ActionFactory actionFactory;
    private ActionFileService actionFileService;
    private CMMediatorService cmMediatorService;
    private NotificationService notificationService;
    private BackupManagerRepository backupManagerRepository;
    private int maxActions = 100;


    /**
     * Creates and persists an Action.
     * @param backupManager owner of action.
     * @param request containing action information.
     * @return action.
     */
    public Action createAction(final BackupManager backupManager, final CreateActionRequest request) {
        return actionFactory.createAction(backupManager, request);
    }

    /**
     * Gets actions for a backupManager
     * @param backupManagerId owner of actions
     * @return list of actions
     */
    public List<Action> getActions(final String backupManagerId) {
        return actionFileService
                .getActions(backupManagerId)
                .stream()
                .map(persistedAction -> new Action(persistedAction, backupManagerId, this::persist))
                .sorted(Comparator.comparing(Action::getStartTime))
                .collect(Collectors.toList());
    }

    /**
     * Persists actions.
     * @param action to be persisted.
     */
    protected void persist(final Action action) {
        actionFileService.writeToFile(action);
        if (shouldPersistActionInCM(action) && !action.isPartOfHousekeeping()) {
            cmMediatorService.enqueueProgressReport(action);
        }
    }

    /**
     *Performs a cleanup across all backupManagers and BRMS
     */
    public void performActionCleanup() {
        logger.info("Performing action cleanup");
        backupManagerRepository.getBackupManagers()
                .stream()
                .map(BackupManager::getActions)
                .filter(actions -> actions.size() > 1)
                .flatMap(Collection::stream)
                .sorted(Comparator.comparing(Action::getStartTime).reversed())
                .skip(maxActions)
                .forEach(a -> {
                    actionFileService.performCleanup(a);
                    backupManagerRepository.getBackupManager(a.getBackupManagerId()).removeAction(a);
                    logger.debug("Deleting persisted file <{}>", a.getAdditionalInfo());
                });
        actionFileService.createDummyFile();
    }

    /**
     * Fail the action if already running. Writes updated state to persistence layer, and returns true, if status was
     * updated.
     * @param action object
     * @return updated action object
     */
    public Action failActionIfRunning(final Action action) {
        if (action.getState().equals(ActionStateType.RUNNING)) {
            action.setState(ActionStateType.FINISHED);
            action.setResult(ResultType.FAILURE);
            action.setCompletionTime(OffsetDateTime.now());
            actionFileService.writeToFile(action);
            if (ActionType.RESTORE.equals(action.getName()) || ActionType.CREATE_BACKUP.equals(action.getName())) {
                notificationService.notifyAllActionFailed(action);
            }
        }
        return action;
    }

    /**
     * Adds a progress report to CM.
     * @param action to be added.
     */
    public void enqueueProgressReport(final Action action) {
        cmMediatorService.enqueueProgressReport(action);
    }

    private boolean shouldPersistActionInCM(final Action action) {
        return !ActionType.RESTORE.equals(action.getName()) || ActionStateType.FINISHED.equals(action.getState());
    }

    @Autowired
    public void setActionFactory(final ActionFactory actionFactory) {
        this.actionFactory = actionFactory;
    }

    @Autowired
    public void setActionFileService(final ActionFileService actionFileService) {
        this.actionFileService = actionFileService;
    }

    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

    @Autowired
    public void setCmMediatorService(final CMMediatorService cmMediatorService) {
        this.cmMediatorService = cmMediatorService;
    }

    @Autowired
    public void setNotificationService(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Value("${bro.actions.maxStoredActions:100}")
    public void setMaxActions(final int maxActions) {
        this.maxActions = maxActions;
    }
}
