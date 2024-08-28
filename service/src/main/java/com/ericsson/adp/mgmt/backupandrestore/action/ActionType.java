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
package com.ericsson.adp.mgmt.backupandrestore.action;

/**
 * Represents all actions that can be performed on a backupManager and produce a measurable result.
 */
public enum ActionType {

    CREATE_BACKUP, DELETE_BACKUP, RESTORE, IMPORT, EXPORT, HOUSEKEEPING, HOUSEKEEPING_DELETE_BACKUP;

}
