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
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.external.client;

import com.ericsson.adp.mgmt.backupandrestore.SpringContext;
import com.ericsson.adp.mgmt.backupandrestore.archive.ArchiveUtils;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.SftpChannelManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.ericsson.adp.mgmt.backupandrestore.exception.ImportExportException;
import com.ericsson.adp.mgmt.backupandrestore.external.ExternalClientProperties;
import com.ericsson.adp.mgmt.backupandrestore.external.connection.SftpConnection;
import com.jcraft.jsch.JSch;

/**
 * Its an SftpClient which can connect with Sftp Server
 */
@Component
@SuppressWarnings({"PMD.CyclomaticComplexity"})
public class SftpClient implements ExternalClient {
    private static final String CONNECTION_ERROR_MESSAGE = "Unable to connect to sftp service";
    private static final int DEFAULT_TIMEOUT_BYTES_RECEIVED_SECONDS = 0;

    private JSch jsch;
    private int sftpChannelTimeout;
    private ArchiveUtils archiveUtils;
    private int timeoutInactivitySeconds = DEFAULT_TIMEOUT_BYTES_RECEIVED_SECONDS;
    private byte egressTrafficDscp;

    /**
     * connect method connects with Sftp Server
     *
     * @param externalClientProperties
     *            externalClientProperties
     * @return SftpConnection
     */
    @Override
    public SftpConnection connect(final ExternalClientProperties externalClientProperties) {
        SftpConnection sftpConn = null;
        try {
            sftpConn = new SftpConnection(getManager(externalClientProperties), archiveUtils);
            sftpConn.setTimeoutBytesReceivedSeconds(timeoutInactivitySeconds);
            sftpConn.connect();
            return sftpConn;
        } catch (Exception e) {
            if (sftpConn != null) {
                sftpConn.close();
            }
            throw new ImportExportException(CONNECTION_ERROR_MESSAGE, e);
        }
    }

    @Autowired
    public void setJsch(final JSch jsch) {
        this.jsch = jsch;
    }

    @Autowired
    public void setArchiveUtils(final ArchiveUtils archiveUtils) {
        this.archiveUtils = archiveUtils;
    }

    /**
     * Sets sftpChannelTimeout of backup and restore rest application.
     *
     * @param sftpTimeout channel timeout in milliseconds
     */
    @Value("${sftpTimeout:5000}")
    public void setSftpChannelTimeout(final int sftpTimeout) {
        this.sftpChannelTimeout = sftpTimeout;
    }

    @Value("${sftp.inactivity.seconds:" + DEFAULT_TIMEOUT_BYTES_RECEIVED_SECONDS + "}")
    public void setTimeoutBytesReceivedSeconds(final int timeoutBytesInactivitySeconds) {
        this.timeoutInactivitySeconds = timeoutBytesInactivitySeconds;
    }

    @Value("${sftp.egress.dscp:0}")
    public void setEgressTrafficDscp(final byte egressTrafficDscp) {
        this.egressTrafficDscp = egressTrafficDscp;
    }

    private SftpChannelManager getManager(final ExternalClientProperties clientProperties) {
        final SftpChannelManager manager = SpringContext.getBean(SftpChannelManager.class).orElseGet(SftpChannelManager::new);
        manager.setJsch(jsch);
        manager.setClientProperties(clientProperties);
        manager.setTimeout(sftpChannelTimeout);
        manager.setEgressTrafficDscp(egressTrafficDscp);
        return manager;
    }
}
