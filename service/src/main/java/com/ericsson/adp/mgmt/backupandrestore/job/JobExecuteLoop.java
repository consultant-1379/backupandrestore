/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionFileService;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.CMMediatorService;
import com.ericsson.adp.mgmt.backupandrestore.rest.action.CreateActionRequest;
import com.ericsson.adp.mgmt.backupandrestore.util.ExceptionUtils;

/**
 * A class responsible for executing jobs
 */
public class JobExecuteLoop implements Runnable{

    private final Logger log = LogManager.getLogger(JobExecuteLoop.class);

    private final BlockingDeque<JobQueueItem> mainQueue;
    private final BlockingDeque<JobQueueItem> sftpServerJobQueue;
    private final JobFactory jobFactory;
    private final ActionRepository actionRepository;
    private final ActionFileService actionFileService;
    private final CMMediatorService cmMediatorService;
    private final CopyOnWriteArrayList<Job> runningJobs;

    /**
     * Creates an instance of the JobExeceuteLoop runnable responsible for executing jobs
     * @param mainQueue the queue where the main actions are added to.
     * @param sftpServerQueue the queue where the post exec actions are added to.
     * @param jobFactory the job factory
     * @param actionRepository the action repository
     * @param cmMediatorService the CM mediator service
     * @param actionFileService the action file service
     * @param runningJobs the list of jobs running on the executor
     */
    public JobExecuteLoop(final BlockingDeque<JobQueueItem> mainQueue,
                          final BlockingDeque<JobQueueItem> sftpServerQueue,
                          final JobFactory jobFactory,
                          final ActionRepository actionRepository,
                          final CMMediatorService cmMediatorService,
                          final ActionFileService actionFileService,
                          final CopyOnWriteArrayList<Job> runningJobs) {
        super();
        this.mainQueue = mainQueue;
        this.sftpServerJobQueue = sftpServerQueue;
        this.jobFactory = jobFactory;
        this.actionRepository = actionRepository;
        this.cmMediatorService = cmMediatorService;
        this.actionFileService = actionFileService;
        this.runningJobs = runningJobs;
    }

    /**
     * Execute loop to run in the execution thread. Execution thread will exit in the event it is interrupted while
     * waiting for an item to be added to the execute queue. The rest of the executor is considered responsible for
     * restarting the execution thread, should that be necessary.
     *
     * This loop effectively implements a busy wait on a blocking queue, which deserves some explanation. The JobExecutor
     * interface (and BRO more broadly) makes a guarantee of rejecting any action passed to JobExecutor::execute in the
     * event a job is currently running. To implement this, while also supporting the running of post-execution actions
     * returned from Job::run, this implementation checks to see if a job is on the queue, and if one is, takes a reference
     * to it but DOES NOT remove it from the queue, in order to ensure calls to queue.isEmpty() return false
     * while the job is running. This is a peek, for which there is no blocking implementation in any standard library
     * thread-safe Dequeue, so we're forced to peek and if it returns null, just wait a bit and then try again. Solutions
     * involving condition variables or locks were considered, but the danger of subtle deadlocking and the general
     * complexity of implementation didn't seem worth the benefits, which, for clarity, are some cpu cycles saved every
     * 10ms and a maximum latency of <10ms from an Action being successfully added to the queue to it being executed.
     * */
    @Override
    public void run() {
        log.info("Execute loop started");
        boolean running = true;
        while (running) {
            final JobQueueItem jobInfo = mainQueue.peek();
            if (jobInfo != null) { // If there was an action in the queues
                final Optional<Job> newJob = ExceptionUtils.tryCatch(
                    () -> jobFactory.createJob(jobInfo.getManager(), jobInfo.getAction()),
                    e -> log.error("Failed to construct job for action {}, failing", jobInfo.getAction().getActionId(), e)
                );
                newJob.ifPresent(toRun -> {
                    runningJobs.add(toRun);
                    log.info("The action {} is running on {}. The running actions on all the threads are {}", toRun.action.getActionId(),
                              Thread.currentThread().getName(), runningActions());
                    final List<CreateActionRequest> postExecutionActions = new ArrayList<>();
                    ExceptionUtils.tryCatch(toRun::run, e -> log.error("Failed to run action", e)).ifPresent(postExecutionActions::addAll);
                    ExceptionUtils.tryCatch(
                        () -> handlePostExecutionActions(postExecutionActions, toRun.backupManager),
                        e -> log.error("Failed to handle post-execution actions", e)
                    );
                    ExceptionUtils.tryCatch(toRun::completeAction, e -> log.warn("Failed to mark action as complete", e));
                });
                // Note: the following order of operations is: make BRO able to accept actions by removing the old one from the queue
                // (assuming there are no post-execution actions), then mark BRO as available by setting currentJob to an empty optional.
                // So FIRST BRO is available, THEN we say we're available
                if (!mainQueue.remove(jobInfo)) { // Failure here is *extremely* surprising, so log it as a warning
                    log.warn("Expected to remove job {} from the execution queue, but it wasn't there", jobInfo.getAction().getActionId());
                }
                newJob.ifPresent(runningJobs::remove);
                log.info("The action {} is now removed from the execution queue. The actions running on all the threads are {}",
                        newJob.isPresent() ?  newJob.get().getActionId() : "",
                                runningActions());
            } else {
                running = sleep();
            }
        }
    }

    private String runningActions() {
        final Iterable<Job> currentRunningJobs = () -> runningJobs.iterator();
        String runningActions = StreamSupport.stream(currentRunningJobs.spliterator(), false)
                                            .map(Job::getActionId)
                                            .collect(Collectors.joining(","));
        runningActions = runningActions.isEmpty() ? "none" : runningActions;
        return runningActions;
    }

    private void persistAcceptedAction(final Action action, final BackupManager manager) {
        if (!action.isPartOfHousekeeping()) {
            cmMediatorService.enqueueProgressReport(action);
        }
        // Action won't be persisted if it fails to push to CMM
        manager.addAction(action);
        actionFileService.writeToFile(action);
    }

    /**
     * As of now, only the Scheduled Create Backup Job has post execution actions which is the auto-export.
     * This method ensures that the auto-export job is enqueued into the SFTP Server queue.
     * @param requests the action requests
     * @param manager the backup manager
     */
    private void handlePostExecutionActions(final List<CreateActionRequest> requests, final BackupManager manager) {
        requests.stream()
                .map(r -> actionRepository.createAction(manager, r))
                .peek(a -> persistAcceptedAction(a, manager))
                .map(a -> new JobQueueItem(manager, a))
                .forEach(sftpServerJobQueue::add);
    }

    // Sleep for 10ms, and return true if not interrupted
    private boolean sleep() {
        try {
            Thread.sleep(10);
            return true;
        } catch (final InterruptedException exception) {
            log.warn(String.format("Execution thread interrupted, exiting: %s", exception.getMessage()));
            Thread.currentThread().interrupt();
            return false;
        }
    }

}
