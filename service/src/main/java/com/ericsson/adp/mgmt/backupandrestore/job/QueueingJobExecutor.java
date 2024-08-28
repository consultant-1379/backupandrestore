/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.job;

import static com.ericsson.adp.mgmt.backupandrestore.action.ResultType.FAILURE;
import static com.ericsson.adp.mgmt.backupandrestore.job.ImportRestorePredicatesUtils.ifSameBRMdifferentBackupName;
import static com.ericsson.adp.mgmt.backupandrestore.job.ImportRestorePredicatesUtils.isValidImportRestore;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionFileService;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionRepository;
import com.ericsson.adp.mgmt.backupandrestore.action.ResultType;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.exception.AnotherActionRunningException;
import com.ericsson.adp.mgmt.backupandrestore.util.ExceptionUtils;

/**
 * Queuing Implementation of JobExecutor.
 * Functions:
 *  - Multiple actions can now by queued to occur in order
 *  - Ensures only one job is running at a time (job lock handling no longer necessary)
 *  - Supports "tail call" behaviour - jobs queuing other actions after they finish, e.g. housekeeping, auto_export
 *  - Ensures "tail call" actions executed before any unrelated queued actions
 * */
@Service
public class QueueingJobExecutor implements JobExecutor {

    private final Logger log = LogManager.getLogger(QueueingJobExecutor.class);

    // The mainQueue has the CREATE_BACKUP, RESTORE, DELETE and HOUSEKEEPING actions
    private final BlockingDeque<JobQueueItem> mainQueue;

    // The sftpServerJobQueue has the IMPORT and EXPORT actions
    private final BlockingDeque<JobQueueItem> sftpServerJobQueue;
    private final CopyOnWriteArrayList<Job> runningJobs;
    private final AtomicBoolean isLimitedParallelActionsEnabled;
    private Thread mainThread;
    private Thread sftpServerJobThread;
    private JobFactory jobFactory;
    private final ActionRepository actionRepository;
    private ActionFileService actionFileService;
    private CMMediatorService cmMediatorService;

    /**
     * Constructor used by spring boot to construct a QueueingJobExecutor bean.
     * The dependencies are injected through the constructor to ensure
     * they are initiated before they are passed to the JobExecuteLoop.
     * There is a dependency cycle between JobExecutor and JobFactory, that is:
     *  ActionService -> JobExecutor -> JobFactory -> ActionService,
     * and Spring does not allow this. To get around this issue, the @Lazy annotation
     * is used for the JobFactory. This will create a proxy for the JobFactory bean
     * when the QueueingJobExecutor is instantiated. The actual JobFactory bean will
     * be fully created when it's first needed.
     * @param jobFactory the job factory
     * @param actionRepository the action repository
     * @param actionFileService the action file service
     * @param cmMediatorService the CM mediator service
     * @param isLimitedParallelActionsEnabled the flag to enable limited parallel actions
     */
    @Autowired
    public QueueingJobExecutor(@Lazy final JobFactory jobFactory,
                              final ActionRepository actionRepository,
                              final ActionFileService actionFileService,
                              final CMMediatorService cmMediatorService,
                              @Value("${flag.enable.enableLimitedParallelActions:false}") final boolean isLimitedParallelActionsEnabled) {
        this.jobFactory = jobFactory;
        this.actionRepository = actionRepository;
        this.actionFileService = actionFileService;
        this.cmMediatorService = cmMediatorService;
        runningJobs = new CopyOnWriteArrayList<>();
        this.isLimitedParallelActionsEnabled = new AtomicBoolean(isLimitedParallelActionsEnabled);
        mainQueue = new LinkedBlockingDeque<>();

        if (this.isLimitedParallelActionsEnabled.get()) {
           /**
            * Create an additional queue and thread which will handle the import/export action.
            * This means, any auto-export is added to the sftpServerJobqueue.
            */
            sftpServerJobQueue = new LinkedBlockingDeque<>();
            mainThread = new Thread(getExecuteLoop(mainQueue, sftpServerJobQueue));
            sftpServerJobThread = new Thread(getExecuteLoop(sftpServerJobQueue, sftpServerJobQueue));
            sftpServerJobThread.start();
        } else {
            /**
             * Only create one main thread which will handle one action at a time.
             * In this case both the primary and post-exec actions are added to the mainQueue.
             */
            mainThread = new Thread(getExecuteLoop(mainQueue, mainQueue));
            sftpServerJobQueue = null;
            sftpServerJobThread = null;
        }
        mainThread.start();
    }

