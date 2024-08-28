/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************/
package com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext;

import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.CREATE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.DELETE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.EXPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.IMPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.RESTORE;
import static com.ericsson.adp.mgmt.backupandrestore.backup.BackupStatus.INCOMPLETE;
import static com.ericsson.adp.mgmt.backupandrestore.backup.Ownership.OWNED;
import static com.ericsson.adp.mgmt.backupandrestore.backup.Ownership.READABLE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.backup.Ownership;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.job.Job;
import com.ericsson.adp.mgmt.backupandrestore.job.JobExecutor;
import com.ericsson.adp.mgmt.backupandrestore.job.ResetConfigJob;


/**
 * A service responsible for determining the tasks
 * that can be run on a backup manager
 */
@Service
public class AvailableTasksService {

    public static final Set<ActionType> NO_AVAILABLE_TASK = Collections.emptySet();
    private static final Set<ActionType> ALL_TASKS = Set.of(CREATE_BACKUP, EXPORT, DELETE_BACKUP, IMPORT, RESTORE);

    private JobExecutor jobExecutor;
    private BackupManagerRepository backupManagerRepository;
    private final AtomicBoolean isLimitedParallelActionsEnabled = new AtomicBoolean(false);

    private Set<String> getAvailableBackups(final String brmId, final Ownership ownership) {
        return backupManagerRepository.getBackupManager(brmId)
                .getBackups(ownership).stream()
                .filter(backup -> !backup.getStatus().equals(INCOMPLETE))
                .filter(backup -> backup.getActionLock().isEmpty())
                .map(backup -> backup.getBackupId())
                .collect(Collectors.toSet());
    }

    private Set<ActionType> getAvailableTasksByBRMBackups(final String brmId,
                                                          final Collection<String> ownedBackups,
                                                          final Collection<String> parentBackups) {
        if (!ownedBackups.isEmpty()) {
            return ALL_TASKS;
        }
        return getAvailableTasksForVirtualBRM(brmId, parentBackups);
    }

    private Set<ActionType> getAvailableTasksForVirtualBRM(final String brmId, final Collection<String> parentBackups) {
        return isVirtualBRM(brmId) && !parentBackups.isEmpty() ? Set.of(CREATE_BACKUP, IMPORT, RESTORE) : Set.of(CREATE_BACKUP, IMPORT);
    }

    private boolean isVirtualBRM(final String brmId) {
        return backupManagerRepository.getBackupManager(brmId).isVirtual();
    }

    private boolean isBroBRM(final String brmId) {
        return brmId.endsWith(ResetConfigJob.RESET_BRM_SUFFIX);
    }

    private Set<ActionType> getAvailableActionsByBRMType(final String brmId) {
        return isBroBRM(brmId) ? Set.of(RESTORE) : ALL_TASKS;
    }

    private Set<ActionType> getAvailableParallelActionsOnDifferentBRM(final String brmId, final Action runningAction) {
        final ActionType running = runningAction.getName();
        final boolean isBroBrmOfRunningBrm = isBroBRMAndParentBRMPair(brmId, runningAction.getBackupManagerId());
        if (running.equals(IMPORT) && !isBroBrmOfRunningBrm) {
            return Set.of(RESTORE);
        }

        final boolean isParentBRMOfRunningBroBrm = isBroBRMAndParentBRMPair(runningAction.getBackupManagerId(), brmId);
        if (running.equals(RESTORE) && !isParentBRMOfRunningBroBrm) {
            return Set.of(IMPORT);
        }

        return NO_AVAILABLE_TASK;
    }

    private boolean isBroBRMAndParentBRMPair(final String broBrm, final String parentBrm) {
        return isBroBRM(broBrm) && broBrm.substring(0, broBrm.indexOf(ResetConfigJob.RESET_BRM_SUFFIX)).equals(parentBrm);
    }

    private Set<ActionType> getAvailableParallelActionsOnSameBRM(final Action runningAction) {
        switch (runningAction.getName()) {
            case CREATE_BACKUP:
                return Set.of(EXPORT);
            case EXPORT:
                return Set.of(CREATE_BACKUP);
            case IMPORT:
                return Set.of(RESTORE);
            case RESTORE:
                return Set.of(IMPORT);
            default:
                return NO_AVAILABLE_TASK;
        }
    }

