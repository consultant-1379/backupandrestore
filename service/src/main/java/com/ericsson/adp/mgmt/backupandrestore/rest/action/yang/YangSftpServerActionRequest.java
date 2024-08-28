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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

/**
 * Request with context, uri and password.
 */
public class YangSftpServerActionRequest extends YangActionRequest {

    @JsonTypeInfo(use = Id.DEDUCTION)
    @JsonSubTypes({ @Type(YangURIInput.class), @Type(YangSftpServerNameInput.class) })
    private YangSftpServerInput input;

    /**
     * Empty constructor, to be used by Jackson.
     */
    public YangSftpServerActionRequest() {}

    /**
     * Creates request
     * @param input with uri and password
     */
    public YangSftpServerActionRequest(final YangSftpServerInput input) {
        this.input = input;
    }

    public YangSftpServerInput getInput() {
        return input;
    }

    public void setInput(final YangSftpServerInput input) {
        this.input = input;
    }

}
