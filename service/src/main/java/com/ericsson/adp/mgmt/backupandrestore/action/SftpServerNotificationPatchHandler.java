/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.action;

import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.CLIENT_LOCAL_PRIVATE_KEY;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.CLIENT_LOCAL_PUBLIC_KEY;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.SERVER_LOCAL_HOST_KEY;
import static com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation.ADD;
import static com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation.REMOVE;
import static com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation.REPLACE;
import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManager;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.Endpoint;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SSHHostKeys;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServer;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerFileService;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerInformation;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.MediatorRequest;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.MediatorRequestPatch;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.MediatorSFTPRequestPatch;
import com.ericsson.adp.mgmt.backupandrestore.exception.SftpServerNotFoundException;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;
import com.ericsson.adp.mgmt.backupandrestore.util.SftpServerValidator;

/**
 * Handles requests from CM Notifications
 * containing SFTPServer patches
 */
@Service
public class SftpServerNotificationPatchHandler {

    private static final Logger log = LogManager.getLogger(SftpServerNotificationPatchHandler.class);
    private BackupManagerRepository backupManagerRepository;
    private SftpServerFileService sftpServerFileService;
    private SftpServerValidator sftpServerValidator;
    private JsonService jsonService;

    /**
     * Add the new SFTP Server patches to the backup manager configuration
     * @param sftpServersPatches a map of list of new SFTP Server patches grouped by backupManagerIndex
     */
    void addSftpServerPatches(final Map<Integer, List<MediatorSFTPRequestPatch>> sftpServersPatches) {
        if (!sftpServersPatches.isEmpty()) {
            log.info("Received a notification from CM. Adding the sftpServers configuration with the new information {} ", sftpServersPatches);
            sftpServersPatches.forEach(this::addSftpServer);
        }
    }

    /**
     * Applies the SFTP server update (except sftp server name update) patches to the backup manager configuration
     * @param sftpServersPatches sftpServersPatches
     */
    void updateSftpServerPatches(final Map<Integer, Map<Integer, List<MediatorRequestPatch>>> sftpServersPatches) {
        if (!sftpServersPatches.isEmpty()) {
            log.info("Received a notification from CM. Updating the sftpServers configuration with the new information {} ", sftpServersPatches);
            sftpServersPatches.forEach(this::updateSftpServer);
        }
    }

    /**
     * Applies the SFTP server name update patches to the backup manager configuration
     * @param sftpServersPatches sftpServersPatches
     */
    void updateSftpServerNamePatches(final Map<Integer, Map<Integer, List<MediatorRequestPatch>>> sftpServersPatches) {
        if (!sftpServersPatches.isEmpty()) {
            log.info("Received a notification from CM. Updating the sftpServer names with the new information {} ", sftpServersPatches);
            sftpServersPatches.forEach(this::updateSftpServerName);
        }
    }

    /**
     * Remove the SFTP server patches to the backup manager configuration
     * @param sftpServersPatches sftpServersPatches
     */
    void removeSftpServerPatches(final Map<Integer, List<MediatorRequestPatch>> sftpServersPatches) {
        if (!sftpServersPatches.isEmpty()) {
            log.info("Received a notification from CM. Removing the sftpServers configuration with the information {} ", sftpServersPatches);
            sftpServersPatches.forEach(this::removeSftpServer);
        }
    }

    /**
     * Retrieves a map of new SFTP Server patches grouped by backupManagerIndex
     * @param request the CM mediator request
     * @return a map of list of new SFTP Server patches grouped by backupManagerIndex
     */
    Map<Integer, List<MediatorSFTPRequestPatch>> getAddSftpServersPatches(final MediatorRequest request) {
        final List<MediatorRequestPatch> patches = request.getPatch()
                .stream()
                .filter(patch -> patch.getPath().toLowerCase().matches(MediatorSFTPRequestPatch.SFTP_SERVER_PATH_REGEX_NO_INDEX))
                .filter(patch -> patch.getOp().toLowerCase().contains(ADD.getStringRepresentation()))
                .filter(patch -> !isSftpServerHostKeyPatch(patch))
                .collect(Collectors.toList());
        final List<MediatorSFTPRequestPatch> SFTPpatches = new ArrayList<MediatorSFTPRequestPatch>();
        patches.forEach ((patch) -> {
            final String patchValue = JsonService.toJsonString(patch);
            final Optional<MediatorSFTPRequestPatch> SFTPPatch = jsonService.parseJsonString(patchValue, MediatorSFTPRequestPatch.class);
            SFTPpatches.add(SFTPPatch.orElseThrow(() -> new RuntimeException("Object not present")));
        });
        return SFTPpatches
                .stream()
                .collect(groupingBy(MediatorSFTPRequestPatch::getBackupManagerIndex));
    }

