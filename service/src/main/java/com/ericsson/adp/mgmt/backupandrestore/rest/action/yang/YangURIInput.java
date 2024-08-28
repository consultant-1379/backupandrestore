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

import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.URI;

/**
 * Input uri and password.
 */
public class YangURIInput implements YangSftpServerInput{

    @JsonProperty("ericsson-brm:uri")
    private URI uri;
    @JsonProperty("ericsson-brm:password")
    private String password;

    public URI getUri() {
        return uri;
    }

    public void setUri(final URI uri) {
        this.uri = uri;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

}
