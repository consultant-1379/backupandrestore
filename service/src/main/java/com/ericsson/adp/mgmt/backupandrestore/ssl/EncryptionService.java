/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/

package com.ericsson.adp.mgmt.backupandrestore.ssl;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.iv.RandomIvGenerator;
import org.springframework.stereotype.Service;

/**
 * Utility Class to provide encryption/decryption of string literals
 * as a service, using "bro-key". Used whenever BRO is required to
 * store sensitive data on disk that must be used in an unencrypted
 * manner, e.g. auto export passwords cannot be stored in plaintext
 * but must be used in plaintext by BRO to export automatic backups
 * */
@Service
public class EncryptionService {
    private static final String JASPYT_KEY_NAME = "jasypt.encryptor.password";
    private final StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();

    /**
     * Constructor for service. Used to generate Bean
     * */
    public EncryptionService() {
        final String key = System.getProperties().containsKey(JASPYT_KEY_NAME) ? System.getProperty(JASPYT_KEY_NAME) : "planb";
        encryptor.setAlgorithm("PBEWithHMACSHA512AndAES_256");
        encryptor.setIvGenerator(new RandomIvGenerator());
        encryptor.setPassword(key);
        encryptor.initialize();
    }

    /**
     * Encrypt given string
     * @param cleartext - Text to encrypt
     * @return ciphertext encrypted using key configured in constructor
     * */
    public String encrypt(final String cleartext) {
        return encryptor.encrypt(cleartext);
    }

    /**
     * Decrypt given string
     * @param ciphertext - Text to decrypt
     * @return cleartext decrypted using key configured in constructor
     * */
    public String decrypt(final String ciphertext) {
        return encryptor.decrypt(ciphertext);
    }
}
