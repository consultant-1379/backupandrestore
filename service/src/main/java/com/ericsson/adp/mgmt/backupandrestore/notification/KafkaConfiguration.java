/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2022
 * <p>
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */
package com.ericsson.adp.mgmt.backupandrestore.notification;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Kafka Configuration
 */
@Configuration
@ConfigurationProperties(prefix = "kafka")
@PropertySource({"classpath:application.properties"})
public class KafkaConfiguration {
    private int partitions;
    private short replicationFactor;
    private String hostname;
    private String port;
    private String tlsPort;

    public int getPartitions() {
        return partitions;
    }

    public void setPartitions(final int partitions) {
        this.partitions = partitions;
    }

    public short getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(final short replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(final String hostname) {
        this.hostname = hostname;
    }

    public String getPort() {
        return port;
    }

    public void setPort(final String port) {
        this.port = port;
    }

    public String getTlsPort() {
        return tlsPort;
    }

    public void setTlsPort(final String tlsPort) {
        this.tlsPort = tlsPort;
    }

}
