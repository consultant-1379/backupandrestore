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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.operation;

import java.util.List;

import com.ericsson.adp.mgmt.backupandrestore.cminterface.EtagNotifIdBase;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchOperationJson;
import com.ericsson.adp.mgmt.backupandrestore.cminterface.json.PatchRequest;

/**
 * Represents a Patch to CM.
 */
public abstract class ConfigurationPatch {

    protected static final String INDEX_TO_ADD_ELEMENT = "-";

    protected PatchOperation operation;
    protected String path;
    private EtagNotifIdBase etagNotifIdBase;

    /**
     * Creates patch.
     * @param operation to be performed.
     * @param path where it will be performed.
     */
    protected ConfigurationPatch(final PatchOperation operation, final String path) {
        this.operation = operation;
        this.path = path;
        this.etagNotifIdBase = new EtagNotifIdBase();
    }

    /**
     * Transforms patch to json object.
     * @return json representation.
     */
    public PatchRequest toJson() {
        return new PatchRequest(getJsonOfOperations(), etagNotifIdBase != null ? etagNotifIdBase.getEtag() : "");
    }

    /**
     * Gets all operations involved in this patch.
     * @return operations as json objects.
     */
    protected abstract List<PatchOperationJson> getJsonOfOperations();

    /**
     * Creates json.
     * @param path where operation will be performed.
     * @param value new value.
     * @return json object.
     */
    protected PatchOperationJson createOperationJson(final String path, final Object value) {
        final PatchOperationJson json = new PatchOperationJson();
        json.setOperation(operation.name().toLowerCase());
        json.setPath(path);
        json.setValue(value);
        return json;
    }

    public void setEtagNotifIdBase(final EtagNotifIdBase etagNotifIdBase) {
        this.etagNotifIdBase = etagNotifIdBase;
    }

    /**
     * Update the etag value if the message already exist
     * @param etag Etag to be updated
     */
    public void setEtag(final String etag) {
        this.etagNotifIdBase.updateEtag(etag);
    }

    public String getEtag() {
        return etagNotifIdBase.getEtag();
    }

    public void setPath(final String path) {
        this.path = path;
    }

    /**
     * Returns Path to be updated
     * @return Path where the configuration will be executed
     */
    public String getPath() {
        return path;
    }

    public void setOperation(final PatchOperation operation) {
        this.operation = operation;
    }

    /**
     * Returns Operation to be executed in the patch
     * @return Operation to be executed in the patch
     */
    public PatchOperation getOperation() {
        return operation;
    }

    @Override
    public String toString() {
        return "ConfigurationPatch [operation=" + operation + ", path=" + path + ", eTag=" + etagNotifIdBase + "]";
    }

}
