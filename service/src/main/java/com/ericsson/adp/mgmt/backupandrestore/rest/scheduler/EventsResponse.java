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

import java.util.ArrayList;
import java.util.List;

/**
 * JSON response with all schedulings of a backupManager.
 */
public class EventsResponse {

    private final List<EventResponse> events = new ArrayList<>();

    public List<EventResponse> getEvents() {
        return events;
    }

}
