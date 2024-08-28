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
package com.ericsson.adp.mgmt.backupandrestore.rest;

import static com.ericsson.adp.mgmt.backupandrestore.test.ApiUrl.SFTP_SERVER_BACKUP_MANAGER;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.ericsson.adp.mgmt.backupandrestore.action.MediatorNotificationHandler;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.BackupManagerRepository;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.ClientLocalDefinition;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.Endpoint;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.ServerLocalDefinition;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServer;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerInformation;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.EtagNotifIdBase;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.MediatorRequest;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.operation.PatchOperation;
import com.ericsson.adp.mgmt.backupandrestore.test.SftpServerSystemTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SftpServerConfigurationMediatorControllerTest extends SftpServerSystemTest {
    public static final String NAME = "name";
    public static final String ENDPOINT = "endpoint/";
    public static final String REMOTE_PORT = "remote-port";
    public static final String REMOTE_ADDRESS_ELEMENT = ENDPOINT + "remote-address";
    public static final String REMOTE_PATH_ELEMENT = ENDPOINT + "remote-path";
    public static final String REMOTE_PORT_ELEMENT = ENDPOINT + REMOTE_PORT;
    public static final String CLIENT_LOCAL_DEFINITION = "client-identity/public-key/local-definition/";
    public static final String USERNAME_ELEMENT = CLIENT_LOCAL_DEFINITION + "username";
    public static final String PUBLIC_KEY_ELEMENT = CLIENT_LOCAL_DEFINITION + "public-key";
    public static final String PRIVATE_KEY_ELEMENT = CLIENT_LOCAL_DEFINITION + "private-key";
    public static final String SERVER_LOCAL_DEFINITION = "server-authentication/ssh-host-keys/local-definition/";
    public static final String SERVER_HOST_KEY_ELEMENT = SERVER_LOCAL_DEFINITION + "host-key/";

    private final EtagNotifIdBase etagNotifIdBase = new EtagNotifIdBase();
    private volatile int lastNotifId = 10;

    @Autowired
    private MediatorNotificationHandler mediatorNotificationHandler;

    @Before
    public void setUp() {
        mediatorNotificationHandler.setEtagNotifIdBase(new EtagNotifIdBase("22", 0, null));
        etagNotifIdBase.updateEtag("22");
    }

    @Test
    public void addSftpServerAndThenPerformUpdates() throws Exception {
        etagNotifIdBase.setNotifId(++lastNotifId);

        assertEquals(0, getSftpServers(SFTP_SERVER_BACKUP_MANAGER.toString()).size());

        final MediatorRequest addSftpServerRequest = new ObjectMapper().readValue(
                getJsonAddSftpServerString(SFTP_SERVER_BACKUP_MANAGER.toString(),
                SftpServerTestConstant.createSftpServerInformation(), etagNotifIdBase), MediatorRequest.class);
        restTemplate.postForEntity(URL_MEDIATOR, addSftpServerRequest, String.class);

        List<SftpServer> brmsSftpServers = getSftpServers(SFTP_SERVER_BACKUP_MANAGER.toString());
        assertEquals(1, brmsSftpServers.size());
        assertEquals(SftpServerTestConstant.SFTP_SERVER_NAME, brmsSftpServers.get(0).getName());

        testInvalidSftpServerUpdates(brmsSftpServers, etagNotifIdBase);

        testValidSftpServerUpdates(brmsSftpServers, etagNotifIdBase);

        testAddHostKey(brmsSftpServers, etagNotifIdBase);

        testRemoveHostKey(brmsSftpServers, etagNotifIdBase);

        replaceSftpServerName(etagNotifIdBase);
    }

    private void testInvalidSftpServerUpdates(List<SftpServer> brmsSftpServers,
                                              final EtagNotifIdBase etagNotifIdBase) throws JsonProcessingException, JsonMappingException, JSONException {
        // Perform multiple invalid updates and verify no change is applied to the SFTP Server.
        final Endpoint endpoint = brmsSftpServers.get(0).getEndpoints().getEndpoint()[0];
        final String invalidPublicKey = SftpServerTestConstant.encodeBase64("x5219 invalidBase64EncodedPublicKey hsjkfh 89");
        final String invalidPrivateKey = SftpServerTestConstant.encodeBase64("-----BEGIN ENCRYPTED PRIVATE KEY-----\n"
                + "anInvalidDummyClientPrivateKeyContent\n"
                + "-----END EC PRIVATE KEY-----\r\n");
        final String invalidHostKey =  SftpServerTestConstant.encodeBase64("x5219 invalidBase64EncodedPublicKey hsjkfh 89");
        final int invalidHostKeyIndex = 0;
        final Map<String, Object> invalidUpdates = Map.of(REMOTE_ADDRESS_ELEMENT, "127:0:0:1",
                REMOTE_PATH_ELEMENT, "home@uploads",
                REMOTE_PORT_ELEMENT, 65536,
                USERNAME_ELEMENT, "foo:bar",
                PUBLIC_KEY_ELEMENT, invalidPublicKey,
                PRIVATE_KEY_ELEMENT, invalidPrivateKey,
                SERVER_HOST_KEY_ELEMENT + invalidHostKeyIndex, invalidHostKey);
        MediatorRequest updateSftpServerRequest = new ObjectMapper().readValue(
                getJsonUpdateSftpServerString(SFTP_SERVER_BACKUP_MANAGER.toString(),
                SftpServerTestConstant.SFTP_SERVER_NAME,
                invalidUpdates, PatchOperation.REPLACE, etagNotifIdBase),
                MediatorRequest.class);
        restTemplate.postForEntity(URL_MEDIATOR, updateSftpServerRequest, String.class);
        assertEquals(1, getSftpServers(SFTP_SERVER_BACKUP_MANAGER.toString()).size());
        assertEquals(SftpServerTestConstant.REMOTE_ADDRESS, endpoint.getRemoteAddress());
        assertEquals(SftpServerTestConstant.REMOTE_PATH, endpoint.getRemotePath());
        assertEquals(SftpServerTestConstant.REMOTE_PORT, endpoint.getRemotePort());
        assertEquals(SftpServerTestConstant.CLIENT_USERNAME, endpoint.getClientIdentity().getUsername());
        final ClientLocalDefinition clientLocalDefinition = endpoint.getClientIdentity().getPublicKey().getLocalDefinition();
        final ServerLocalDefinition serverLocalDefinition = endpoint.getServerAuthentication().getSshHostKeys().getLocalDefinition();
        assertEquals(SftpServerTestConstant.CLIENT_PUBLIC_KEY, clientLocalDefinition.getPublicKey());
        assertEquals(SftpServerTestConstant.CLIENT_PRIVATE_KEY, clientLocalDefinition.getPrivateKey());
        // verify that the invalid host key is not added to the list of existing host keys
        assertEquals(1, serverLocalDefinition.getHostKeys().size());
        assertFalse(serverLocalDefinition.getHostKeys().contains(invalidHostKey));
    }

    private void testValidSftpServerUpdates(List<SftpServer> brmsSftpServers,
                                            final EtagNotifIdBase etagNotifIdBase) throws JsonProcessingException, JsonMappingException, JSONException {
        // Perform multiple valid updates and verify all the changes are applied.
        final String newRemoteAddress = "127.0.0.1";
        final String newRemotePath = "foo-bar/uploads";
        final int newPort = 2200;
        final String newUserName = "foo";
        final String newPublicKey = SftpServerTestConstant.encodeBase64("ecdsa-sha2-nistp521 AAAAE2VjZHNhLXNoYTItbmlzdHA1MjEAAAAIb newBase64EncodedPublicKey");
        final String newPrivateKey = SftpServerTestConstant.encodeBase64("-----BEGIN EC PRIVATE KEY-----\n"
                + "anUpdatedDummyClientPrivateKeyContent\n"
                + "-----END EC PRIVATE KEY-----\r\n");
        // This host key is expected to REPLACE the existing host key
        final String newHostKey =  SftpServerTestConstant.encodeBase64("ecdsa-sha2-nistp521 AAAAE2VjZHNhLXNoYTItbmlzdHA1MjEAAAAIbm newBase64EncodedHostKey");
        final int hostKeyIndex = 0;
        final Map<String, Object> validUpdates = Map.of(REMOTE_ADDRESS_ELEMENT, newRemoteAddress,
                                                        REMOTE_PATH_ELEMENT, newRemotePath,
                                                        REMOTE_PORT_ELEMENT, newPort,
                                                        USERNAME_ELEMENT, newUserName,
                                                        PUBLIC_KEY_ELEMENT, newPublicKey,
                                                        PRIVATE_KEY_ELEMENT, newPrivateKey,
                                                        SERVER_HOST_KEY_ELEMENT + hostKeyIndex, newHostKey);
        MediatorRequest updateSftpServerRequest = new ObjectMapper().readValue(getJsonUpdateSftpServerString(SFTP_SERVER_BACKUP_MANAGER.toString(),
                SftpServerTestConstant.SFTP_SERVER_NAME, validUpdates, PatchOperation.REPLACE, etagNotifIdBase), MediatorRequest.class);
        restTemplate.postForEntity(URL_MEDIATOR, updateSftpServerRequest, String.class);
        final Endpoint endpoint = brmsSftpServers.get(0).getEndpoints().getEndpoint()[0];
        final ClientLocalDefinition clientLocalDefinition = endpoint.getClientIdentity().getPublicKey().getLocalDefinition();
        final ServerLocalDefinition serverLocalDefinition = endpoint.getServerAuthentication().getSshHostKeys().getLocalDefinition();
        assertEquals(1, getSftpServers(SFTP_SERVER_BACKUP_MANAGER.toString()).size());
        assertEquals(newRemoteAddress, endpoint.getRemoteAddress());
        assertEquals(newRemotePath, endpoint.getRemotePath());
        assertEquals(newPort, endpoint.getRemotePort());
        assertEquals(newUserName, endpoint.getClientIdentity().getUsername());
        assertEquals(newPublicKey, clientLocalDefinition.getPublicKey());
        assertEquals(newPrivateKey, clientLocalDefinition.getPrivateKey());
        assertEquals(1, serverLocalDefinition.getHostKeys().size());
        assertTrue(serverLocalDefinition.getHostKeys().contains(newHostKey));
    }

    private void testAddHostKey(List<SftpServer> brmsSftpServers,
                                final EtagNotifIdBase etagNotifIdBase) throws JsonMappingException, JsonProcessingException, JSONException {
        // This host key is expected to be ADDED to the existing host keys
        final String newHostKey =  SftpServerTestConstant.encodeBase64("ecdsa-sha2-nistp521 AAAAE2VjZHNhLXNoYTItbmlzdHA1MjEAAAAIbm additionalBase64EncodedHostKey");
        final int hostKeyIndex = 1;
        MediatorRequest updateSftpServerRequest = new ObjectMapper().readValue(getJsonUpdateSftpServerString(SFTP_SERVER_BACKUP_MANAGER.toString(),
                SftpServerTestConstant.SFTP_SERVER_NAME,
                Map.of(SERVER_HOST_KEY_ELEMENT + hostKeyIndex, newHostKey),
                PatchOperation.ADD, etagNotifIdBase),
                MediatorRequest.class);
        restTemplate.postForEntity(URL_MEDIATOR, updateSftpServerRequest, String.class);
        final Endpoint endpoint = brmsSftpServers.get(0).getEndpoints().getEndpoint()[0];
        final ServerLocalDefinition serverLocalDefinition = endpoint.getServerAuthentication().getSshHostKeys().getLocalDefinition();
        assertEquals(2, serverLocalDefinition.getHostKeys().size());
        assertTrue(serverLocalDefinition.getHostKeys().contains(newHostKey));
    }

    private void testRemoveHostKey(List<SftpServer> brmsSftpServers,
                                   final EtagNotifIdBase etagNotifIdBase) throws JsonMappingException, JsonProcessingException, JSONException {
        // This host key is expected to be REMOVED
        final String existingHostKey =  SftpServerTestConstant.encodeBase64("ecdsa-sha2-nistp521 AAAAE2VjZHNhLXNoYTItbmlzdHA1MjEAAAAIbm additionalBase64EncodedHostKey");
        final int hostKeyIndex = 1;
        MediatorRequest updateSftpServerRequest = new ObjectMapper().readValue(getJsonUpdateSftpServerString(SFTP_SERVER_BACKUP_MANAGER.toString(),
                SftpServerTestConstant.SFTP_SERVER_NAME,
                Map.of(SERVER_HOST_KEY_ELEMENT + hostKeyIndex, existingHostKey),
                PatchOperation.REMOVE, etagNotifIdBase),
                MediatorRequest.class);
        restTemplate.postForEntity(URL_MEDIATOR, updateSftpServerRequest, String.class);
        final Endpoint endpoint = brmsSftpServers.get(0).getEndpoints().getEndpoint()[0];
        final ServerLocalDefinition serverLocalDefinition = endpoint.getServerAuthentication().getSshHostKeys().getLocalDefinition();
        assertEquals(1, serverLocalDefinition.getHostKeys().size());
        assertFalse(serverLocalDefinition.getHostKeys().contains(existingHostKey));
    }

    private void replaceSftpServerName(final EtagNotifIdBase etagNotifIdBase)
                                        throws JsonProcessingException, JsonMappingException, JSONException {

        final String newSftpName = "NewServerName";
        final Map<String, Object> validUpdates = Map.of(NAME, newSftpName);

        final MediatorRequest replaceSftpServerNameRequest = new ObjectMapper().readValue(
                getJsonUpdateSftpServerString(SFTP_SERVER_BACKUP_MANAGER.toString(),
                SftpServerTestConstant.SFTP_SERVER_NAME,
                validUpdates,
                PatchOperation.REPLACE,
                etagNotifIdBase), MediatorRequest.class);
        restTemplate.postForEntity(URL_MEDIATOR, replaceSftpServerNameRequest, String.class);

        List<SftpServer> brmsSftpServersUpdated = getSftpServers(SFTP_SERVER_BACKUP_MANAGER.toString());
        assertEquals(1, brmsSftpServersUpdated.size());
        assertEquals(newSftpName, brmsSftpServersUpdated.get(0).getName());
    }

    @Test
    public void addSftpServer_invalidSftpServerName() throws Exception {
        etagNotifIdBase.setNotifId(++lastNotifId);
        final SftpServerInformation sftpServer = SftpServerTestConstant.createSftpServerInformation();
        final String invalidSftpServerName = "";
        sftpServer.setName(invalidSftpServerName);
        testAddInvalidSftpServer(sftpServer, etagNotifIdBase);
    }

    @Test
    public void addSftpServer_invalidEndpointName() throws Exception {
        etagNotifIdBase.setNotifId(4);
        final SftpServerInformation sftpServer = SftpServerTestConstant.createSftpServerInformation();
        final String invalidEndpointName = "@Endpoint";
        sftpServer.getEndpoints().getEndpoint()[0].setName(invalidEndpointName);
        testAddInvalidSftpServer(sftpServer, etagNotifIdBase);
    }

    @Test
    public void addSftpServer_nullRemotePath() throws Exception {
        etagNotifIdBase.setNotifId(++lastNotifId);
        final SftpServerInformation sftpServer = SftpServerTestConstant.createSftpServerInformation();
        sftpServer.getEndpoints().getEndpoint()[0].setRemotePath(null);
        testAddInvalidSftpServer(sftpServer, etagNotifIdBase);
    }

    @Test
    public void addSftpServer_nullUserName() throws Exception {
        etagNotifIdBase.setNotifId(++lastNotifId);
        final SftpServerInformation sftpServer = SftpServerTestConstant.createSftpServerInformation();
        sftpServer.getEndpoints().getEndpoint()[0].getClientIdentity().setUsername(null);
        testAddInvalidSftpServer(sftpServer, etagNotifIdBase);
    }

    @Test
    public void updateAnUnknownSftpServer() throws Exception {
        MediatorRequest updateSftpServerRequest = new ObjectMapper().readValue(getJsonUpdateSftpServerString(SFTP_SERVER_BACKUP_MANAGER.toString(),
                "UNKNOWN_SERVER", Map.of(REMOTE_PORT, 1234), PatchOperation.REPLACE), MediatorRequest.class);
        final ResponseEntity<String> responseEntity = restTemplate.postForEntity(URL_MEDIATOR, updateSftpServerRequest, String.class);
        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode());
    }

    @Test
    public void removeSftpServer() throws Exception {
        etagNotifIdBase.setNotifId(++lastNotifId);

        assertEquals(0, getSftpServers(SFTP_SERVER_BACKUP_MANAGER.toString()).size());

        final MediatorRequest addSftpServerRequest = new ObjectMapper().readValue(
                getJsonAddSftpServerString(SFTP_SERVER_BACKUP_MANAGER.toString(),
                        SftpServerTestConstant.createSftpServerInformation(), etagNotifIdBase), MediatorRequest.class);
        restTemplate.postForEntity(URL_MEDIATOR, addSftpServerRequest, String.class);

        List<SftpServer> brmsSftpServers = getSftpServers(SFTP_SERVER_BACKUP_MANAGER.toString());
        assertEquals(1, brmsSftpServers.size());
        assertEquals(SftpServerTestConstant.SFTP_SERVER_NAME, brmsSftpServers.get(0).getName());

        final MediatorRequest removeSftpServerRequest = new ObjectMapper().readValue(
                getJsonRemoveSftpServerString(SFTP_SERVER_BACKUP_MANAGER.toString(),
                        SftpServerTestConstant.SFTP_SERVER_NAME,
                        SftpServerTestConstant.createSftpServerInformation(), etagNotifIdBase), MediatorRequest.class);
        restTemplate.postForEntity(URL_MEDIATOR, removeSftpServerRequest, String.class);

        List<SftpServer> brmsSftpServersUpdated = getSftpServers(SFTP_SERVER_BACKUP_MANAGER.toString());
        assertEquals(0, brmsSftpServersUpdated.size());

    }

    private void testAddInvalidSftpServer(final SftpServerInformation sftpServer,
                                          final EtagNotifIdBase etagNotifIdBase) throws Exception {
        final String backupManager = "backupManagerWithBackupToDelete";
        assertEquals(0, getSftpServers(backupManager).size());

        final MediatorRequest addSftpServerRequest = new ObjectMapper().readValue(
                getJsonAddSftpServerString(backupManager,
                sftpServer, etagNotifIdBase), MediatorRequest.class);
        restTemplate.postForEntity(URL_MEDIATOR, addSftpServerRequest, String.class);

        assertEquals(0, getSftpServers(backupManager).size());
    }
}
