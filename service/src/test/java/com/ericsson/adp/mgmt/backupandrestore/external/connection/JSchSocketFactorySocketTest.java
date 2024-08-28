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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.easymock.EasyMock;
import org.junit.Test;

public class JSchSocketFactorySocketTest {

    @Test
    public void getSocketInputStream() throws IOException {
        final JSchSocketFactory socketFactory = new JSchSocketFactory((byte) 0, 10);
        Socket socket = EasyMock.createMock(Socket.class);
        InputStream stream = EasyMock.createMock(InputStream.class);
        EasyMock.expect(socket.getInputStream()).andReturn(stream).times(1);
        EasyMock.replay(socket, stream);
        assertEquals(stream, socketFactory.getInputStream(socket));
        EasyMock.verify(socket);
    }

    @Test
    public void getSocketOutputStream() throws IOException {
        final JSchSocketFactory socketFactory = new JSchSocketFactory((byte) 0, 10);
        Socket socket = EasyMock.createMock(Socket.class);
        OutputStream stream = EasyMock.createMock(OutputStream.class);
        EasyMock.expect(socket.getOutputStream()).andReturn(stream).times(1);
        EasyMock.replay(socket, stream);
        assertEquals(stream, socketFactory.getOutputStream(socket));
        EasyMock.verify(socket);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeDscpIsInvalid() {
        new JSchSocketFactory((byte) -1, 1000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void aDscpGreaterThan63IsInvalid() {
        new JSchSocketFactory((byte) 64, 1000);
    }
}
