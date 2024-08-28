/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.kms;

import com.ericsson.adp.mgmt.backupandrestore.exception.KMSRequestFailedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Single-purpose service to decrypt SFTP credentials encrypted via KMS using the CM-Key
 * */
@Service
public class CMKeyPassphraseService {
    private static final Logger log = LogManager.getLogger(CMKeyPassphraseService.class);
    private static final int RETRY_ATTEMPTS = 3;

    private KeyManagementService keyManagementService;
    private KeyManagementService.RequestSettings settings;
    private boolean enabled;

    private Path serviceAccountRoleMount;
    private String role;
    private String keyName;

    /**
     * Takes an SFTP passphrase encrypted by CMM using the CM key and returns the passphrase in plaintext
     *
     * Retries decryption via KMS 3 times before failing
     *
     * @param encryptedPassphrase - the password encrypted by the CM key in KMS
     * @return the plaintext passphrase decrypted by KMS
     * */
    public String getPassphrase(final String encryptedPassphrase) {
        int attempt = 0;
        while (attempt < RETRY_ATTEMPTS) {
            try {
                log.info("Attempting to decrypt passphrase in KMS");
                return keyManagementService.decrypt(encryptedPassphrase, getSettings());
            } catch (Exception e) {
                attempt++;
                log.warn("Failed to decrypt the passphrase, attempt {} of {}", attempt, RETRY_ATTEMPTS, e);
                keyManagementService.refreshToken(getSettings());
                sleep();
            }
        }
        throw new KMSRequestFailedException(
                "Failed to decrypt the passphrase after max retry attempts: " + RETRY_ATTEMPTS);
    }

    /**
     * Returns true if CM is enabled
     * @return true if CM is enabled
     * */
    public boolean isEnabled() {
        return enabled;
    }

    private KeyManagementService.RequestSettings getSettings() {
        if (settings == null) {
            settings = new KeyManagementService.RequestSettings(serviceAccountRoleMount, role, keyName);
        }
        return settings;
    }

    private void sleep() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Autowired
    public void setKeyManagementService(final KeyManagementService service) {
        this.keyManagementService = service;
    }

    @Value("${kms.serviceaccount.token.mount:file:/run/secrets/kubernetes.io/serviceaccount/token}")
    public void setServiceAccountRoleMount(final Path serviceAccountRoleMount) {
        this.serviceAccountRoleMount = serviceAccountRoleMount;
    }

    @Value("${cm.key.role:eric-cm-key-role}")
    public void setRole(final String role) {
        this.role = role;
    }

    @Value("${cm.key.name:eric-cm-key-v1}")
    public void setKeyName(final String keyName) {
        this.keyName = keyName;
    }

    @Value("${flag.enable.cm:false}")
    public void setEnabled(final boolean enabled) {
        this.enabled = enabled;
    }
}
