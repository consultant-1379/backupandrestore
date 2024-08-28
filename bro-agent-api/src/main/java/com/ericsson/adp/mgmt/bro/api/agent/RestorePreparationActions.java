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
package com.ericsson.adp.mgmt.bro.api.agent;

/**
 * Provides means to perform some preparatory actions prior to restore
 */
public class RestorePreparationActions extends RestoreActions {

    /**
     * @param agent The agent performing the restore
     * @param restoreInformation required for restore
     */
    public RestorePreparationActions(final Agent agent, final RestoreInformation restoreInformation) {
        super(agent, restoreInformation);
    }
}
