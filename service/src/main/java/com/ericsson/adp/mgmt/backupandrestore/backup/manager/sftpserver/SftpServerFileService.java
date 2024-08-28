/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.exception.FilePersistenceException;
import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import com.ericsson.adp.mgmt.backupandrestore.util.OrchestratorDataFileService;

/**
 * Responsible for reading or writing SFTPServer information files
 */
@Service
public class SftpServerFileService  extends OrchestratorDataFileService<SftpServerInformation> {

    private static final String SFTP_SERVER_FOLDER = "sftp-server";
    private static final Logger log = LogManager.getLogger(SftpServerFileService.class);

    private final List<Version<SftpServerInformation>> versions = List.of(getDefaultVersion(
        s -> jsonService.parseJsonString(s, SftpServerInformation.class),
        p -> p.getFileName().toString().endsWith(JSON_EXTENSION)
    ));

    /**
     * Writes sftpServer to file.
     * @param sftpServer to be written.
     * @throws FilePersistenceException On error throws exception
     */
    public void writeToFile(final SftpServer sftpServer) {
        final Path sftpServerFolder = getSftpServerFolder(sftpServer.getBackupManagerId());
        final Path sftpServerFile = sftpServerFolder.resolve(getFile(sftpServer.getName()));

        if (sftpServer.getVersion() == null) {
            sftpServer.setVersion(getLatestVersion());
        }

        try {
            writeFile(sftpServerFolder, sftpServerFile, toJson(sftpServer).getBytes(), sftpServer.getVersion());
        } catch (Exception exception) {
            throw new FilePersistenceException(exception);
        }
    }

    /**
     * Delete the sftpServer file on disk.
     * @param sftpServer - the sftpServer whose file is to be deleted
     * @return true if file deleted, false otherwise
     * */
    public boolean deleteSftpServer(final SftpServer sftpServer) {
        final Path sftpServerFolder = getSftpServerFolder(sftpServer.getBackupManagerId());
        final Path sftpServerFile = sftpServerFolder.resolve(getFile(sftpServer.getName()));
        try {
            delete(sftpServerFile);
        } catch (IOException e) {
            log.debug("sftpServerFile path <{}> not exist", sftpServerFile);
            return false;
        }
        return true;
    }

    /**
     * Replace the sftpServer file on disk.
     * @param backupManagerId - the backupManagerId
     * @param sftpServerName - the sftpServer name whose file is to be replaced
     * @param sftpServerNewName - the sftpServer name whose file is to be updated
     * @return true if file deleted, false otherwise
     * */
    public boolean replaceSftpServer(final String backupManagerId, final String sftpServerName, final String sftpServerNewName) {
        final Path sftpServerFolder = getSftpServerFolder(backupManagerId);
        final Path sftpServerFile = sftpServerFolder.resolve(getFile(sftpServerName));
        try {
            Files.move(sftpServerFile, sftpServerFile.resolveSibling(sftpServerNewName + ".json"));
        } catch (IOException e) {
            log.debug("new file replace failed, sftpServerFile path <{}> exist", sftpServerFile);
            return false;
        }
        return true;
    }

    /**
     * Gets persisted sftp servers from the backup manager
     * @param backupManagerId the id of the backup manager
     * @return a list of SftpServer
     */
    public List<SftpServer> getSftpServers(final String backupManagerId) {
        return getSftpServersInformation(backupManagerId)
                .stream().map(sftpServerInformation -> new SftpServer(sftpServerInformation, backupManagerId, this::writeToFile))
                .collect(Collectors.toList());
    }

    private List<SftpServerInformation> getSftpServersInformation(final String backupManagerId) {
        final Path sftpServerFolder = getSftpServerFolder(backupManagerId);
        if (exists(sftpServerFolder)) {
            return readObjectsFromFiles(sftpServerFolder);
        }
        return new ArrayList<>();
    }

    private String toJson(final SftpServer sftpServer) {
        final SftpServerInformation copy = new SftpServerInformation(sftpServer);
        return jsonService.toJsonString(copy);
    }

    private Path getSftpServerFolder(final String backupManagerId) {
        return backupManagersLocation.resolve(backupManagerId).resolve(SFTP_SERVER_FOLDER);
    }

    @Override
    protected List<Version<SftpServerInformation>> getVersions() {
        return versions;
    }
}
