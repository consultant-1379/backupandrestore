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
package com.ericsson.adp.mgmt.backupandrestore.rest.action;

import java.net.URI;
import org.springframework.web.util.UriComponentsBuilder;

import com.ericsson.adp.mgmt.backupandrestore.archive.ArchiveUtils;
import com.ericsson.adp.mgmt.backupandrestore.backup.Backup;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JSON response of all V4 actions of a backupManager
 */
@JsonInclude(Include.NON_DEFAULT)
public class V4ActionResponse {

    @JsonProperty("id")
    private String actionId;
    private URI uri;
    private TaskResponse task;
    private String backup;

    /**
     * For Jackson
     */
    public V4ActionResponse() {}

    /**
     * Creates V4ActionsResponse
     * @param task {@link TaskResponse} task response
     */
    public V4ActionResponse(final TaskResponse task) {
        this.task = task;
    }

    /**
     * Creates V4ActionsResponse
     * @param actionId action actionId
     */
    public V4ActionResponse(final String actionId) {
        this.actionId = actionId;
    }

    /**
     * Creates V4ActionsResponse
     * @param task {@link TaskResponse} task response
     * @param uri URI
     * @param backupManagerId id of the backup manager the backup is taken under
     * @param backup backup being exported
     */
    public V4ActionResponse(final TaskResponse task, final URI uri, final String backupManagerId, final Backup backup) {
        this.task = task;
        this.uri = getExportURI(uri, backupManagerId, backup);
    }

    /**
     * Creates V4ActionsResponse
     * @param actionId action actionId
     * @param uri URI
     * @param backupManagerId id of the backup manager the backup is taken under
     * @param backup backup being exported
     */
    public V4ActionResponse(final String actionId, final URI uri, final String backupManagerId, final Backup backup) {
        this.actionId = actionId;
        this.uri = getExportURI(uri, backupManagerId, backup);
    }

    /**
     * Creates V4ActionsResponse
     * @param actionId action actionId
     * @param uri URI
     * @param backup - backup path to be used to query for a backup
     */
    public V4ActionResponse(final String actionId, final URI uri, final String backup) {
        this.actionId = actionId;
        this.uri = uri;
        this.backup = backup;
    }

    /**
     * Creates V4ActionsResponse
     * @param actionId action actionId
     * @param uri URI
     * @param backup - backup path to be used to query for a backup
     * @param task {@link TaskResponse} task response
     */
    public V4ActionResponse(final String actionId, final URI uri, final String backup, final TaskResponse task) {
        this.actionId = actionId;
        this.uri = uri;
        this.backup = backup;
        this.task = task;
    }

    private URI getExportURI(final URI uri, final String backupManagerId, final Backup backup) {
        return UriComponentsBuilder.fromUri(uri)
                .pathSegment(backupManagerId, ArchiveUtils.getTarballName(backup))
                .build().toUri();
    }

    public String getBackup() {
        return backup;
    }

    public TaskResponse getTask() {
        return task;
    }

    public URI getUri() {
        return uri;
    }

    public String getActionId() {
        return actionId;
    }

    public void setActionId(final String actionId) {
        this.actionId = actionId;
    }

    public void setUri(final URI uri) {
        this.uri = uri;
    }

    public void setTask(final TaskResponse task) {
        this.task = task;
    }

    public void setBackup(final String backupPath) {
        backup = backupPath;
    }

}
