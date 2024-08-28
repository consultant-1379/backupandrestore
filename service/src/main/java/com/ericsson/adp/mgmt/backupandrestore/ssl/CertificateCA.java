/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.ssl;

/**
 * POJO class used for CA certificates
 */
public class CertificateCA extends CertificatesCommon{

    /**
     * @param certificateCAPath Path for the CA Certificate
     * @param certificateCAAlias Alias assigned to this certificate
     */
    public CertificateCA(final String certificateCAAlias, final String certificateCAPath) {
        super();
        setCertificatePath(certificateCAPath);
        setAlias(certificateCAAlias);
    }

}
