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

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.*;

import com.ericsson.adp.mgmt.backupandrestore.exception.KeyStoreGenerationException;
import com.ericsson.adp.mgmt.backupandrestore.util.SecurityEventLogger;
import com.ericsson.adp.mgmt.backupandrestore.util.TLSCertUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Service for generating the key store
 */
@Service
public class KeyStoreService {
    private static final Logger log = LogManager.getLogger(KeyStoreService.class);
    private static final String BRO_CERT_ISSUE_CATEGORY = "BRO-Certificate-Issue";
    private static final String CLASS_STRING = KeyStoreService.class.getName();
    private static final String KEY_STORE_TYPE = "PKCS12";

    private final Map<KeyStoreAlias, KeyStore> keyStores;
    private final Map<KeyStoreAlias, KeyStoreConfiguration> keyStoreConfigs;
    private final List<String> expiredCerts = new ArrayList<>();

    /**
     * Creates a KeyStoreService
     * @param restKeyStoreConfiguration - rest keystore config
     * @param cmmRestKeyStoreConfiguration - cmm keystore config
     * @param pmKeyStoreConfiguration - pm keystore config
     * @param kafkaKeyStoreConfiguration - kafka keystore config
     * @param cmmNotificationEndpointKeyStoreConfiguration CMM Notification config
     * @param redisKeyStoreConfiguration Redis Keystore config
     * @param osmnKeyStoreConfiguration OSMN keystore config
     */
    @Autowired
    public KeyStoreService(
            final BroKeyStoreConfiguration restKeyStoreConfiguration,
            final CMMRestKeyStoreConfiguration cmmRestKeyStoreConfiguration,
            final PMKeyStoreConfiguration pmKeyStoreConfiguration,
            final KafkaKeyStoreConfiguration kafkaKeyStoreConfiguration,
            final CMMNotificationEndpointKeyStoreConfiguration cmmNotificationEndpointKeyStoreConfiguration,
            final RedisKeyStoreConfiguration redisKeyStoreConfiguration,
            final OSMNKeyStoreConfiguration osmnKeyStoreConfiguration
    ) {
        keyStores = new EnumMap<>(KeyStoreAlias.class);
        keyStoreConfigs = new EnumMap<>(KeyStoreAlias.class);
        keyStoreConfigs.put(restKeyStoreConfiguration.getAlias(), restKeyStoreConfiguration);
        keyStoreConfigs.put(cmmRestKeyStoreConfiguration.getAlias(), cmmRestKeyStoreConfiguration);
        keyStoreConfigs.put(pmKeyStoreConfiguration.getAlias(), pmKeyStoreConfiguration);
        keyStoreConfigs.put(kafkaKeyStoreConfiguration.getAlias(), kafkaKeyStoreConfiguration);
        keyStoreConfigs.put(cmmNotificationEndpointKeyStoreConfiguration.getAlias(), cmmNotificationEndpointKeyStoreConfiguration);
        keyStoreConfigs.put(redisKeyStoreConfiguration.getAlias(), redisKeyStoreConfiguration);
        keyStoreConfigs.put(osmnKeyStoreConfiguration.getAlias(), osmnKeyStoreConfiguration);
    }

    /**
     * Generate PKCS12 Key Store
     */
    public void generateKeyStores() {
        keyStoreConfigs.keySet().forEach(this::regenerateKeyStoreForAlias);
    }

