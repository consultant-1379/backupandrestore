/**
 * ------------------------------------------------------------------------------
 * ******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of Ericsson
 * Inc. The programs may be used and/or copied only with written permission from
 * Ericsson Inc. or in accordance with the terms and conditions stipulated in
 * the agreement/contract under which the program(s) have been supplied.
 * ******************************************************************************
 * ------------------------------------------------------------------------------
 */
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Mediator Patch notification
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediatorSFTPRequestPatch extends MediatorRequestPatch{
    public static final String SFTP_SERVER_PATH_REGEX = "(.*?)(\\d+)(/sftp-server)((/.*)+)";
    public static final String SFTP_SERVER_PATH_REGEX_NO_INDEX = "(.*?)(\\d+)(/sftp-server)((.*)+)";

    @JsonProperty("op")
    private String operations;
    @JsonProperty("path")
    private String path;
    @JsonProperty("value")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<Object> value;

    @JsonProperty("op")
    public String getOp() {
        return operations;
    }

    @JsonProperty("path")
    public String getPath() {
        return path;
    }

    @JsonProperty("value")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public List<Object> getValue() {
        return value;
    }

    @JsonProperty("value")
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public void setValue(final List<Object> value) {
        this.value = value;
    }

    @JsonProperty("op")
    public void setOp(final String operations) {
        this.operations = operations;
    }

    @JsonProperty("path")
    public void setPath(final String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append("Patch [operations=" + operations + ", path=" + path + ", value=[");
        value.forEach(obj -> result.append(obj).append(", "));
        if (!value.isEmpty()) {
            result.setLength(result.length() - 2);
        }
        result.append("]]");
        return result.toString();
    }

}
