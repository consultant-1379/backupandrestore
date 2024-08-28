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
package com.ericsson.adp.mgmt.backupandrestore.grpc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.backup.BackupMetadataWriter;
import com.ericsson.adp.mgmt.backupandrestore.exception.UncontrollableRestoreDataChannelException;
import com.ericsson.adp.mgmt.backupandrestore.grpc.backup.BackupDataStream;
import com.ericsson.adp.mgmt.backupandrestore.grpc.backup.UnexpectedBackupDataStream;
import com.ericsson.adp.mgmt.backupandrestore.grpc.restore.RestoreDataService;
import com.ericsson.adp.mgmt.backupandrestore.grpc.restore.RestoreFragmentStream;
import com.ericsson.adp.mgmt.backupandrestore.job.CreateBackupJob;
import com.ericsson.adp.mgmt.backupandrestore.job.Job;
import com.ericsson.adp.mgmt.backupandrestore.job.JobExecutor;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.util.IdValidator;
import com.ericsson.adp.mgmt.data.BackupData;
import com.ericsson.adp.mgmt.data.DataInterfaceGrpc.DataInterfaceImplBase;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.data.RestoreData;
import com.google.protobuf.Empty;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

/**
 * Implements the data interface, INT_BR_ORCH_DATA.
 */
@Service
public class DataInterfaceImplementation extends DataInterfaceImplBase {
    protected static List<String> backupDataStreamIds = new ArrayList<>();

    private static final Logger log = LogManager.getLogger(DataInterfaceImplementation.class);


    private BackupMetadataWriter backupMetadataWriter;
    private RestoreDataService restoreDataService;
    private JobExecutor jobExecutor;
    private IdValidator idValidator;
    private int timeToWait;

    @Override
    public StreamObserver<BackupData> backup(final StreamObserver<Empty> stream) {
        final List<Job> runningJobs = jobExecutor.getRunningJobs();
        if (!runningJobs.isEmpty()) {
            return createBackupDataStream(runningJobs, stream);
        }
        return createUnexpectedBackupDataStream(stream);
    }

    @Override
    public void restore(final Metadata metadata, final StreamObserver<RestoreData> stream) {
        final List<Job> runningJobs = jobExecutor.getRunningJobs();;
        if (!runningJobs.isEmpty()) {
            handleRestore(metadata, stream, runningJobs);
        } else {
            handleUnexpectedRestore(metadata, stream);
        }
    }

    private Optional<Job> findJob(final List<Job> runningJobs, final Predicate<Job> jobType) {
        return runningJobs.stream().filter(jobType).findFirst();
    }

    private StreamObserver<BackupData> createBackupDataStream(final List<Job> runningJobs, final StreamObserver<Empty> stream) {
        final Optional<Job> currentCreateBackupJob = findJob(runningJobs, job -> job instanceof CreateBackupJob);
        if (currentCreateBackupJob.isPresent()) {
            final BackupDataStream backupDataStream = new BackupDataStream(backupMetadataWriter,
                                                                           (CreateBackupJob) currentCreateBackupJob.get(),
                                                                           stream, idValidator);
            while (backupDataStreamIds.contains(backupDataStream.getStreamId()) ||
                    backupDataStream.getStreamId().equals("")) {  // assign a unique ID
                backupDataStream.updateStreamId();
            }
            backupDataStreamIds.add(backupDataStream.getStreamId());
            log.info("Agent is opening backup data channel for stream {}", backupDataStream.getStreamId());
            return backupDataStream;
        }
        return createUnexpectedBackupDataStream(stream);
    }

    /**
     * Method to get the list of stream Ids
     * @return list of backupDataStream Ids
     */
    public static List<String> getBackupDataStreamIds() {
        return backupDataStreamIds;
    }

    /**
     * Method to remove the ID of a backupDataStream on closing stream, from the list of IDs
     * @param streamId ID of the stream to be removed
     */
    public static void removeStreamId(final String streamId) {
        backupDataStreamIds.remove(streamId);
    }

    private StreamObserver<BackupData> createUnexpectedBackupDataStream(final StreamObserver<Empty> stream) {
        log.error("Rejecting backup data channel. No create-backup job is in progress");
        stream.onError(getAbortedException("Orchestrator not expecting backup. No create-backup job is in progress."));
        return new UnexpectedBackupDataStream();
    }

    private void handleRestore(final Metadata metadata, final StreamObserver<RestoreData> stream, final List<Job> runningJobs) {
        final Optional<Job> currentRestoreJob = findJob(runningJobs, job -> job instanceof RestoreJob);
        if (currentRestoreJob.isPresent()) {
            performRestore(metadata, stream, (RestoreJob) currentRestoreJob.get());
        } else {
            Job otherJob = runningJobs.get(0);
            final Optional<Job> otherCreateBackupJob = findJob(runningJobs, job -> job instanceof CreateBackupJob);
            // If EXPORT and CREATE_BACKUP jobs are running in parallel,
            // ensure the Create Backup job handles the unexpected data channel.
            if (runningJobs.size() > 1 && otherCreateBackupJob.isPresent()) {
                otherJob = otherCreateBackupJob.get();
            }
            otherJob.handleUnexpectedDataChannel(metadata);
            handleUnexpectedRestore(metadata, stream);
        }
    }

    private void performRestore(final Metadata metadata, final StreamObserver<RestoreData> stream, final RestoreJob job) {
        try {
            log.info("Agent is opening restore data channel with metadata <{}>", metadata);
            restoreDataService.processMessage(metadata, job, controlStreamAccess(stream));
            log.info("Closing restore data channel for metadata <{}>", metadata);
            stream.onCompleted();
        } catch (final Exception e) {
            log.error("Closing restore data channel for metadata <{}> due to error", metadata, e);
            stream.onError(getAbortedException(e.getMessage()));
        }
    }

    private StreamObserver<RestoreData> controlStreamAccess(final StreamObserver<RestoreData> stream) {
        if (streamCanBeControlled(stream)) {
            final ServerCallStreamObserver<RestoreData> serverCallStream = (ServerCallStreamObserver<RestoreData>) stream;
            return new RestoreFragmentStream(serverCallStream, this.timeToWait);
        }
        throw new UncontrollableRestoreDataChannelException();
    }

    private void handleUnexpectedRestore(final Metadata metadata, final StreamObserver<RestoreData> stream) {
        log.error("Rejecting restore data channel for metadata <{}>. No restore job is in progress", metadata);
        stream.onError(getAbortedException("Orchestrator not expecting restore metadata. No restore job is in progress."));
    }

    private StatusRuntimeException getAbortedException(final String description) {
        return Status.ABORTED.withDescription(description).asRuntimeException();
    }

    private boolean streamCanBeControlled(final StreamObserver<RestoreData> stream) {
        return stream instanceof ServerCallStreamObserver;
    }

    @Autowired
    public void setBackupMetadataWriter(final BackupMetadataWriter backupMetadataWriter) {
        this.backupMetadataWriter = backupMetadataWriter;
    }

    @Autowired
    public void setRestoreDataService(final RestoreDataService restoreDataService) {
        this.restoreDataService = restoreDataService;
    }

    @Autowired
    public void setJobExecutor(final JobExecutor jobExecutor) {
        this.jobExecutor = jobExecutor;
    }

    @Autowired
    public void setIdValidator(final IdValidator idValidator) {
        this.idValidator = idValidator;
    }

    @Value("${timeout.data.channel}")
    public void setTimeToWait(final int timeToWait) {
        this.timeToWait = timeToWait;
    }

}
