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
package com.ericsson.adp.mgmt.backupandrestore.util;

/**
 * Utility class to help with OS related information
 *
 */
public class OSUtils {

    private OSUtils() {

    }

    /**
     * Check if running on Windows system
     *
     * @return true if OS is Windows, false for other OS
     */
    public static boolean isWindows() {
        return System.getProperty("os.name").startsWith("Windows");
    }

    /**
     * Sleep for certain milliseconds
     * @param milliseconds the time for sleep
     */
    public static void sleep(final long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
