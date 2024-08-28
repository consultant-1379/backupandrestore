/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */

package com.ericsson.adp.mgmt.backupandrestore.rest.health;

/**
 * Enumerates availability states of the Backup and Restore Orchestrator. Each
 * state provides a human readable description via the toString method.
 */
public enum AvailabilityState {
    BUSY {
        @Override
        public String toString() {
            return "Busy";
        }
    },
    AVAILABLE {
        @Override
        public String toString() {
            return "Available";
        }
    }
}
