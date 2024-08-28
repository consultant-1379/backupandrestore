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

import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidExternalClientProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientProperties;

/**
 * ExternalClientPropertiesValidator validates External Storage Properties
 */
@Component
public class ExternalClientPropertiesValidator {

    private static final Logger log = LogManager.getLogger(ExternalClientPropertiesValidator.class);

    private static final String HOST = "host";
    private static final String PORT = "port";
    private static final String USER = "user";
    private static final String EXTERNAL_CLIENT_PATH = "externalClientPath";
    private static final String URI = "uri";

    private static final String ERROR_MESSAGE = " attribute is invalid - can neither be 0 nor null nor empty";

    /**
     * validateExternalClientProperties helps to validate the parsed URI properties
     *
     * @param externalClientProperties
     *            externalClientProperties
     */
    public void validateExternalClientProperties(final ExternalClientProperties externalClientProperties) {

        final StringBuilder errorMessageBuilder = new StringBuilder();

        if (externalClientProperties.isUsingHttpUriScheme()) {
            validateExternalClientPropertiesAttribute(externalClientProperties.getUri(), URI, errorMessageBuilder);
        } else {
            validateExternalClientPropertiesAttribute(externalClientProperties.getHost(), HOST, errorMessageBuilder);
            validateExternalClientPropertiesAttribute(externalClientProperties.getPort(), PORT, errorMessageBuilder);
            validateExternalClientPropertiesAttribute(externalClientProperties.getUser(), USER, errorMessageBuilder);
            validateExternalClientPropertiesAttribute(externalClientProperties.getExternalClientPath(), EXTERNAL_CLIENT_PATH, errorMessageBuilder);
        }

        final String errorMessage = errorMessageBuilder.toString();

        if (!errorMessage.isEmpty()) {
            throw new InvalidExternalClientProperties(errorMessage);
        }

        log.debug("external client properties are valid");
    }

    private void validateExternalClientPropertiesAttribute(final Object value, final String attributeName, final StringBuilder errorMessageBuilder) {

        if ((value instanceof Integer && ((int) value == 0)) || value == null || (value instanceof String && ((String) value).isEmpty())) {
            addErrorMessage(attributeName, errorMessageBuilder);
        }

    }

    private void addErrorMessage(final String attributeName, final StringBuilder errorMessageBuilder) {
        addCommasIfMessagesAlreadyThere(errorMessageBuilder);
        errorMessageBuilder.append(attributeName).append(ERROR_MESSAGE);
    }

    private void addCommasIfMessagesAlreadyThere(final StringBuilder errorMessageBuilder) {

        if (errorMessageBuilder.length() > 0) {
            errorMessageBuilder.append(", ");
        }
    }

}
