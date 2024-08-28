/**------------------------------------------------------------------------------
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
package com.ericsson.adp.mgmt.backupandrestore.rest.action.yang;

/**
 * Request with context.
 */
public class YangActionRequest {

    private String context;
    private String configETag;

    public String getContext() {
        return context;
    }

    public void setContext(final String context) {
        this.context = context;
    }

    public String getConfigETag() {
        return configETag;
    }

    public void setConfigETag(final String configETag) {
        this.configETag = configETag;
    }

}
