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
package com.ericsson.adp.mgmt.backupandrestore.grpc.restore;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.exception.RestoreLocationDoesNotExistException;
import com.ericsson.adp.mgmt.backupandrestore.job.RestoreJob;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProvider;
import com.ericsson.adp.mgmt.backupandrestore.persist.PersistProviderFactory;
import com.ericsson.adp.mgmt.backupandrestore.restore.ChecksumValidationException;
import com.ericsson.adp.mgmt.backupandrestore.restore.RestoreBackupFile;

import com.ericsson.adp.mgmt.backupandrestore.restore.RestoreCustomMetadataFile;
import com.ericsson.adp.mgmt.data.Metadata;
import com.ericsson.adp.mgmt.data.RestoreData;
import com.ericsson.adp.mgmt.metadata.Fragment;
import io.grpc.stub.StreamObserver;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.nio.file.Path;
import java.util.Optional;

/**
 * Responsible for passing responseObserver and metadata for validation and processing.
 */
@Service
public class RestoreDataService {

    private static final Logger log = LogManager.getLogger(RestoreDataService.class);

    private int restoreFragmentChunkSize;

    private PersistProvider provider;

    /**
     * Setup the persistence provider used by the file service
     * @param configuration - provider configuration used
     * */
    @Autowired
    public void setProvider(final PersistProviderFactory configuration) {
        provider = configuration.getPersistProvider();
    }

    /**
     * Responsible for validation of message & file location, if successful will send fragment.
     * @param metadata
     *            - contains information from which location of the restore files can be determined.
     * @param job
     *            - responsible for generating file path from metadata information.
     * @param stream
     *            - Stream Observer.
     */
    public void processMessage(final Metadata metadata, final RestoreJob job, final StreamObserver<RestoreData> stream) {
        validateMessage(metadata, job);

        try {
            // send the backup file to the agent
            final long sent = new RestoreBackupFile(stream, restoreFragmentChunkSize, job.getAwsConfig()).sendFile(getBackupFile(metadata, job));
            job.updateAgentChunkSize(metadata.getAgentId(), sent);

            // send the custom metadata to the agent
            final Optional<Path> customMetadataFileFolder = findFile(job.getFragmentFolder(metadata).getCustomMetadataFileFolder());
            customMetadataFileFolder.ifPresent(customMetadataPath -> sendCustomMetadata(customMetadataPath, stream, job.getAwsConfig()));
        } catch (ChecksumValidationException e) {
            log.error("Backup checksum verification failed", e);
            job.markBackupAsCorrupted();
            throw e;
        }
    }

    private void sendCustomMetadata(final Path customMetadataPath, final StreamObserver<RestoreData> stream, final S3Config s3Config) {
        new RestoreCustomMetadataFile(stream, s3Config).sendCustomMetadataFile(customMetadataPath);
    }

    @Value("${restore.fragmentChunk.size}")
    public void setRestoreChunkSize(final int restoreFragmentChunkSize) {
        this.restoreFragmentChunkSize = restoreFragmentChunkSize * 1024;
    }

    /**
     * responsible for validating metadata message & checks backup path.
     * @param metadata contains information from which location of the restore files can be determined.
     * @param job responsible for generating file path from metadata information.
     */
    private void validateMessage(final Metadata metadata, final RestoreJob job) {
        validateMetadataMessage(metadata);
        pathGuard(job.getFragmentFolder(metadata).getMetadataFile());
        pathGuard(job.getFragmentFolder(metadata).getDataFileFolder());
    }

    /**
     * checks Backup data path.
     * @param metadata contains information from which location of the restore files can be determined.
     * @param job responsible for generating file path from metadata information.
     * @return backup data file path.
     */
    private Path getBackupFile(final Metadata metadata, final RestoreJob job) {
        return findFile(job.getFragmentFolder(metadata).getDataFileFolder()).orElseThrow(() -> handleBackupFileNotFound(metadata, job));
    }

    private RestoreLocationDoesNotExistException handleBackupFileNotFound(final Metadata metadata, final RestoreJob job) {
        final String fragmentId = metadata.getFragment().getFragmentId();
        final Path dataFileFolder = job.getFragmentFolder(metadata).getDataFileFolder();
        return new RestoreLocationDoesNotExistException(
                "Backup file for fragment <" + fragmentId + "> not found at <" + dataFileFolder + ">");
    }

    /**
     * Find the file inside this folder.
     * @param folderLocation the location of the folder
     * @return the path of the file reside in this folder.
     */
    private Optional<Path> findFile(final Path folderLocation) {
        return provider.list(folderLocation)
                .stream()
                .filter(provider::isFile)
                .sorted(Path::compareTo)
                .findFirst();
    }

    private void validateMetadataMessage(final Metadata metadata) {
        if (metadata.getAgentId().isEmpty() || metadata.getBackupName().isEmpty() || hasInvalidFragmentInfo(metadata.getFragment())) {
            throw new RestoreLocationDoesNotExistException("Invalid metadata <" + metadata + "> received");
        }
    }

    private boolean hasInvalidFragmentInfo(final Fragment fragment) {
        return fragment.getFragmentId().isEmpty() || fragment.getSizeInBytes().isEmpty() || fragment.getVersion().isEmpty();
    }


    private void pathGuard(final Path filePath) {
        if (!provider.exists(filePath)) {
            throw new RestoreLocationDoesNotExistException("Restore file does not exist at <" + filePath + ">");
        }
    }
}