    /**
     * Retrieves a map of SFTP Server update patches (except update sftp server name)
     * grouped by backupManagerIndex and then sftpServerIndex
     * @param request the CM mediator request
     * @return a map of list of SFTP Server update patches grouped by backupManagerIndex and then sftpServerIndex
     */
    Map<Integer, Map<Integer, List<MediatorRequestPatch>>> getUpdateSftpServersPatches(final MediatorRequest request) {
        return request.getPatch()
                .stream()
                .filter(patch -> patch.getPath().toLowerCase().matches(MediatorRequestPatch.SFTP_SERVER_PATH_REGEX_SKIP_ONLY_NAME))
                .filter(patch -> patch.getOp().toLowerCase().contains(REPLACE.getStringRepresentation()) || isSftpServerHostKeyPatch(patch))
                .collect(groupingBy(MediatorRequestPatch::getBackupManagerIndex,
                        groupingBy(MediatorRequestPatch::getSftpServerIndex)));
    }

    /**
     * Retrieves a map of SFTP Server update patches (only update sftp server name)
     * grouped by backupManagerIndex and then sftpServerIndex
     * @param request the CM mediator request
     * @return a map of list of SFTP Server update patches grouped by backupManagerIndex and then sftpServerIndex
     */
    Map<Integer, Map<Integer, List<MediatorRequestPatch>>> getUpdateSftpServerNamesPatches(final MediatorRequest request) {
        return request.getPatch()
                .stream()
                .filter(patch -> patch.getPath().toLowerCase().matches(MediatorRequestPatch.SFTP_SERVER_PATH_REGEX_SFTP_NAME))
                .filter(patch -> patch.getOp().toLowerCase().contains(REPLACE.getStringRepresentation()))
                .collect(groupingBy(MediatorRequestPatch::getBackupManagerIndex,
                        groupingBy(MediatorRequestPatch::getSftpServerIndex)));
    }

    /**
     * Retrieves a map of SFTP Server patches to be removed grouped by backupManagerIndex
     * match with Regex SFTP_SERVER_PATH_REGEX_NO_INDEX
     * since CMM: It will only have an index if there are more indexes there, not when last entry is removed
     * or when first entry is added. This is the behavior via NBI
     * @param request the CM mediator request
     * @return a map of list of new SFTP Server patches grouped by backupManagerIndex
     */
    Map<Integer, List<MediatorRequestPatch>> getRemoveSftpServersPatches(final MediatorRequest request) {
        return request.getPatch()
                .stream()
                .filter(patch -> patch.getPath().toLowerCase().matches(MediatorRequestPatch.SFTP_SERVER_PATH_REGEX_NO_INDEX))
                .filter(patch -> patch.getOp().toLowerCase().contains(REMOVE.getStringRepresentation()))
                .filter(patch -> !isSftpServerHostKeyPatch(patch))
                .collect(groupingBy(MediatorRequestPatch::getBackupManagerIndex));
    }

    private void addSftpServer(final int backupManagerIndex, final List<MediatorSFTPRequestPatch> sftpServerPatches) {
        final BackupManager backupManager = backupManagerRepository.getBackupManager(backupManagerIndex);
        log.info("Adding SFTP Servers for backupManager {} ",  backupManager.getBackupManagerId());
        for (final MediatorSFTPRequestPatch sftpServerPatch: sftpServerPatches) {
            sftpServerPatch.getValue().forEach ((value) -> {
                final String patchValue = JsonService.toJsonString(value);
                final Optional<SftpServerInformation> server = jsonService.parseJsonString(patchValue, SftpServerInformation.class);
                server.ifPresent(sftpServer -> createAndPersistSftpServer(backupManager, sftpServer));
            });
        }
        backupManager.persist();
    }

