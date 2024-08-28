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
package com.ericsson.adp.mgmt.backupandrestore.test;

import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.SFTP_SERVER;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.SFTP_SERVER_BACKUP_MANAGER;
import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.V3_BASE_URL;
import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.URLMAPPING;

import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServer;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.EtagNotifIdBase;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation;
import com.ericsson.adp.mgmt.backupandrestore.util.JsonService;

/**
 * Created to group the use of common requirements to test SftpServer configuration
 */
public abstract class SftpServerSystemTest extends SystemTestWithAgents {
    protected static final TestRestTemplate restTemplate = new TestRestTemplate();

    protected static final String URL_MEDIATOR=V3_BASE_URL + URLMAPPING;

    protected static final String AGENT_SFTP_SERVER="backupSftpServerAgent";
    protected String baseEtag;
    protected String configEtag;
    protected int notifId;

    @Autowired
    private BackupManagerRepository backupManagerRepository;

    @Autowired
    private JsonService jsonService;

    @Before
    public void setup() {
        backupManagerRepository.createBackupManager(SFTP_SERVER_BACKUP_MANAGER.toString());
    }

    protected String getJsonAddSftpServerString2(final String backupManager, final Object value) throws JSONException {
        return getJsonNotification ()
                .put("changedBy", "cmyp")
                .put("patch", getAddSftpServerPatch(backupManager, value))
                .toString();
    }

    /**
     * Creates a CM Notification containing a patch to add a new SFTP server
     * @param backupManager the backup manager where the SFTP server will be added
     * @param value the object holding the SFTP server information
     * @return the JSON string representation of the CM notification
     * @throws JSONException
     */
    protected String getJsonAddSftpServerString(final String backupManager, final Object value,
                                                final EtagNotifIdBase etagNotifIdBase) throws JSONException  {
        baseEtag = etagNotifIdBase.getEtag();
        nextNotifId(etagNotifIdBase);
        configEtag = etagNotifIdBase.getEtag();
        notifId = etagNotifIdBase.getNotifId();
        return getJsonNotification ()
                .put("changedBy", "cmyp")
                .put("patch", getAddSftpServerPatch(backupManager, value))
                .put("baseETag", baseEtag)
                .put("configETag", configEtag)
                .put("notifId", notifId)
                .toString();
    }

    /**
     * Creates a patch object to add a new SFTP server.
     * This automatically calculates the new index of the SFTPServer based on the
     * existing number of sftp servers in the backup manager.
     * @param backupManager the backup manager where the SFTP server will be added
     * @param value the object holding the SFTP server information
     * @return the JSON Array containing the JSON patch
     * @throws JSONException
     */
    protected JSONArray getAddSftpServerPatch(final String backupManager, final Object value) throws JSONException {
        final JSONArray patches = new JSONArray();
        final int position=getBackupManagerPosition(backupManager);
        final int sftpServerIndex = getSftpServers(backupManager).size();
        patches.put(getAddSftpServerPatchContent(position, sftpServerIndex, value));
        return patches;
    }

    /**
     * Creates the content of the patch object to add a new SFTP server
     * @param position the index of the backup manager
     * @param sftpServerIndex the index of the sftp server
     * @param value the object holding the SFTP server information
     * @return the JSON object containing the patch information.
     * @throws JSONException
     */
    protected JSONObject getAddSftpServerPatchContent(final int position, final int sftpServerIndex, final Object value) throws JSONException {
        return new JSONObject()
                .put ("op", ADD_OPERATION)
                .put ("backupManagerIndex",position)
                .put ("sftpServerIndex",sftpServerIndex)
                .put ("path","/ericsson-brm:brm/backup-manager/"+ position + "/" + SFTP_SERVER.toString() + "/" + sftpServerIndex)
                .put ("value", new JSONObject(jsonService.toJsonString(value)));
    }

    /**
     * Creates a CM Notification containing a patch to update a new SFTP server
     * @param backupManager the backup manager where the SFTP server will be added
     * @param sftpServerName the name of the SFTP server
     * @param updates map of the 'path to the SFTP server property to be updated' to its 'new value'
     * @param hostKeyOperation the patch operation for the host key
     * @return the JSON string representation of the CM notification
     * @throws JSONException
     */
    protected String getJsonUpdateSftpServerString(final String backupManager,
                                                   final String sftpServerName,
                                                   final Map<String, Object> updates,
                                                   final PatchOperation hostKeyOperation,
                                                   final EtagNotifIdBase etagNotifId ) throws JSONException {
        baseEtag = etagNotifId.getEtag();
        nextNotifId(etagNotifId);
        configEtag = etagNotifId.getEtag();
        notifId = etagNotifId.getNotifId();
        return getJsonNotification ()
                .put("changedBy", "cmyp")
                .put("patch", getUpdateSftpServerPatch(backupManager, sftpServerName, updates, hostKeyOperation))
                .put("baseETag", baseEtag)
                .put("configETag", configEtag)
                .put("notifId", notifId)
                .toString();
    }

