/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

import org.junit.Test;
import org.junit.Before;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TLSCertUtilsTest {

    @Mock
    private X509CertificateHolder mockCert;

    @Before
    public void setup() {
        mockCert = Mockito.mock(X509CertificateHolder.class);
    }

    @Test
    public void testIsCertificateExpired_ExpiredCert_ReturnsTrue() {
        when(mockCert.getNotAfter()).thenReturn(Date.from(Instant.now().minusSeconds(100)));
        when(mockCert.getSubject()).thenReturn(new X500Name("CN=testcert"));
        when(mockCert.getIssuer()).thenReturn(new X500Name("CN=testcert"));

        boolean result = TLSCertUtils.isCertificateExpired(mockCert, "testcert");

        assertTrue(result);
        verify(mockCert).getNotAfter();
    }

    @Test
    public void testIsCertificateExpired_ValidCert_ReturnsFalse() {
        when(mockCert.getNotAfter()).thenReturn(Date.from(Instant.now().plusSeconds(100)));
        when(mockCert.getSubject()).thenReturn(new X500Name("CN=testcert"));
        when(mockCert.getIssuer()).thenReturn(new X500Name("CN=testcert"));

        boolean result = TLSCertUtils.isCertificateExpired(mockCert, "testcert");

        assertFalse(result);
    }

    @Test
    public void testIsCertificateExpired_NullDate_ReturnsFalse() {
        when(mockCert.getNotAfter()).thenReturn(null);
        when(mockCert.getSubject()).thenReturn(new X500Name("CN=testcert"));
        when(mockCert.getIssuer()).thenReturn(new X500Name("CN=testcert"));

        boolean result = TLSCertUtils.isCertificateExpired(mockCert, "testcert");

        assertFalse(result);
    }
}