    private void createAndPersistSftpServer(final BackupManager backupManager, final SftpServerInformation sftpServerInfo) {
        final String backupManagerId = backupManager.getBackupManagerId();
        if (sftpServerValidator.isValid(sftpServerInfo)) {
            final SftpServer sftp = new SftpServer(sftpServerInfo, backupManagerId, backupManagerRepository::persistSftpServer);
            backupManager.addSftpServer(sftp);
            sftp.persist();
            log.info("The sftp server <{}> of backup-manager <{}> has been successfully created.", sftpServerInfo.getName(), backupManagerId);
        } else {
            log.error("The sftp server <{}> of backup-manager <{}> is not created as it is invalid.", sftpServerInfo.getName(), backupManagerId);
        }
    }

    private void updateSftpServer(final int backupManagerIndex, final Map<Integer, List<MediatorRequestPatch>> sftpServerUpdates) {
        final BackupManager backupManager = backupManagerRepository.getBackupManager(backupManagerIndex);
        sftpServerUpdates.forEach((sftpServerIndex, sftpServerPatches) -> {
            final Optional<SftpServer> server = backupManager.getSftpServer(sftpServerIndex);
            server.ifPresentOrElse(sftpServer -> {
                log.info("Updating SFTP Server <{}> of Backup Manager <{}>", sftpServer.getName(), backupManager.getBackupManagerId());
                updateSftpServerProperties(sftpServerPatches, sftpServer);
            }, () -> { throw new SftpServerNotFoundException(String.valueOf(sftpServerIndex)); });
        });
        backupManager.persist();
    }

    private void updateSftpServerName(final int backupManagerIndex, final Map<Integer, List<MediatorRequestPatch>> sftpServerUpdates) {
        final Map<Integer, List<MediatorRequestPatch>> sortedMap = new TreeMap<>((a, b) -> Integer.compare(b, a));
        sortedMap.putAll(sftpServerUpdates);
        final BackupManager backupManager = backupManagerRepository.getBackupManager(backupManagerIndex);
        sortedMap.forEach((sftpServerIndex, sftpServerPatches) -> {
            final Optional<SftpServer> server = backupManager.getSftpServer(sftpServerIndex);
            server.ifPresentOrElse(sftpServer -> {
                log.info("Updating SFTP Server name for <{}> of Backup Manager <{}>", sftpServer.getName(), backupManager.getBackupManagerId());
                updateSftpServerName(backupManager, sftpServer, sftpServerPatches, sftpServerIndex);
            }, () -> { throw new SftpServerNotFoundException(String.valueOf(sftpServerIndex)); });
        });
    }

    private void updateSftpServerProperties(final List<MediatorRequestPatch> sftpServerPatches, final SftpServer sftpServer) {
        final String sftpServerName = sftpServer.getName();
        final Endpoint endpoint = sftpServer.getEndpoints().getEndpoint()[0];
        int validUpdatesCount = 0;
        for (final MediatorRequestPatch sftpServerPatch : sftpServerPatches) {
            final String property = sftpServerPatch.getUpdatedElement();
            final String newValue = String.valueOf(sftpServerPatch.getValue());
            if (sftpServerPatch.getOp().equals(REMOVE.getStringRepresentation()) ||
                    sftpServerValidator.isValidEndpoint(property, newValue)) {
                applySftpServerUpdate(sftpServerName, endpoint, sftpServerPatch, property, newValue);
                validUpdatesCount++;
            } else {
                final String loggedValue = hideKey(property, newValue);
                log.error("Failed to update the <{}> of SFTP Server <{}> as the new value <{}> is invalid", property, sftpServerName, loggedValue);
            }
        }
        if (validUpdatesCount > 0) {
            sftpServer.persist();
            log.info("Successfully persisted the valid updates for SFTP Server <{}>", sftpServer.getName());
        } else {
            log.info("No update is persisted for SFTP Server <{}>", sftpServer.getName());
        }
    }

    private void updateSftpServerName(final BackupManager backupManager, final SftpServer sftpServer,
                                        final List<MediatorRequestPatch> sftpServerPatches,
                                        final int sftpServerIndex) {
        for (final MediatorRequestPatch sftpServerPatch : sftpServerPatches) {
            final String newName = String.valueOf(sftpServerPatch.getValue());
            replaceSftpServerNameWithIndex(backupManager, newName, sftpServerIndex);
        }
        sftpServer.persist();
        log.info("Successfully persisted the valid updates for SFTP Server Name <{}>", sftpServer.getName());
    }