    /**
     * Creates a JobExecuteLoop runnable
     * @param queue the queue where the primary actions are to be picked up from by the execution thread.
     * @param postExecActionsJobQueue the queue where any post-exec actions (ie the auto-export actions) are put into.
     * @return a JobExecuteLoop runnable.
     */
    private JobExecuteLoop getExecuteLoop(final BlockingDeque<JobQueueItem> queue,
                                          final BlockingDeque<JobQueueItem> postExecActionsJobQueue) {
        return new JobExecuteLoop(queue,
                postExecActionsJobQueue,
                this.jobFactory,
                this.actionRepository,
                this.cmMediatorService,
                this.actionFileService,
                this.runningJobs);
    }

    /**
     * Adds a job to the execution queue.
     * */
    @Override
    public void execute(final BackupManager backupManager, final Action action) {
        if (isLimitedParallelActionsEnabled.get()) {
            queueParallelAction(backupManager, action);
        } else {
            queueAction(backupManager, action);
        }
    }

    /**
     * Immediately start executing a job and waits for it to finish. Using this method is considered UNSAFE, as it
     * breaks this executors guarantee of priority ordered in series execution, and does not modify or respect state of
     * the execution thread. Code using this method should be refactored to make use of the priority-based queue ordering.
     *
     * ExecuteAndWait is safe (as in the caller can know no other job is running while the action given to ExecuteAndWait is),
     * only if it is called from the job execution thread (e.g. in the case of housekeeping). Use in all other
     * circumstances should be considered a bug.
     *
     * Ideally all calls to executeAndWait could be replaced by having a job which requires other actions to run to return
     * those actions, in which case this executor guarantees the returned list of actions will be executed in the order
     * provided.
     *
     * @deprecated as considered unsafe. Kept around as used by housekeeping
     * */
    @Override
    @Deprecated(since = "05/11/2020")
    public ResultType executeAndWait(final BackupManager backupManager, final Action action) {
        ResultType result = FAILURE;
        persistAcceptedAction(action, backupManager);
        final Job jobWaiting = jobFactory.createJob(backupManager, action);
        try {
            final Thread runningThread = new Thread(jobWaiting::run);
            runningThread.start();
            runningThread.join();
            result = jobWaiting.getAction().getResult();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        ExceptionUtils.tryCatch(jobWaiting::completeAction, e -> log.error("Failed to mark action {} as complete", action.getActionId(), e));
        return result;
    }

    /**
     * Do nothing implementation of interface method. Does nothing as the executor internally manages which job is
     * currently running
     *
     * @param currentJob - unused
     *
     * @deprecated as executor internally handles which job is running
     * */
    @Override
    @Deprecated
    public void setCurrentJob(final Optional<Job> currentJob) {
        // Do nothing method as the executor decides what the current job is
        // This is simply to maintain the executor interface
    }

    /**
     * Checks the actions queue, and, if it's empty, adds an action to it. If it isn't empty, throws
     * an exception indicating only one action may be run at a time. Synchronized to avoid multiple threads
     * calling pendingActions.isEmpty(), getting "true" and going on to add to the Queue at the same time.
     *
     * If you're reading this as part of adding support for true action queueing, it should be as easy as removing
     * "synchronized" and the isEmpty() check (and related failure code) from this method. executeLoop() should be
     * refactored as well, but in terms of pure functionality this is what stops multiple actions from queueing, the
     * structure of executeLoop just helps guarantee it by letting you use the action queue like a lock, with isEmpty()
     * having a similar function as Lock.tryLock().
     * */
    private synchronized void queueAction(final BackupManager manager, final Action newAction) {
        if (mainQueue.isEmpty()) {
            acceptAction(manager, newAction, mainQueue);
        } else {
            rejectAction(newAction);
        }
    }

    /**
     * Checks the actions in the queues, and, if they're empty, adds an action to one of the queues.
     * An action can be queued in one of the two queues:
     * The mainQueue will accept CREATE_BACKUP, RESTORE, HOUSEKEEPING and DELETE actions
     * The sftpServerQueue will accept the IMPORT and EXPORT actions.
     *
     * An action will be queued if any of the following conditions are met:
     * 1. Both of the queues are empty
     * 2. If the main queue is running a CREATE_BACKUP action, then an empty sftpServer queue can accept
     *    an EXPORT action that is for the same backup manager but targeted on a different backup
     * 3. If the main queue is running a RESTORE action, then an empty sftpServer queue can accept
     *    an IMPORT action which is for the same backup manager but addressed to a different backup
     *    an IMPORT action which is for a different backup manager except for same name backup shared between
     *    parent BRM and child vBRM or Config BRM and its targeted BRM.
     * 4. If the sftp server queue is running an EXPORT action, then an empty main queue can accept
     *    a CREATE_BACKUP action for the same backup manager but targeted on a different backup
     * 5. If the sftp server queue is running an IMPORT action, then an empty main queue can accept
     *    a RESTORE action which is for the same backup manager but addressed to a different backup
     *    a RESTORE action which is for a different backup manager except for same name backup shared between
     *    parent BRM and child vBRM or Config BRM and its targeted BRM.
     * Otherwise, an action will be rejected and this method throws an
     * exception indicating only one action may be run at a time.
     *
     * Synchronized to avoid race condition where multiple threads call queue.isEmpty(), both getting "true"
     * and therefore adding an action to the queue at the same time.
     *
     * */
    private synchronized void queueParallelAction(final BackupManager manager, final Action newAction) {
        if (mainQueue.isEmpty() && sftpServerJobQueue.isEmpty()) {
            acceptParallelAction(manager, newAction);
            return;
        }
        if (mainQueueEmptysftpQueueNotEmpty (manager, newAction)) {
            return;
        }
        if (sftpQueueEmptyMainQueueNotEmpty (manager, newAction)) {
            return;
        }
        rejectAction(newAction);
    }

    private boolean mainQueueEmptysftpQueueNotEmpty(final BackupManager manager, final Action newAction) {
        if (mainQueue.isEmpty() && !sftpServerJobQueue.isEmpty()) {
            if (isValidParallelCreateBackup(sftpServerJobQueue.peek().getAction(), newAction)
                    || isValidImportRestore.test(sftpServerJobQueue.peek().getAction(), newAction)) {
                acceptParallelAction(manager, newAction);
                return true;
            }
        }
        return false;
    }

    private boolean sftpQueueEmptyMainQueueNotEmpty(final BackupManager manager, final Action newAction) {
        if (!mainQueue.isEmpty() && sftpServerJobQueue.isEmpty()) {
            if (isValidParallelExport(mainQueue.peek().getAction(), newAction) ||
                    isValidImportRestore.test(mainQueue.peek().getAction(), newAction)) {
                acceptParallelAction(manager, newAction);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the new action is a valid parallel 'export' to the running action
     * @param runningAction the running action
     * @param newAction the new action can be run in parallel
     * @return true if the new action can be run in parallel, false otherwise
     */
    private boolean isValidParallelExport(final Action runningAction, final Action newAction) {
        return isCreateBackup(runningAction) && newAction.isExport() &&
                (ifSameBRMdifferentBackupName.test(runningAction, newAction));
    }

    /**
     * Checks if the new action is a valid parallel 'create backup' to the running action
     * @param runningAction the running action
     * @param newAction the new action can be run in parallel
     * @return true if the new action can be run in parallel, false otherwise
     */
    private boolean isValidParallelCreateBackup(final Action runningAction, final Action newAction) {
        return isExport(runningAction) && newAction.isCreateBackup() &&
                (ifSameBRMdifferentBackupName.test(runningAction, newAction));
    }

    /**
     * Adds the new action to the right queue
     * If it is an IMPORT/EXPORT then add to the sftpServerJobQueue,
     * Otherwise, add to the mainQueue.
     * @param manager the backup manager
     * @param newAction the new action
     */
    private void acceptParallelAction(final BackupManager manager, final Action newAction) {
        if (newAction.isImportOrExport()) {
            acceptAction(manager, newAction, sftpServerJobQueue);
        } else {
            acceptAction(manager, newAction, mainQueue);
        }
    }

    private boolean isCreateBackup(final Action currentAction) {
        return currentAction != null && currentAction.isCreateBackup();
    }

    private boolean isExport(final Action currentAction) {
        return currentAction != null && currentAction.isExport();
    }

    private void acceptAction(final BackupManager manager, final Action action, final BlockingDeque<JobQueueItem> queue) {
        persistAcceptedAction(action, manager);
        final JobQueueItem queuedAction = new JobQueueItem(manager, action);
        queue.add(queuedAction);
        restartExecutionIfNecessary();
    }

    private void rejectAction(final Action newAction) {
        String runningActionIds = getRunningJobs().stream()
                                            .map(Job::getActionId)
                                            .collect(Collectors.joining(","));
        if (!runningActionIds.isEmpty()) {
            runningActionIds = String.format("[%s] ", runningActionIds);
        }
        final String failureMessage = newAction.getName() +
                " action did not execute because another action " + runningActionIds + "is already running. "
                + (isLimitedParallelActionsEnabled.get() ? newAction.getName() +
                " cannot be executed in parallel as a conflict scenario was detected." : "");
        throw new AnotherActionRunningException(failureMessage);
    }

    /**
     * Check whether the execution threads are running. If not, restart them. This assumes the execution threads should
     * always be running, as long as BRO is attempting to execute jobs.
     * */
    private synchronized void restartExecutionIfNecessary() {
        if (!mainThread.isAlive()) {
            mainThread = new Thread(getExecuteLoop(mainQueue,
                                                   isLimitedParallelActionsEnabled.get() ? sftpServerJobQueue : mainQueue));
            mainThread.start();
        }
        if (this.isLimitedParallelActionsEnabled.get() && !sftpServerJobThread.isAlive()) {
            sftpServerJobThread = new Thread(getExecuteLoop(sftpServerJobQueue, sftpServerJobQueue));
            sftpServerJobThread.start();
        }
    }

    private void persistAcceptedAction(final Action action, final BackupManager manager) {
        if (!action.isPartOfHousekeeping()) {
            cmMediatorService.enqueueProgressReport(action);
        }
        // Action won't be persisted if it fails to push to CMM
        manager.addAction(action);
        actionFileService.writeToFile(action);
    }

    @Override
    public List<Job> getRunningJobs() {
        final Iterable<Job> currentRunningJobs = () -> runningJobs.iterator();
        return StreamSupport.stream(currentRunningJobs.spliterator(), false)
                                            .collect(Collectors.toList());
    }

    @Override
    public void setJobFactory(final JobFactory jobFactory) {
        this.jobFactory = jobFactory;
    }

    @Override
    public void setActionFileService(final ActionFileService actionFileService) {
        this.actionFileService = actionFileService;
    }

    @Override
    public void setCmMediatorService(final CMMediatorService cmMediatorService) {
        this.cmMediatorService = cmMediatorService;
    }
}