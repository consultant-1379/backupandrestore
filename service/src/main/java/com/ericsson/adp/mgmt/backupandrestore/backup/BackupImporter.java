/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup;

import static com.ericsson.adp.mgmt.backupandrestore.external.ImportFormat.LEGACY;
import static com.ericsson.adp.mgmt.backupandrestore.external.ImportFormat.TARBALL;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;

import com.ericsson.adp.mgmt.backupandrestore.archive.ArchiveUtils;
import com.ericsson.adp.mgmt.backupandrestore.exception.UnexpectedBackupManagerException;
import com.ericsson.adp.mgmt.backupandrestore.exception.FileDirectoryException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.exception.ImportException;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientImportProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.ExternalConnection;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.SftpConnection;
import com.ericsson.adp.mgmt.backupandrestore.util.BackupLimitValidator;

/**
 * BackupImporter import backup from the Sftp/Http Server to BRO
 */
@Service
public class BackupImporter {

    private static final Logger log = LogManager.getLogger(BackupImporter.class);
    private BackupRepository backupRepository;
    private ExternalConnectionFactory externalConnectionFactory;
    private BackupLimitValidator backupLimitValidator;

    private ArchiveUtils archiveUtils;


    /**
     * importBackup validates backup limit and imports backup to the Orchestrator from Sftp/Http Server
     *
     * @param externalClientProperties
     *            externalClientProperties
     * @param backupManager
     *            backupManager
     * @param listener
     *            Property listener used as observer for properties changes on Monitor
     * @return Backup
     */
    public Backup importBackup(final ExternalClientImportProperties externalClientProperties,
            final BackupManager backupManager, final PropertyChangeListener listener) {
        Backup backup = null;
        Optional<String> backupID = Optional.empty();
        try (ExternalConnection connection = externalConnectionFactory.connect(externalClientProperties)) {
            String remotePath;
            backupLimitValidator.validateLimit(backupManager.getBackupManagerId());
            if ((LEGACY == externalClientProperties.getImportFormat()) && (!externalClientProperties.isUsingHttpUriScheme())) {
                // If directory backup is found, the remotePath of the backup.json will be returned to validate if the backup exists in BackupManager
                // If a tarball is found, the prefix (backup ID) from the tarball name will be used to validate if backup exists in BackupManager.
                //      a. If backup exists the action will fail.
                //      b. If backup doesn't exist, the tarball will be downloaded to tmpdir param and untar,
                //         and the path of the _backup_.json in the tmp folder will be returned.
                final String url = externalClientProperties.getExternalClientPath();
                backupID = Optional.of(url.substring(url.lastIndexOf('/') + 1));
                remotePath = handleLegacyURIFormat(externalClientProperties, connection, backupManager, listener);
            } else {
                backupID = backupManager.getBackupID(externalClientProperties.getExternalClientPath());
                backupManager.assertBackupIsNotPresent(externalClientProperties.getExternalClientPath());
                remotePath = connection.downloadBackupFile(externalClientProperties, listener);
            }
            // The _backup_.json file will be downloaded from SFTP for a directory backup or taken from the untar backup from tarball.
            // A persisted backup will be created with the _backup_.json and add to BackupManager.
            // The id from the _backup_.json will be validated in backupManager before backup is added to the BackupManager.
            backup = importBackupFile(connection, externalClientProperties, remotePath, backupManager);
            log.info("Imported Backup: {} into Orchestrator.", backup.getBackupId());
            connection.importBackupData(remotePath, externalClientProperties.getFolderToStoreBackupData(),
                    externalClientProperties.getImportFormat());
        } catch (UnexpectedBackupManagerException | FileDirectoryException e) {
            if (backupID.isPresent()) {
                deleteCorruptedBackup (externalClientProperties, backupID.get());
            }
            throw e;
        } catch (final Exception e) {
            if (backup != null) {
                backup.setStatus(BackupStatus.CORRUPTED);
                backup.persist();
            }
            throw new ImportException("Failed while trying to import backup from the Server", e);
        }
        return backup;
    }

    private void deleteCorruptedBackup(final ExternalClientImportProperties externalClientProperties, final String backupId) {
        archiveUtils.deleteFile(externalClientProperties.getFolderToStoreBackupData().resolve(backupId));
        archiveUtils.deleteFile(externalClientProperties.getFolderToStoreBackupFile().resolve(backupId.concat(".json")));
    }

    /**
     * @param externalClientProperties
     * @param connection
     * @param backupManager
     * @return
     * @throws URISyntaxException
     */
    private String handleLegacyURIFormat(final ExternalClientImportProperties externalClientProperties,
                      final ExternalConnection connection, final BackupManager backupManager,
                      final PropertyChangeListener listener) throws URISyntaxException {
        final SftpConnection sftp = (SftpConnection) connection;
        String remotePath;
        final List<String> tarballNames = sftp.getMatchingBackUpTarballNames(externalClientProperties.getExternalClientPath());

        if (sftp.isDirectoryExists(externalClientProperties.getExternalClientPath())) {
            remotePath = externalClientProperties.getExternalClientPath();
        } else if (tarballNames.size() == 1) {
            final File file = new File(externalClientProperties.getExternalClientPath());
            externalClientProperties.setImportFormat(TARBALL);
            final String singleTarball = tarballNames.get(0);
            backupManager.assertBackupIsNotPresent(singleTarball);
            final URI remote = new URI(file.getParent() + File.separator + singleTarball);
            final ExternalClientImportProperties externalClient = new ExternalClientImportProperties(
                    remote.toString(),
                    externalClientProperties.getPassword(),
                    externalClientProperties.getFolderToStoreBackupData(),
                    externalClientProperties.getFolderToStoreBackupFile());
            remotePath = sftp.downloadBackupFile (externalClient, listener);
        } else {
            final String failureInfo = getLegacyURIHandlingFailureInfo(tarballNames);
            throw new ImportException(failureInfo);
        }
        return remotePath;
    }

    private String getLegacyURIHandlingFailureInfo(final List<String> tarballNames) {
        if (tarballNames.size() > 1) {
            return "Multiple tar.gz with backup name found. Please select a specific tarball to import " + tarballNames;
        } else {
            return "Exported backup directory not found";
        }
    }

    private Backup importBackupFile(final ExternalConnection connection, final ExternalClientImportProperties externalClientProperties,
                                    final String remotePath, final BackupManager backupManager) {
        return backupRepository.importBackup(connection.getBackupFileContent(remotePath, externalClientProperties.getImportFormat()), backupManager);
    }

    @Autowired
    public void setBackupRepository(final BackupRepository backupRepository) {
        this.backupRepository = backupRepository;
    }

    @Autowired
    public void setExternalConnectionFactory(final ExternalConnectionFactory externalConnectionFactory) {
        this.externalConnectionFactory = externalConnectionFactory;
    }

    @Autowired
    public void setBackupLimitValidator(final BackupLimitValidator backupLimitValidator) {
        this.backupLimitValidator = backupLimitValidator;
    }

    @Autowired
    public void setArchiveUtils(final ArchiveUtils archiveUtils) {
        this.archiveUtils = archiveUtils;
    }
}
