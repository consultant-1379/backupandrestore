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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JSchSocketFactoryDscpTest {
    private Integer dscp;
    private Integer tos;

    public JSchSocketFactoryDscpTest(int dscp, int tos) {
        this.dscp = dscp;
        this.tos = tos;
    }

    /**
     * This parameters represent a mapping
     * from DSCP to TOS values.
     * @return
     */
    @Parameterized.Parameters
    public static Collection primeNumbers() {
       return Arrays.asList(new Integer[][] {
          { 0, 0x00 },
          { 1, 0x04 },
          { 2, 0x08 },
          { 30, 0x78 },
          { 32, 0x80 },
          { 61, 0xF4 },
          { 62, 0xF8 },
          { 63, 0xFC }
       });
    }

    @Test
    public void validDscpValue() {
        final JSchSocketFactory socketFactory = new JSchSocketFactory(this.dscp.byteValue(), 10000);
        assertEquals(socketFactory.getTOS(), tos.intValue());
    }
}
