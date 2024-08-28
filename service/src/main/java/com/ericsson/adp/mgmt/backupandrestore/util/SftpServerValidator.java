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
package com.ericsson.adp.mgmt.backupandrestore.util;

import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.CLIENT_LOCAL_PRIVATE_KEY;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.CLIENT_LOCAL_PUBLIC_KEY;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.CLIENT_USERNAME;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.NAME;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.REMOTE_ADDRESS;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.REMOTE_PATH;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.REMOTE_PORT;
import static com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerConstants.SERVER_LOCAL_HOST_KEY;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jasypt.contrib.org.apache.commons.codec_1_3.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.ClientIdentity;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.ClientLocalDefinition;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.Endpoint;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.ServerAuthentication;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.ServerLocalDefinition;
import com.ericsson.adp.mgmt.backupandrestore.backup.manager.sftpserver.SftpServerInformation;
import com.ericsson.adp.mgmt.backupandrestore.kms.CMKeyPassphraseService;
import com.google.common.net.InetAddresses;
import com.jcraft.jsch.HostKey;

/**
 * A utility class for validating the SFTP Server parameters.
 */
@Component
public class SftpServerValidator {

    private static final String ENDPOINT_NAME = "endpoint-name";

    private static final List<String> SUPPORTED_PUBLIC_KEY_ALGORITHMS = List.of("ssh-rsa", "ecdsa-sha2-nistp256", "ecdsa-sha2-nistp384",
                                                                                "ecdsa-sha2-nistp521", "ssh-ed25519", "ssh-dss");

    private static final List<String> KEY_FIELDS = List.of(CLIENT_LOCAL_PRIVATE_KEY.toString(),
                                                           CLIENT_LOCAL_PUBLIC_KEY.toString(),
                                                           SERVER_LOCAL_HOST_KEY.toString());

    private static final Logger log = LogManager.getLogger(SftpServerValidator.class);

    private ESAPIValidator esapiValidator;

    private IdValidator idValidator;

    private CMKeyPassphraseService cmKeyPassphraseService;

    /**
     * Validates the parameters of an SFTP Server.
     * The validation is currently performed on the following:
     * <ul>
     * <li> sftp-server-name
     * <li> endpoint's name
     * <li> remote-address
     * <li> remote-port
     * <li> remote-path
     * <li> username
     * </ul>
     * @param sftpServerInfo the SFTP Server being validated
     * @return true if all the parameters are valid, otherwise false.
     */
    public boolean isValid(final SftpServerInformation sftpServerInfo) {
        final String sftpServerName = sftpServerInfo.getName();
        final boolean isSftpServerName = isAlphaNumericField("sftp-server-name", sftpServerName);

        final Endpoint endpoint = sftpServerInfo.getEndpoints().getEndpoint()[0];
        return isSftpServerName && isValid(endpoint);
    }

    /**
     * Validates the value of a specific parameter of an SFTP Server endpoint.
     * The actual validation is currently only performed on the following:
     * <ul>
     * <li> endpoint's name
     * <li> remote-address
     * <li> remote-port
     * <li> remote-path
     * <li> username
     * </ul>
     * Validating the following parameters will always return true, for now.
     * <ul>
     * <li> private-key
     * <li> public-key
     * <li> host-key
     * </ul>
     * @param property the parameter name based on the Yang model.
     * @param value the new value of the property
     * @return true if the new value is valid,
     *          false if the property is not recognized or if the new value is invalid.
     */
    public boolean isValidEndpoint(final String property, final String value) {
        if (property.equalsIgnoreCase(NAME.toString())) {
            return isAlphaNumericField(value, ENDPOINT_NAME);
        } else if (property.equalsIgnoreCase(REMOTE_ADDRESS.toString())) {
            return isHostAddress(value);
        } else if (property.equalsIgnoreCase(REMOTE_PATH.toString())) {
            return isRemotePath(value);
        } else if (property.equalsIgnoreCase(REMOTE_PORT.toString())) {
            return isRemotePort(Integer.parseInt(value));
        } else {
            return isValidKeyCredentials(property, value);
        }
    }

