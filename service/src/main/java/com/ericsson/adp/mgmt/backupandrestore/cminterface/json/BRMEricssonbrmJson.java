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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents Ericsson-brm configuration in CM.
 */
public class BRMEricssonbrmJson {

    private String name;
    private String title;
    private BRMConfiguration brmConfiguration;

    /**
     * Default constructor, used by Jackson.
     */
    public BRMEricssonbrmJson() {}

    /**
     * constructor.
     * @param name Ericsson-brm name
     * @param title Ericsson-brm title
     * @param brmConfiguration Data
     */
    public BRMEricssonbrmJson(final String name, final String title, final BRMConfiguration brmConfiguration) {
        super();
        this.name = name;
        this.title = title;
        this.brmConfiguration = brmConfiguration;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }
    @JsonProperty("name")
    public void setName(final String name) {
        this.name = name;
    }

    @JsonProperty("title")
    public String getTitle() {
        return title;
    }

    @JsonProperty("title")
    public void setTitle(final String title) {
        this.title = title;
    }

    @JsonProperty("data")
    public BRMConfiguration getBRMConfiguration() {
        return brmConfiguration;
    }

    @JsonProperty("data")
    public void setData(final BRMConfiguration data) {
        this.brmConfiguration = data;
    }
}
