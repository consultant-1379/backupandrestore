/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

/**
 * This class provides functionalities to log security events
 */
public class SecurityEventLogger {

    private static final String CATEGORY_NAME = "category";
    private static final String FACILITY_NAME = "facility";
    private static final String SECURITY_EVENT_LOGGER_NAME = "SECURITY_EVENT";
    private static final String BRO_SECURITY_MSG_FACILITY = "security/authorization messages";
    //Logger name is tightly coupled with log4j2.xml configuration
    private static final Logger logger = LogManager.getLogger(SECURITY_EVENT_LOGGER_NAME);

    private SecurityEventLogger() {}

    /**
     * Log the security event as error using ThreadContext mechanism
     *
     * @param classString class name of the caller
     * @param category category to be used in log
     * @param message  message to be logged
     */
    public static void logSecurityErrorEvent(final String classString, final String category, final String message) {
        logSecurityEvent(classString, category, logger::error, message);
    }

    /**
     * Log the security event as warn using ThreadContext mechanism
     *
     * @param classString class name of the caller
     * @param category category to be used in log
     * @param message  message to be logged
     */
    // public static void logSecurityWarnEvent(String category, String message) {
    //     logSecurityEvent(category, logger::warn, message);
    // }

    /**
     * Log the security event as info using ThreadContext mechanism
     *
     * @param classString class name of the caller
     * @param category category to be used in log
     * @param message  message to be logged
     */
    public static void logSecurityInfoEvent(
        final String classString, final String category, final String message) {
        logSecurityEvent(classString, category, logger::info, message);
    }

    private static void logSecurityEvent(
        final String classString, final String category,
        final Consumer<String> messageConsumer, final String message) {
        try {
            ThreadContext.put(CATEGORY_NAME, category);
            ThreadContext.put(FACILITY_NAME, BRO_SECURITY_MSG_FACILITY);
            messageConsumer.accept(message + ", from class: " + classString);
        } finally {
            // Need to be cleared so that next log events shouldn't use current category
            ThreadContext.clearAll();
        }
    }
}