    /**
     * Validates the value of a specific parameter of an SFTP Server client keys and host keys.
     * <ul>
     * <li> username
     * </ul>
     * Validating the following parameters will always return true, for now.
     * <ul>
     * <li> private-key
     * <li> public-key
     * <li> host-key
     * </ul>
     * @param property the parameter name based on the Yang model.
     * @param value the new value of the property
     * @return true if the new value is valid,
     *          false if the property is not recognized or if the new value is invalid.
     */
    public boolean isValidKeyCredentials(final String property, final String value) {
        if (property.equalsIgnoreCase(CLIENT_USERNAME.toString())) {
            return isAlphaNumericField(CLIENT_USERNAME.toString(), value);
        } else if (property.equalsIgnoreCase(CLIENT_LOCAL_PRIVATE_KEY.toString())) {
            return isPrivateKeyInPemFormat(value);
        } else if (property.equalsIgnoreCase(CLIENT_LOCAL_PUBLIC_KEY.toString())) {
            return isOpenSSHPublicKey(CLIENT_LOCAL_PUBLIC_KEY.toString(), value);
        } else if (property.equalsIgnoreCase(SERVER_LOCAL_HOST_KEY.toString())) {
            return isOpenSSHPublicKey(SERVER_LOCAL_HOST_KEY.toString(), value);
        } else {
            log.warn("The property <{}> being validated is not recognized. Returning <false>", property);
            return false;
        }
    }

    private boolean isOpenSSHPublicKey(final String key, final String value) {
        final String[] publicKeyElements = getSSHPublicKeyElements(value);
        // checks if the public key follows the structure: [algorithm base64-encoded-key optional-comment]
        // and it uses a supported algorithm.
        if (publicKeyElements.length >= 2 && publicKeyElements.length <= 3) {
            final String algorithm = publicKeyElements[0];
            final String base64EncodedKeyContent = publicKeyElements[1];
            return isSupportedAlgorithm(key, algorithm) && isValidPublicKeyContent(key, algorithm, base64EncodedKeyContent);
        } else {
            log.warn("The value of the param <{}>: <******> is invalid as it does not have a correct format.", key);
            return false;
        }
    }

    /**
     * This checks if the base64 encoded content of the public key is valid
     * by constructing an instance of HostKey from the JSch library.
     * While creating a HostKey, JSch parses the content and identifies
     * the type of the key.
     * The content will be considered invalid if:
     * (1) it cannot be parsed by JSch
     * (2) its declared algorithm does not match the type identified by JSch
     * @param key the SftpServer property
     * @param algorithm the declared algorithm of the key
     * @param base64EncodedKey the base64 encoded content
     * @return true if the content is valid, false otherwise.
     */
    private boolean isValidPublicKeyContent(final String key, final String algorithm, final String base64EncodedKey) {
        final byte[] hostKey = decodeBase64(base64EncodedKey).getBytes();
        final String invalidContentErrorMessage = "The value of the param <{}>: <******> is invalid as its content {}.";
        try {
            final HostKey host = new HostKey(null, 0, hostKey);
            if (!host.getType().equals(algorithm)) {
                log.warn(invalidContentErrorMessage, key, "does not use the correct algorithm");
                return false;
            }
            return true;
        } catch (Exception e) {
            log.warn(invalidContentErrorMessage, key, "cannot be parsed");
            return false;
        }
    }

    /**
     * Decodes the base 64 encoded OpenSSH public key
     * and then returns an array containing the host key elements.
     * @param encodedOpenSSHKey a base 64 encoded public key
     * @return An array containing the host key elements [algorithm-type base64-encoded-key optional-comment].
     */
    private String[] getSSHPublicKeyElements(final String encodedOpenSSHKey) {
        return new String(decodeBase64(encodedOpenSSHKey)).trim().split(" ");
    }

    /**
     * Decodes a base 64 encoded key string
     * @param key the base64 encoded key string
     * @return the decoded key string
     */
    private String decodeBase64(final String key) {
        return new String(Base64.decodeBase64(key.getBytes()));
    }

    private boolean isSupportedAlgorithm(final String key, final String algorithm) {
        final boolean isSupportedAlgorithm = isValid(key, algorithm, SUPPORTED_PUBLIC_KEY_ALGORITHMS::contains);
        if (!isSupportedAlgorithm) {
            log.warn("The algorithm <{}> of the <{}> is not supported", algorithm, key);
        }
        return isSupportedAlgorithm;
    }

