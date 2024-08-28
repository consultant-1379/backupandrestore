/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.Validator;
import org.springframework.stereotype.Component;

import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidBackupNameException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidIdException;
import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidURIException;

/**
 * ESAPIValidator initializes esapi's validator and validates backup name based on regular expression pattern set in ESAPI.properties
 */
@Component
public class ESAPIValidator {

    private static final Logger log = LogManager.getLogger(ESAPIValidator.class);

    private static final String ESAPI_CONTEXT = "ESAPI_VALIDATION";

    private static final int CALENDAR_TIME_MAX_LENGTH = 8;
    private static final int CALENDAR_DATETIME_MAX_LENGTH = 45;
    private static final int ID_MAX_LENGTH = 200;
    private static final int URI_MAX_LENGTH = 2000;
    private static final int HOST_ADDRESS_MAX_LENGTH = 253;

    private final Validator validator = ESAPI.validator();

    /**
     * Validates backupName with the help of esapi's validator.
     *
     * @param backupName
     *            to be validated.
     */
    public void validateBackupName(final String backupName) {
        if (!isValidBackupName(backupName)) {
            throw new InvalidBackupNameException("Failing for having an invalid backup name");
        }
    }

    /**
     * Validates backupName with the help of esapi's validator.
     *
     * @param backupName
     *            to be validated.
     * @return isValid
     */
    public boolean isValidBackupName(final String backupName) {
        if (!validator.isValidInput(ESAPI_CONTEXT, backupName, "BackupName", ID_MAX_LENGTH, false)) {
            return false;
        }
        log.debug("backupName <{}> is valid", backupName);
        return true;
    }

    /**
     * Validates eventId with the help of esapi's validator.
     *
     * @param eventId
     *            to be validated.
     */
    public void validateEventId(final String eventId) {
        if (!validator.isValidInput(ESAPI_CONTEXT, eventId, "eventId", ID_MAX_LENGTH, false)) {
            log.info("periodic event id <{}> is invalid", eventId);
            throw new InvalidIdException("Failing for having an invalid periodic event id");
        }
        log.debug("periodic event id <{}> is valid", eventId);
    }

    /**
     * Validate URI with help of esapi's validator
     *
     * @param uri
     *            uri
     */
    public void validateURI(final URI uri) {
        final String uriS = uri.toString();
        if (!isValidURI(uriS)) {
            throw new InvalidURIException("Failing for having an invalid URI");
        }
    }

    /**
     * Validate URI with help of esapi's validator
     *
     * @param uri
     *            uri
     * @return isValid
     */
    public boolean isValidURI(final String uri) {
        return validator.isValidInput(ESAPI_CONTEXT, uri, "SFTP_URI", URI_MAX_LENGTH, false) ||
                validator.isValidInput(ESAPI_CONTEXT, uri, "HTTP_URI", URI_MAX_LENGTH, false);
    }

    /**
     * Validates Calendar Event's time with the help of esapi's validator.
     * @param time the time from the calendar event
     * @return if the time is valid or not
     */
    public boolean isValidCalendarTime(final String time) {
        return validator.isValidInput(ESAPI_CONTEXT, time, "CALENDAR_TIME", CALENDAR_TIME_MAX_LENGTH, false);
    }

    /**
     * Validates if a string value contains at least one alphanumeric character
     * @param value the value being validated
     * @return true if the value is valid, otherwise false.
     */
    public boolean isAlphaNumeric(final String value) {
        return validator.isValidInput(ESAPI_CONTEXT, value, "ALPHANUMERIC", ID_MAX_LENGTH, false);
    }

    /**
     * Validates if a host address is a valid domain name
     * This validates the host name according to RFC 1034 Domain Names - Concepts and Facilities in November 1987.
     * Example Syntax: <label-node>.<label-node>
     * A host name can have one or more label-nodes separated by a dot.
     * Each label-node should be 63 characters or less, and should only contain alphanumeric characters and hyphen
     * The total length of the host name should not exceed 253 characters.
     * @param hostName the host name being validated
     * @return true if the host name is valid, otherwise false.
     */
    public boolean isValidHostName(final String hostName) {
        return validator.isValidInput(ESAPI_CONTEXT, hostName, "HOSTNAME", HOST_ADDRESS_MAX_LENGTH, false);
    }

    /**
     * Validates if a remote path is valid
     * @param path the path being validated
     * @return true if the path is valid, otherwise false.
     */
    public boolean isValidRemotePath(final String path) {
        return validator.isValidInput(ESAPI_CONTEXT, path, "REMOTEPATH", ID_MAX_LENGTH, false);
    }

    /**
     * Checks if the private key is in OpenSSL PEM format
     * @param key the private key
     * @return true if the private key format is valid, otherwise false.
     */
    public boolean isValidOpenSSLPrivateKeyFormat(final String key) {
        return validator.isValidInput(ESAPI_CONTEXT, key, "PEMPRIVATEKEY", Integer.MAX_VALUE, false);
    }

    /**
     * Validates Calendar Event's datetime with the help of esapi's validator.
     * @param datetime the datetime from the calendar event
     * @return if the datetime is valid or not
     */
    public boolean isValidCalendarDateTime(final String datetime) {
        return validator.isValidInput(ESAPI_CONTEXT, datetime, "CALENDAR_DATETIME", CALENDAR_DATETIME_MAX_LENGTH, false);
    }
}
