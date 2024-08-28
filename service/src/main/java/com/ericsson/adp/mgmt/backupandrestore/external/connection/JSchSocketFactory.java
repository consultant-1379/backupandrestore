/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/

package com.ericsson.adp.mgmt.backupandrestore.external.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.jcraft.jsch.SocketFactory;

/**
 * A Custom JSch Socket Factory used to define the
 * TOS for the egress traffic from BRO to the SFTP Server
 *
 * NOTE: In the event that an exception occurs when the socket is connected
 * to the SFTP Server, the JSch session will catch the exception and
 * automatically close the socket.
 */
public class JSchSocketFactory implements SocketFactory{
    private static final Logger log = LogManager.getLogger(JSchSocketFactory.class);
    private final byte dscp;
    private final int connectionTimeout;

    /**
     * A Custom JSch socket factory that allows setting a DSCP and
     * connection timeout values.
     * @param dscp The Differentiated Services Code Point (DSCP) value. Allowed range is [0..63]
     * @param timeOut The connection timeout in milliseconds
     */
    public JSchSocketFactory(final byte dscp, final int timeOut) {
        if (dscp < 0 || dscp > 63) {
            throw new IllegalArgumentException("The dscp is not in range [0..63]");
        }
        this.dscp = dscp;
        this.connectionTimeout = timeOut;
    }

    /*
    * Suppressing Sonar warning as the socket should
    * not be closed immediately
    */
    @SuppressWarnings("squid:S2095")
    @Override
    public Socket createSocket(final String host, final int port) throws IOException {
        final Socket socket = new Socket();
        final int tos = getTOS();
        final String tosInHex = Integer.toHexString(tos);
        log.info("Setting the SFTP client socket's TOS to <{}>", tosInHex);
        socket.setTrafficClass(tos);
        final InetAddress address = InetAddress.getByName(host);
        socket.connect(new InetSocketAddress(address, port), this.connectionTimeout);
        return socket;
    }

    @Override
    public InputStream getInputStream(final Socket socket) throws IOException {
        return socket.getInputStream();
    }

    @Override
    public OutputStream getOutputStream(final Socket socket) throws IOException {
        return socket.getOutputStream();
    }

    /**
     * The Type of Service(TOS) value is an 8-bit field, with the first 6 bits
     * used to represent the DSCP, and the remaining 2 bits used to represent the
     * Explicit Congestion Notification (ECN). See Differentiated Services Field
     * Definition Section in https://www.rfc-editor.org/rfc/rfc2474)
     *
     * This method involves the conversion of the DSCP value to TOS value through a
     * binary operation wherein the DSCP value undergoes a left-shift operation by
     * two bits. For instance, a DSCP value of 30 (00011110 in binary) will be
     * transformed to a TOS value of 120 (01111000 in binary).
     * @return the Type Of Service (TOS) value
     */
    protected int getTOS() {
        return (this.dscp << 2) & 0xFF;
    }
}