    private void applySftpServerUpdate(final String sftpServerName, final Endpoint endpoint,
                                       final MediatorRequestPatch sftpServerPatch, final String property,
                                       final String newValue) {
        if (isSftpServerHostKeyPatch(sftpServerPatch)) {
            final SSHHostKeys hostKeys = endpoint.getServerAuthentication().getSshHostKeys();
            final String operation = sftpServerPatch.getOp();
            final int hostKeyIndex = sftpServerPatch.getSftpServerHostKeyIndex();
            hostKeys.patchHostKey(newValue, operation, hostKeyIndex);
            log.info("Successfully performed <{}> operation on the host-key with index <{}> of the SFTP Server <{}>",
                    operation, hostKeyIndex, sftpServerName);
        } else {
            endpoint.updateProperty(property, newValue);
            final String loggedValue = hideKey(property, newValue);
            log.info("Successfully updated the <{}> of SFTP Server <{}> with the new value <{}>", property, sftpServerName, loggedValue);
        }
    }

    private void removeSftpServer(final int backupManagerIndex, final List<MediatorRequestPatch> sftpServerPatches) {
        final BackupManager backupManager = backupManagerRepository.getBackupManager(backupManagerIndex);
        for (final MediatorRequestPatch sftpServerPatch: sftpServerPatches) {
            final int sftpServerIndex = sftpServerPatch.getSftpServerIndex();
            if (sftpServerIndex == -1) {
                for (int i = backupManager.getSftpServers().size() - 1; i >= 0; i-- ) {
                    removeSftpServerWithIndex(backupManager, i);
                }
            } else {
                removeSftpServerWithIndex(backupManager, sftpServerIndex);
            }
        }
    }

    private void removeSftpServerWithIndex(final BackupManager backupManager , final int sftpServerIndex) {
        final Optional<SftpServer> server = backupManager.getSftpServer(sftpServerIndex);
        server.ifPresentOrElse(sftpServer -> {
            log.info("Removing SFTP Server <{}> of Backup Manager <{}>", sftpServer.getName(), backupManager.getBackupManagerId());
            sftpServerFileService.deleteSftpServer(sftpServer);
            backupManager.removeSftpServer(sftpServer);
            log.info("Successfully Removed SFTP Server <{}> of Backup Manager <{}>", sftpServer.getName(), backupManager.getBackupManagerId());
        }, () -> { throw new SftpServerNotFoundException(String.valueOf(sftpServerIndex)); });
    }

    private void replaceSftpServerNameWithIndex(final BackupManager backupManager,
                                                final String sftpServerNewName, final int sftpServerIndex) {
        final Optional<SftpServer> server = backupManager.getSftpServer(sftpServerIndex);
        server.ifPresentOrElse(sftpServer -> {
            final String sftpServerName = sftpServer.getName();
            log.info("Replacing SFTP Server name <{}> to <{}> of Backup Manager <{}>",
                    sftpServerName, sftpServerNewName, backupManager.getBackupManagerId());
            sftpServer.setName(sftpServerNewName);
            sftpServerFileService.replaceSftpServer(backupManager.getBackupManagerId(), sftpServerName, sftpServerNewName);
        }, () -> { throw new SftpServerNotFoundException(String.valueOf(sftpServerIndex)); });
    }

    private String hideKey(final String key, String value) {
        if (key.equals(CLIENT_LOCAL_PRIVATE_KEY.toString()) ||
                key.equals(CLIENT_LOCAL_PUBLIC_KEY.toString()) ||
                key.equals(SERVER_LOCAL_HOST_KEY.toString())) {
            value = "******";
        }
        return value;
    }

    private boolean isSftpServerHostKeyPatch(final MediatorRequestPatch patch) {
        return patch.getPath().toLowerCase().contains("host-key");
    }

    @Autowired
    public void setBackupManagerRepository(final BackupManagerRepository backupManagerRepository) {
        this.backupManagerRepository = backupManagerRepository;
    }

    @Autowired
    public void setSftpServerValidator(final SftpServerValidator sftpServerValidator) {
        this.sftpServerValidator = sftpServerValidator;
    }

    @Autowired
    public void setJsonService(final JsonService jsonService) {
        this.jsonService = jsonService;
    }

    @Autowired
    public void setSftpServerFileService(final SftpServerFileService sftpServerFileService) {
        this.sftpServerFileService = sftpServerFileService;
    }
}