    /**
     * Checks if the private key is in PEM Format
     * @param key the base64-encoded private key which is encrypted using the CM key
     * @return true if the private key is in PEM format, false otherwise.
     */
    private boolean isPrivateKeyInPemFormat(final String key) {
        String decryptedKey = key;
        if (cmKeyPassphraseService.isEnabled()) {
            decryptedKey = cmKeyPassphraseService.getPassphrase(key);
        }
        final String decodedPrivateKey = decodeBase64(decryptedKey);
        return isValid(CLIENT_LOCAL_PRIVATE_KEY.toString(), decodedPrivateKey, esapiValidator::isValidOpenSSLPrivateKeyFormat);
    }

    /**
     * Checks if the value of a field contains at least one alphanumeric character,
     * an underscore, or a hyphen.
     * @param field the name of the field. Mainly used to log the invalid field name.
     * @param value the value being validated
     * @return true if the field is valid, otherwise false.
     */
    public boolean isAlphaNumericField(final String field, final String value) {
        return isValid(field, value, val -> idValidator.isValidId(val) && esapiValidator.isAlphaNumeric(val));
    }

    /**
     * Checks if the the remote-address is a valid IP address or domain name
     * @param value the remote-address
     * @return true if the remote-address is valid, otherwise false.
     */
    public boolean isHostAddress(final String value) {
        final Predicate<String> validator = val -> InetAddresses.isInetAddress(val) || esapiValidator.isValidHostName(val);
        return isValid(REMOTE_ADDRESS.toString(), value, validator);
    }

    /**
     * Checks if the the remote-path is a valid IP address or domain name
     * @param value the path
     * @return true if the "remote-path" is valid, otherwise false.
     */

    public boolean isRemotePath(final String value) {
        return isValid(REMOTE_PATH.toString(), value, val -> esapiValidator.isValidRemotePath(val));
    }

    /**
     * Checks if the the remote-port is within the valid range [0-65535]
     * @param value the port number
     * @return true if the remote-port is valid, otherwise false.
     */
    public boolean isRemotePort(final int value) {
        return isValid(REMOTE_PORT.toString(), value, val -> val >= 0 && val <= 65535);
    }

    private <T> boolean isValid(final String field, final T value, final Predicate<T> validator) {
        final boolean isValid = validator.test(value);
        String loggedValue = String.valueOf(value);
        if (KEY_FIELDS.contains(field)) {
            loggedValue = "******";
        }
        if (!isValid) {
            log.warn("The value of the param <{}>: <{}> is invalid", field, loggedValue);
        } else {
            log.debug("The value of the param <{}>: <{}> is valid", field, loggedValue);
        }
        return isValid;
    }

    private boolean isValid(final Endpoint endpoint) {
        return isAlphaNumericField(ENDPOINT_NAME, endpoint.getName())
                && isHostAddress(endpoint.getRemoteAddress())
                && isRemotePath(endpoint.getRemotePath())
                && isRemotePort(endpoint.getRemotePort())
                && isValid(endpoint.getClientIdentity())
                && isValid(endpoint.getServerAuthentication());
    }

    private boolean isValid(final ServerAuthentication serverAuthentication) {
        final ServerLocalDefinition localDefinition = serverAuthentication.getSshHostKeys().getLocalDefinition();
        final Collection<String> newHostKeys = localDefinition.getHostKeys();
        newHostKeys.removeIf(key -> !isValidKeyCredentials(SERVER_LOCAL_HOST_KEY.toString(), key));
        return !newHostKeys.isEmpty();
    }

    private boolean isValid(final ClientIdentity clientIdentity) {
        final String username = clientIdentity.getUsername();
        final ClientLocalDefinition localDefinition = clientIdentity.getPublicKey().getLocalDefinition();
        return isValidKeyCredentials(CLIENT_USERNAME.toString(), username)
                && isValidKeyCredentials(CLIENT_LOCAL_PRIVATE_KEY.toString(), localDefinition.getPrivateKey())
                && isValidKeyCredentials(CLIENT_LOCAL_PUBLIC_KEY.toString(), localDefinition.getPublicKey());
    }

    @Autowired
    public void setEsapiValidator(final ESAPIValidator esapiValidator) {
        this.esapiValidator = esapiValidator;
    }

    @Autowired
    public void setIdValidator(final IdValidator idValidator) {
        this.idValidator = idValidator;
    }

    @Autowired
    public void setCmKeyPassphraseService(final CMKeyPassphraseService cmKeyPassphraseService) {
        this.cmKeyPassphraseService = cmKeyPassphraseService;
    }
}
