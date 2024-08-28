/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.rest.scheduler;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON response to scheduled event creation request
 */
public class CreateEventResponse {

    @JsonProperty("id")
    private String eventId;

    /**
     * Default constructor, to be used by Jackson
     */
    public CreateEventResponse() {}

    /**
     * JSON response to scheduled event creation request
     * @param eventId id of created event
     */
    public CreateEventResponse(final String eventId) {
        this.eventId = eventId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(final String eventId) {
        this.eventId = eventId;
    }

}
