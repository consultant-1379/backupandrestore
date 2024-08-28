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

import org.apache.commons.codec.binary.Base64;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.ClientIdentity;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.ClientLocalDefinition;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.Endpoint;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.Endpoints;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.PublicKey;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SSHHostKeys;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.ServerAuthentication;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.ServerLocalDefinition;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerInformation;

public class SftpServerTestConstant {
    public static final String CLIENT_PRIVATE_KEY = encodeBase64("-----BEGIN EC PRIVATE KEY-----\n"
            + "aDummyPrivateKeyContent\n"
            + "-----END EC PRIVATE KEY-----\r\n");
    public static final String CLIENT_PUBLIC_KEY = encodeBase64("ecdsa-sha2-nistp521 AAAAE2VjZHNhLXNoYTItbmlzdHA1MjEA aDummyClientPublicKeyContent");
    public static final String ENDPOINT_NAME = "EndpointName";
    public static final String CLIENT_USERNAME = "brosftp";
    public static final String REMOTE_ADDRESS = "1.2.3.4";
    public static final String REMOTE_PATH = "UploadPath";
    public static final int REMOTE_PORT = 2222;
    public static final String SSH_HOST_KEY = encodeBase64("ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAAAgQDzlaS30BWJlK aDummyHostPublicKeyContent");
    public static final String SFTP_SERVER_NAME = "ServerName";

    public static SftpServerInformation createSftpServerInformation() {
        return new SftpServerInformation(SFTP_SERVER_NAME, createEndpoints());
    }

    private static Endpoints createEndpoints() {
        final Endpoints endpoints = new Endpoints();
        endpoints.addEndpoint(createEndpoint());
        return endpoints;
    }

    private static Endpoint createEndpoint() {
        return new Endpoint(ENDPOINT_NAME, REMOTE_ADDRESS, REMOTE_PORT, REMOTE_PATH, createClientIdentity(), createServerAuthentication());
    }

    private static ClientIdentity createClientIdentity() {
        final ClientIdentity clientIdentity = new ClientIdentity(CLIENT_USERNAME, createPublicKey());
        return clientIdentity;
    }

    private static PublicKey createPublicKey() {
        final PublicKey publicKey = new PublicKey(new ClientLocalDefinition(CLIENT_PUBLIC_KEY,
                CLIENT_PRIVATE_KEY));
        return publicKey;
    }

    private static ServerAuthentication createServerAuthentication() {
        final ServerLocalDefinition serverAuthLocalDef = new ServerLocalDefinition();
        serverAuthLocalDef.addKey(SSH_HOST_KEY);
        final SSHHostKeys sshHostKeys = new SSHHostKeys(serverAuthLocalDef);
        final ServerAuthentication serverAuthentication = new ServerAuthentication(sshHostKeys);
        return serverAuthentication;
    }

    public static String encodeBase64(final String key) {
        return Base64.encodeBase64String((key).getBytes());
    }
}
