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
package com.ericsson.adp.mgmt.brotestagent.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Centralized location of properties.
 */
public class PropertiesHelper {

    private static final Logger log = LogManager.getLogger(PropertiesHelper.class);
    private static final Properties properties = new Properties();

    /**
     * Prevents external instantiation.
     */
    private PropertiesHelper() {}

    /**
     * Loads properties from a file.
     * @param filePath path to the properties file.
     */
    public static void loadProperties(final String filePath) {
        try {
            log.info("Loading properties at {}", filePath);
            properties.load(new FileInputStream(filePath));
            log.info("Loaded properties {}", properties);
        } catch (final IOException e) {
            log.error("Exception loading properties file", e);
        }
    }

    /**
     * Gets property, or default value.
     * @param property to look for.
     * @param defaultValue to be returned if property is not found
     * @return property value.
     */
    public static String getProperty(final String property, final String defaultValue) {
        return properties.getProperty(property, defaultValue);
    }

    /**
     * Gets list of values assigned to a property, or default value.
     * @param property to look for.
     * @param defaultValue to be returned if property is not found
     * @return list of values assigned to a property.
     */
    public static List<String> getPropertyValueList(final String property, final String defaultValue) {
        final String value = properties.getProperty(property, defaultValue);
        return Arrays.asList(value.split(",", -1));
    }

    /**
     * Sets property.
     * @param property to set.
     * @param value to set.
     */
    public static void setProperty(final String property, final String value) {
        properties.put(property, value);
    }

    /**
     * clears the properties.
     */
    public static void clear() {
        properties.clear();
    }

}
