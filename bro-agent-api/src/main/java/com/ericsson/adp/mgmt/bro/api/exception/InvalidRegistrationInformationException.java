/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.bro.api.exception;

/**
 * An exception thrown by the validate() method of
 * {@link com.ericsson.adp.mgmt.bro.api.registration.RegistrationInformation}
 */
public class InvalidRegistrationInformationException extends RuntimeException {

    private static final long serialVersionUID = -3930964677780754300L;

    /**
     * A list of error messages
     */
    public enum ErrorMessage {
        AGENT_ID_IS_NULL("agentID is null."),
        SCOPE_IS_NULL("scope is null."),
        API_VERSION_IS_NULL("apiVersion is null."),
        SOFTWARE_VERSION_IS_NULL("softwareVersion is null."),
        AGENT_ID_IS_BLANK("agentID is blank."),
        SOFTWARE_VERSION_INVALID("softwareVersion is invalid."),
        BLANK("");


        private final String message;

        /**
         * @param message - The message for the instance of {@link ErrorMessage} to be created.
         */
        ErrorMessage(final String message) {
            this.message = message;
        }

        /**
         * @return - A {@link String} containing the message for this instance of
         * {@link ErrorMessage}
         */
        public String getMessage() {
            return message;
        }
    }

    /**
     * Constructor.
     */
    public InvalidRegistrationInformationException() {
        super("An instance of RegistrationInformation is invalid.");
    }

    /**
     * Another constructor.
     *
     * @param message - The error message to send with the exception.
     */
    public InvalidRegistrationInformationException(final ErrorMessage message) {
        super(message.getMessage());
    }
}
