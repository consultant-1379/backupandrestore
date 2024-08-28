/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.external.connection;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * The logger which implements JSch logger interface, and ships the JSch log to Log4j logger
 */
public class JschToLog4j implements com.jcraft.jsch.Logger{
    private static final Logger log4jLogger = LogManager.getLogger(JschToLog4j.class);
    private static final Map<Integer, Level> levels = new HashMap<>();

    static  {
        levels.put(DEBUG, Level.DEBUG);
        levels.put(INFO, Level.INFO);
        levels.put(WARN, Level.WARN);
        levels.put(ERROR, Level.ERROR);
        levels.put(FATAL, Level.FATAL);
    }

    @Override
    public boolean isEnabled(final int logLevel) {
        return true;
    }

    @Override
    public void log(final int logLevel, final String log) {
        Level level = levels.get(logLevel);
        if (level == null) {
            level = Level.DEBUG;
        }
        log4jLogger.log(level, log);
    }
}