    /**
     * Creates a CM Notification containing a patch to update a new SFTP server
     * @param backupManager the backup manager where the SFTP server will be added
     * @param sftpServerName the name of the SFTP server
     * @param updates map of the 'path to the SFTP server property to be updated' to its 'new value'
     * @param hostKeyOperation the patch operation for the host key
     * @return the JSON string representation of the CM notification
     * @throws JSONException
     */
    protected String getJsonUpdateSftpServerString(final String backupManager,
                                                   final String sftpServerName,
                                                   final Map<String, Object> updates,
                                                   final PatchOperation hostKeyOperation) throws JSONException  {
        return getJsonNotification ()
                .put("changedBy", "cmyp")
                .put("patch", getUpdateSftpServerPatch(backupManager, sftpServerName, updates, hostKeyOperation))
                .toString();
    }
    /**
    * Creates a patch object to update a new SFTP server.
    * @param backupManager the backup manager where the SFTP server will be added
     * @param sftpServerName the name of the SFTP server
     * @param updates map of the 'path to the SFTP server property to be updated' to its 'new value'
     * @param hostKeyOperation the patch operation for the host key
     * @return the JSON Array containing the JSON patch
     * @throws JSONException
     */
    protected JSONArray getUpdateSftpServerPatch(final String backupManager, final String sftpServerName, final Map<String, Object> updates, final PatchOperation hostKeyOperation) throws JSONException {
        final JSONArray patches = new JSONArray();
        final int position=getBackupManagerPosition(backupManager);
        final int sftpServerIndex = getSftpServerIndex(backupManager, sftpServerName);
        for (String updatedElement : updates.keySet()) {
            patches.put(getUpdateSftpServerPatchContent(position, sftpServerIndex, updatedElement, hostKeyOperation, updates.get(updatedElement)));
        }
       return patches;
    }

    /**
     * Creates the content of the patch object to add a new SFTP server
     * @param position the index of the backup manager
     * @param sftpServerIndex the index of the sftp server
     * @param updatedElement the relative path to the updated sftp server property
     * @param hostKeyOperation the patch operation for the host key
     * @param value the object holding the SFTP server information
     * @return the JSON object containing the patch information.
     * @throws JSONException
     */
    protected JSONObject getUpdateSftpServerPatchContent(final int position, final int sftpServerIndex,
                                                         final String updatedElement, final PatchOperation hostKeyOperation, final Object value) throws JSONException {
        return new JSONObject()
                .put ("op", updatedElement.contains("/host-key") && hostKeyOperation != null ? hostKeyOperation.getStringRepresentation() : REPLACE_OPERATION)
                .put ("backupManagerIndex",position)
                .put ("sftpServerIndex",sftpServerIndex)
                .put ("path","/ericsson-brm:brm/backup-manager/"+ position + "/" + SFTP_SERVER.toString()  + "/" + sftpServerIndex + "/" + updatedElement)
                .put ("value", updatedElement.contains("/host-key") && hostKeyOperation == PatchOperation.REMOVE ? null : value);
    }

    /**
     * Creates a CM Notification containing a patch to delete a SFTP server
     * @param backupManager the backup manager where the SFTP server will be deleted
     * @param sftpServerName the name of the SFTP server
     * @param value the object holding the SFTP server information
     * @return the JSON string representation of the CM notification
     * @throws JSONException
     */
    protected String getJsonRemoveSftpServerString(final String backupManager,
                                                   final String sftpServerName,
                                                   final Object value,
                                                   final EtagNotifIdBase etagNotifId ) throws JSONException {
        baseEtag = etagNotifId.getEtag();
        nextNotifId(etagNotifId);
        configEtag = etagNotifId.getEtag();
        notifId = etagNotifId.getNotifId();
        return getJsonNotification ()
                .put("changedBy", "cmyp")
                .put("patch", getRemoveSftpServerPatch(backupManager, sftpServerName, value))
                .put("baseETag", baseEtag)
                .put("configETag", configEtag)
                .put("notifId", notifId)
                .toString();
    }

    /**
     * Creates a patch object to delete a SFTP server.
     * @param backupManager the backup manager where the SFTP server will be deleted
     * @param sftpServerName the name of the SFTP server
     * @param value the object holding the SFTP server information
     * @return the JSON Array containing the JSON patch
     * @throws JSONException
     */
    protected JSONArray getRemoveSftpServerPatch(final String backupManager, final String sftpServerName, final Object value) throws JSONException {
        final JSONArray patches = new JSONArray();
        final int position=getBackupManagerPosition(backupManager);
        final int sftpServerIndex = getSftpServerIndex(backupManager, sftpServerName);
        patches.put(getRemoveSftpServerPatchContent(position, sftpServerIndex, value));
        return patches;
    }

    /**
     * Creates the content of the patch object to delete a SFTP server
     * @param position the index of the backup manager
     * @param sftpServerIndex the index of the sftp server
     * @param value the object holding the SFTP server information
     * @return the JSON object containing the patch information.
     * @throws JSONException
     */
    protected JSONObject getRemoveSftpServerPatchContent(final int position, final int sftpServerIndex,
                                                         final Object value) throws JSONException {
        return new JSONObject()
                .put ("op", REMOVE_OPERATION)
                .put ("backupManagerIndex",position)
                .put ("sftpServerIndex",sftpServerIndex)
                .put ("path","/ericsson-brm:brm/backup-manager/"+ position + "/" + SFTP_SERVER.toString()  + "/" + sftpServerIndex)
                .put ("value", new JSONObject(jsonService.toJsonString(value)));
    }

    protected List<SftpServer> getSftpServers(final String backupManager) {
        return backupManagerRepository.getBackupManager(getBackupManagerPosition(backupManager)).getSftpServers();
    }

    protected int getSftpServerIndex(final String backupManager, final String sftpServerName) {
        final List<SftpServer> servers = getSftpServers(backupManager);
        for (int index=0; index < servers.size(); index++) {
            if (servers.get(index).getName().equalsIgnoreCase(sftpServerName)) {
                return index;
            }
        }
        return -1;
    }

    protected void nextNotifId (final EtagNotifIdBase etagNotifId) {
        etagNotifId.setNotifId(etagNotifId.getNotifId() + 1);
    }
}
