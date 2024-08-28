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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Request to create a new configuration.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NewConfigurationRequest {

    private String name;
    private String title;
    private BRMConfiguration data = new BRMConfiguration();

    /**
     * Default constructor, to be used by Jackson.
     */
    public NewConfigurationRequest() {}

    /**
     * Creates request.
     * @param name of configuration.
     * @param data of configuration.
     */
    public NewConfigurationRequest(final String name, final BRMConfiguration data) {
        this.name = name;
        this.title = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public BRMConfiguration getData() {
        return data;
    }

    public void setData(final BRMConfiguration data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "NewConfigurationRequest [name=" + name + ", title=" + title + ", data=" + data + "]";
    }

}
