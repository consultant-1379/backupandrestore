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

import java.nio.file.Path;
import java.security.Security;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ericsson.adp.mgmt.backupandrestore.caching.Cached;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Configuration class for configuring {@link KeyStoreService}
 */
public abstract class KeyStoreConfiguration {
    private static final Logger log = LogManager.getLogger(KeyStoreConfiguration.class);

    protected Path keyStoreFile;
    protected String keystorePassword;
    protected CertificateKeyEntry cert;
    protected CertificateKeyEntry certDefinedInValues;
    protected Set<CertificateCA> authorities;
    protected Set<CertificateCA> authoritiesDefinedInValues;
    protected final Map<Cached<?>, Boolean> listeners = new ConcurrentHashMap<>();

    private final AtomicBoolean validConfiguration;

    /**
     * Constructs KeyStoreConfiguration. Also adds the bouncycastle security provider if it hasn't been added already
     * */
    public KeyStoreConfiguration() {
        validConfiguration = new AtomicBoolean();
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Get's the keystore config path
     * @return the keystore config path
     * */
    public Path getKeyStoreFile() {
        return keyStoreFile;
    }

    /**
     * Get's the keystore config certificate set
     * @return the keystore config certificate set
     * */
    public CertificateKeyEntry getCert() {
        return cert;
    }

    /**
     * Get's the keystore config certificate set
     * @return the keystore config certificate set
     * */
    public CertificateKeyEntry getCertDefinedInValues() {
        return certDefinedInValues;
    }

    /**
     * Add a cached object that should be marked invalid when this keystore is rebuilt
     *
     * NOTE: by default, the Cached<T> is marked invalid on keystore /rebuild/, not keystore /refresh/, that is to say,
     * only when the actual underlying keystore file's contents have changed, not every refresh cycle, and not if the
     * keystore is not in use (isEmpty returns true or isValid returns false). Other KeyStoreConfigurations should override
     * the postRefreshHook to change this behavior as necessary
     * @param listener - the Cached<T> to me marked invalid when the keystore is rebuilt
     * */
    public void addListener(final Cached<?> listener) {
        this.listeners.put(listener, true);
        listener.addDropper(listeners::remove);
    }

    /**
     * Get's the keystore config CA set
     * @return the keystore config CA set
     * */
    public Set<CertificateCA> getAuthorities() {
        return authorities;
    }

    /**
     * Get's the keystore config password
     * @return the keystore config password
     * */
    public String getKeyStorePassword() {
        return keystorePassword;
    }

    /**
     * Get's the keystore config alias
     * @return the keystore config alias
     * */
    public abstract KeyStoreAlias getAlias();

    /**
     * Returns true if this keystore contains nothing
     * @return true if this keystore contains nothing
     * */
    public boolean isEmpty() {
        return cert == null && authorities.isEmpty();
    }


    /**
     * Indicates if the configuration was able to be created with values
     * @param isValid true if the configuration is valid
     */
    public void setValidConfiguration(final boolean isValid) {
        if (log.isDebugEnabled()) {
            log.debug("Keystore {} marked as valid", this::getAlias);
        }
        this.validConfiguration.set(isValid);
    }

    /**
     * Validate the keystore path as valid and not empty
     * if it pass the configuration was created
     * @return true if is valid keystore
     */
    public boolean isValidConfiguration() {
        return validConfiguration.get();
    }

    /**
     * A hook called by the KeyStoreService after a keystore refresh cycle has occurred.
     *
     * By default this marks all registered Cached<T> objects as invalid ONLY if wasRebuilt is true. Children
     * should override this method if they want to implement different cache invalidation patterns
     *
     * @param wasRebuilt - should be true if the underlying keystore files contents were changed, false otherwise
     * */
    public void postRefreshHook(final boolean wasRebuilt) {
        if (wasRebuilt) {
            listeners.keySet().forEach(Cached::invalidate);
        }
    }

    public void setAuthorities(final Set<CertificateCA> authorities) {
        this.authorities = authorities;
    }

    /**
     * Set the keystore config certificate set
     * @param certificateKeyEntry certificateKeyEntry
     * */
    public void setCert(final CertificateKeyEntry certificateKeyEntry) {
        this.cert = certificateKeyEntry;
    }

    /**
     * Used to generate a copy of the authorities at initialisation
     * */
    public void setAuthoritiesDefinedInValues() {
        final Set<CertificateCA> copy = new HashSet<>();
        if (authorities != null) {
            copy.addAll(authorities);
        }
        this.authoritiesDefinedInValues = copy;
    }
}
