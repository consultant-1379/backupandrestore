/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.persist;

import com.ericsson.adp.mgmt.backupandrestore.aws.S3Config;
import com.ericsson.adp.mgmt.backupandrestore.aws.service.S3MultipartClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/**
 * Persistence provider configuration class, used as factory service for persistence providers
 * */
@Configuration
public class PersistProviderFactory {
    private S3Config s3Config;

    /**
     * Construct a persistence provider for a given FileService
     * @return a persistence provider configured using the passed FileService
     * */
    public PersistProvider getPersistProvider() {
        return (s3Config != null && s3Config.isEnabled()) ?
                new S3PersistProvider(new S3MultipartClient(s3Config)) :
                new PVCPersistProvider();
    }

    @Autowired
    public void setAwsConfig(final S3Config s3Config) {
        this.s3Config = s3Config;
    }
}
