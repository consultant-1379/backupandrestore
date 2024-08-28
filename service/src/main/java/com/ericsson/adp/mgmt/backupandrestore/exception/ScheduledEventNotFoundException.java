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
package com.ericsson.adp.mgmt.backupandrestore.exception;

/**
 * Represents failing to find a scheduled event.
 */
public class ScheduledEventNotFoundException extends NotFoundException {

    private static final long serialVersionUID = 4437250094420863447L;

    /**
     * Creates exception.
     * @param eventId id of scheduled event
     */
    public ScheduledEventNotFoundException(final String eventId) {
        super("Scheduled event <" + eventId + "> not found");
    }

}
