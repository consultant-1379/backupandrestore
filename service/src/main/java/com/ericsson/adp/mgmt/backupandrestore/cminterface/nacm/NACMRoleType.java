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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.nacm;

/**
 * NACM Role Types
 */
public enum NACMRoleType {

    SYSTEM_ADMIN("system-admin"), SYSTEM_READ_ONLY("system-read-only");

    private final String cmRepresentation;

    /**
     * Creates NACM roles.
     * @param cmRepresentation how CM expects it.
     */
    NACMRoleType(final String cmRepresentation) {
        this.cmRepresentation = cmRepresentation;
    }

    public String getCmRepresentation() {
        return cmRepresentation;
    }

}