    private Set<ActionType> getAvailableParallelActions(final String brmId, final Job runningJob) {
        return runningJob.getBackupManagerId().equals(brmId) ?
                getAvailableParallelActionsOnSameBRM(runningJob.getAction()) :
                    getAvailableParallelActionsOnDifferentBRM(brmId, runningJob.getAction());
    }

    private Set<ActionType> getAvailableActionsByRunningActions(final String brmId) {
        final List<Job> runningJobs = jobExecutor.getRunningJobs();

        if (runningJobs.size() > 1) {
            return NO_AVAILABLE_TASK;
        }

        if (runningJobs.size() == 1) {
            return isLimitedParallelActionsEnabled.get() ? getAvailableParallelActions(brmId, runningJobs.get(0)) : NO_AVAILABLE_TASK;
        }

        return ALL_TASKS;
    }

    private List<AvailableTask> createAvailableTasks(final Collection<ActionType> availableActions,
                                                  final Collection<String> ownedBackups,
                                                  final Collection<String> parentBackups) {
        final List<AvailableTask> availableTasks = new ArrayList<>();
        availableActions.stream().forEach(name -> {
            switch (name) {
                case RESTORE:
                    parentBackups.stream().forEach(addToAvailableTasks(name, availableTasks));
                    ownedBackups.stream().forEach(addToAvailableTasks(name, availableTasks));
                    break;
                case DELETE_BACKUP:
                case EXPORT:
                    ownedBackups.stream().forEach(addToAvailableTasks(name, availableTasks));
                    break;
                default:
                    availableTasks.add(new AvailableTask(name));
                    break;
            }
        });
        return availableTasks;
    }

    private Consumer<? super String> addToAvailableTasks(final ActionType name, final List<AvailableTask> availableTasks) {
        return backup -> availableTasks.add(new AvailableTask(name, backup));
    }

    /**
     * Wrapper class for an action type and an optional backup
     * that the action can be applied to.
     */
    public static class AvailableTask {
        private final ActionType name;
        private Optional<String> backup;

        /**
         * Create an instance of AvailableTask
         * @param name the type of the action
         * @param backup the backup for which the action can be applied to.
         */
        public AvailableTask(final ActionType name, final String backup) {
            this.name = name;
            this.backup = Optional.ofNullable(backup);
        }

        /**
         * Create an instance of AvailableTask
         * @param name the type of the action
         */
        public AvailableTask(final ActionType name) {
            this(name, null);
        }

        public ActionType getName() {
            return name;
        }

        public Optional<String> getBackup() {
            return backup;
        }

        @Override
        public int hashCode() {
            return Objects.hash(backup, name);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final AvailableTask other = (AvailableTask) obj;
            return Objects.equals(backup, other.backup) && name == other.name;
        }
    }

    /**
     * Gets the available tasks that the backup manager can run.
     * @param brmId the backup manager id
     * @return the list of available tasks that the backup manager can run.
     */
    public List<AvailableTask> getAvailableTasks(final String brmId) {
        final Set<ActionType> availableActions = new HashSet<>(ALL_TASKS);

        final Set<String> ownedBackups = getAvailableBackups(brmId, OWNED);
        final Set<String> parentBackups = getAvailableBackups(brmId, READABLE);
        parentBackups.removeAll(ownedBackups);

        availableActions.retainAll(getAvailableTasksByBRMBackups(brmId, ownedBackups, parentBackups));
        availableActions.retainAll(getAvailableActionsByBRMType(brmId));
        availableActions.retainAll(getAvailableActionsByRunningActions(brmId));

        return createAvailableTasks(availableActions, ownedBackups, parentBackups);
    }

    @Autowired
    public void setJobExecutor(final JobExecutor jobExecutor) {
        this.jobExecutor = jobExecutor;
    }

    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

    /**
     * Sets the flag for limited parallel actions
     * @param isLimitedParallelActionsEnabled the limited parallel actions settings
     */
    @Value("${flag.enable.enableLimitedParallelActions:false}")
    public void setIsLimitedParallelActionsEnabled(final boolean isLimitedParallelActionsEnabled) {
        this.isLimitedParallelActionsEnabled.set(isLimitedParallelActionsEnabled);
    }
}
