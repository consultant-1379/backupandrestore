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
public class InvalidSoftwareVersionException extends RuntimeException {

    private static final long serialVersionUID = -5325577198020303471L;

    /**
     * A list of error messages
     */
    public enum ErrorMessage {
        PRODUCT_NAME_IS_NULL("productName is null."),
        PRODUCT_NUMBER_IS_NULL("productNumber is null."),
        REVISION_IS_NULL("revision is null."),
        PRODUCTION_DATE_IS_NULL("productionDate is null."),
        DESCRIPTION_IS_NULL("description is null."),
        TYPE_IS_NULL("type is null."),
        COMMERCIALVERSION_IS_NULL("commercialVersion is null"),
        SEMANTICVERSION_IS_NULL("semanticVersion is null"),
        PRODUCT_NAME_IS_BLANK("productName is blank."),
        PRODUCT_NUMBER_IS_BLANK("productNumber is blank."),
        REVISION_IS_BLANK("revision is blank."),
        PRODUCTION_DATE_IS_BLANK("productionDate is blank."),
        DESCRIPTION_IS_BLANK("description is blank."),
        TYPE_IS_BLANK("type is blank."),
        COMMERCIALVERSION_IS_BLANK("commercialVersion is blank"),
        SEMANTICVERSION_IS_BLANK("semanticVersion is blank");


        private final String message;

        /**
         * @param message - The message for the instance of {@link ErrorMessage} to be created.
         */
        ErrorMessage(final String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * Constructor.
     */
    public InvalidSoftwareVersionException() {
        super("An instance of SoftwareVersion is invalid.");
    }

    /**
     * Another constructor.
     *
     * @param message - The error message to send with the exception.
     */
    public InvalidSoftwareVersionException(final ErrorMessage message) {
        super(message.getMessage());
    }
}
