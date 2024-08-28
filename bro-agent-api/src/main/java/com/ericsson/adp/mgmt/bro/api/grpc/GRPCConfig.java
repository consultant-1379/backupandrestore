/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.bro.api.grpc;

/**
 * Reads GRPC configuration from environment variables
 */
public enum GRPCConfig {

    /**
     * Time, in seconds, to wait for the data channel to be ready before aborting the backup
     */
    AGENT_DATA_CHANNEL_TIMEOUT_SECS(30),

     /**
     * Maximum Size, in bytes, of fragment chunk to send with each backup data message
     */
    AGENT_FRAGMENT_CHUNK_SIZE(512, 1024);

    private final int value;

    /**
     * Constructor to provide a default value and multiplication factor
     *
     * @param defaultValue default value for the configuration if the environment variable is not set
     * @param multiplier multiplication factor to apply to the read value
     */
    GRPCConfig(final int defaultValue, final int multiplier) {
        value = getIntEnvOrDefault(this.name(), defaultValue) * multiplier;
    }

    /**
     * Constructor to provide a default value using 1 as multiplication factor
     *
     * @param defaultValue default value for the configuration if the environment variable is not set
     */
    GRPCConfig(final int defaultValue) {
        this(defaultValue, 1);
    }

    /**
     * Returns the environment variable value as integer or a default value if it is not set.
     *
     * @param envVarName environment variable name
     * @param defaultValue default value to return if the environment variable is not set
     * @return environment variable or default value
     * @exception NumberFormatException if environment variable value is not valid
     */
    static int getIntEnvOrDefault(final String envVarName, final int defaultValue) {
        final int result;
        final String envVarValue = System.getenv(envVarName);
        if (envVarValue == null) {
            result = defaultValue;
        } else {
            try {
                result = Integer.parseInt(envVarValue);
            } catch (NumberFormatException e) {
                throw new NumberFormatException(
                        String.format("%s - Invalid Environment Variable value '%s'", envVarName, envVarValue));
            }
        }
        return result;
    }

    /**
     * Return the configured value
     * @return the configured value
     */
    public int getValue() {
        return value;
    }

}