    /**
     * Regenerate a PKCS12 Key Store for alias
     * @param keyStoreAlias regenerate keystore for alias
     */
    public void regenerateKeyStoreForAlias(final KeyStoreAlias keyStoreAlias) {
        final KeyStoreConfiguration configuration = keyStoreConfigs.get(keyStoreAlias);
        if (configuration == null) {
            log.warn("Got null configuration for keystore alias " + keyStoreAlias);
            return;
        }

        if (!configuration.isEmpty()) {
            try {
                generateKeyStore(configuration);
                configuration.postRefreshHook(true);
            } catch (final KeyStoreGenerationException exception) {
                log.warn("Failed to create keystore: {}", exception.toString());
            }
        } else {
            configuration.postRefreshHook(false);
        }
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity"})
    private void checkIfFilesExists(final KeyStoreConfiguration configuration) {
        final Set<CertificateCA> cas = configuration.getAuthorities();
        final Set<CertificateCA> temp = new HashSet<>();
        for (final CertificateCA ca: cas) {
            final Optional<String> caCertificatePath = ca.getCertificatePath();
            if (caCertificatePath.isPresent()) {
                final Path path = Path.of(caCertificatePath.get());
                final String caFile = TLSCertUtils.checkCertsInPathAndTakePreference(path.toString(), "ca");
                if (!caFile.equals(caCertificatePath.get())) {
                    log.debug("Selected CA Path {}", caFile);
                }
                temp.add(new CertificateCA(ca.getAlias(), caFile));
            }
        }
        configuration.setAuthorities(temp);
        updateCertificatePaths(configuration);
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity"})
    private void updateCertificatePaths(final KeyStoreConfiguration configuration) {
        final CertificateKeyEntry certificateKeyEntry = configuration.getCert();
        final CertificateKeyEntry certificateKeyEntryInValues = configuration.getCertDefinedInValues();
        if (certificateKeyEntry != null && certificateKeyEntryInValues != null) {
            final Optional<String> certKeyEntryInValues_certPath = certificateKeyEntryInValues.getCertificatePath();
            final Optional<String> certKeyEntryInValues_PrivKeypath = certificateKeyEntryInValues.getPrivateKeyPath();
            final Optional<String> certKeyEntry_certPath = certificateKeyEntry.getCertificatePath();
            final Optional<String> certKeyEntry_PrivKeyPath = certificateKeyEntry.getPrivateKeyPath();
            if (certKeyEntryInValues_certPath.isPresent()
                    && certKeyEntryInValues_PrivKeypath.isPresent()
                    && certKeyEntry_certPath.isPresent()
                    && certKeyEntry_PrivKeyPath.isPresent()) {
                final String updatedCertPath = TLSCertUtils.checkCertsInPathAndTakePreference(
                        certKeyEntryInValues_certPath.get(), "cert");
                if (!updatedCertPath.equals(certKeyEntry_certPath.get())) {
                    log.debug("Selected Certificate Path {}", updatedCertPath);
                }

                final String privatePath = TLSCertUtils.checkCertsInPathAndTakePreference(certKeyEntryInValues_PrivKeypath.get(), "priv");
                if (privatePath != null && !privatePath.equals(certKeyEntry_PrivKeyPath.get())) {
                    log.debug("Selected Certificate Path {}", privatePath);
                }
                if (!TLSCertUtils.isCertificateExpired(updatedCertPath)) {
                    configuration.setCert(new CertificateKeyEntry(configuration.getAlias().toString(),
                            privatePath, updatedCertPath));
                    if (expiredCerts.contains(updatedCertPath)) {
                        expiredCerts.remove(updatedCertPath);
                        SecurityEventLogger.logSecurityInfoEvent(
                                CLASS_STRING, BRO_CERT_ISSUE_CATEGORY,
                                "Certificate Issue Cleared: The certificate " + updatedCertPath
                                + " is now valid and operational.");
                    }
                } else {
                    if (!expiredCerts.contains(updatedCertPath)) {
                        expiredCerts.add(updatedCertPath);
                    }
                }
            }
        }
    }

    private void generateKeyStore(final KeyStoreConfiguration config) {
        final Path tempKeyStoreFile = Path.of(config.getKeyStoreFile().toString() + ".new");
        final File keyStoreDir = new File(FilenameUtils.getPath(config.getKeyStoreFile().toString() + ".new"));
        keyStoreDir.mkdirs();
        checkIfFilesExists(config);
        try (FileOutputStream fileOutputStream =
                     new FileOutputStream(tempKeyStoreFile.toFile())) {
            final KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
            keyStore.load(null); // initialize key store empty
            configureKeyStore(config, keyStore);
            keyStore.store(fileOutputStream, config.getKeyStorePassword().toCharArray());
            // closed before to be moved
            fileOutputStream.close();
            log.debug("KeyStore created <{}>", config.getKeyStoreFile());
            Files.move(tempKeyStoreFile, config.getKeyStoreFile(), ATOMIC_MOVE, REPLACE_EXISTING);
            keyStores.put(config.getAlias(), keyStore);
        } catch (Exception e) {
            log.error("Failed when generating KeyStore for {}.", config.getAlias());
            throw new KeyStoreGenerationException(
                    "Failed to create keystore for config with alias: " + config.getAlias(), e);
        }
    }

    private static void configureKeyStore(final KeyStoreConfiguration config, final KeyStore keyStore) {
        try {
            if (config.getCert() != null ) {
                final CertificateKeyEntry certificateEntry = config.getCert();
                final Optional<PrivateKey> privateKey = certificateEntry.getPrivateKey();
                if (privateKey.isPresent()) {
                    keyStore.setKeyEntry(certificateEntry.getAlias(), privateKey.get(), config.getKeyStorePassword().toCharArray(),
                            certificateEntry.getListCertificates().toArray(new Certificate[0]));
                }
            }
            for (final CertificateCA ca: config.getAuthorities()) {
                keyStore.setCertificateEntry(ca.getAlias(), ca.getCertificates().get(0));
            }
        } catch (Exception e) {
            throw new KeyStoreGenerationException("Error adding certificates into key store.", e);
        }
    }

    /**
     * Retrieves the keyStore for a given alias
     * @param alias - the particular alias assigned to the keystore you want to retrieve,
     *              as defined by it's configuration class init method
     * @return the keystore associated with that alias, or null if there isn't one
     */
    public KeyStore getKeyStore(final KeyStoreAlias alias) {
        return keyStores.get(alias);
    }

    /**
     * Retrieves the keyStore configuration for a given alias
     * @param alias - the particular alias assigned to the keystore you want to retrieve,
     *              as defined by it's configuration class init method
     * @return the keystore configuration associated with that alias, or null if there isn't one
     */
    public KeyStoreConfiguration getKeyStoreConfig(final KeyStoreAlias alias) {
        return keyStoreConfigs.get(alias);
    }

    public String getKeyStoreType() {
        return KEY_STORE_TYPE;
    }

    public List<String> getExpiredCertList() {
        return expiredCerts;
    }

    /**
     * Get a shallow copy of the keystore config map
     *
     * @return a shallow copy of the keystore config map
     * */
    public Map<KeyStoreAlias, KeyStoreConfiguration> getKeyStoreConfigs() {
        return new EnumMap<>(keyStoreConfigs);
    }
}
