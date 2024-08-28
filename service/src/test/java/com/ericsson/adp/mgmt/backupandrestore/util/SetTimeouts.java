/*
 *  ******************************************************************************
 *  COPYRIGHT Ericsson 2020
 *
 *  The copyright to the computer program(s) herein is the property of
 *  Ericsson Inc. The programs may be used and/or copied only with written
 *  permission from Ericsson Inc. or in accordance with the terms and
 *  conditions stipulated in the agreement/contract under which the
 *  program(s) have been supplied.
 *  *******************************************************************************
 *
 */

package com.ericsson.adp.mgmt.backupandrestore.util;

/**
 * Utility interface to allow for a longer time period on the unit tests when debugging.
 * With the small time interval used for automation the tests end before an analysis can be done.
 */
public interface SetTimeouts {
    int TIMEOUT_SECONDS = 10;
}
