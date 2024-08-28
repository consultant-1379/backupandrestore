/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.action.payload;

/**
 * Empty payload used for requests without payload details.
 */
public class EmptyPayload implements Payload {
    protected String data = "";

    /**
     * Empty payload constructor
     */
    public EmptyPayload() {
        super();
    }

    /**
     * Creates a payload and fills the content with some data
     * @param data Data to be included
     */
    public EmptyPayload(final String data) {
        super();
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }


    @Override
    public String toString() {
        return "Payload [" + data + "]";
    }
}
