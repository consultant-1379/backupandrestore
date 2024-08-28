/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************/
package com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext;

import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.DELETE_BACKUP;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import java.util.Optional;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;

/**
 * A service responsible for determining the on-going
 * and available tasks of the backup managers
 */
@Service
public class TaskHypertextControlFactory {
    final TaskHrefFieldBuilder taskHrefBuilder = new TaskHrefFieldBuilder();

    private TaskHypertextControl getTaskHypertextControl(final HypertextControl hypertextControl, final ActionType type) {
        switch (type) {
            case CREATE_BACKUP:
                return new CreateBackupTaskHypertextControl(hypertextControl);
            case DELETE_BACKUP:
                return new DeleteBackupTaskHypertextControl(hypertextControl);
            case EXPORT:
                return new ExportBackupTaskHypertextControl(hypertextControl);
            case IMPORT:
                return new ImportBackupTaskHypertextControl(hypertextControl);
            case RESTORE:
                return new RestoreBackupTaskHypertextControl(hypertextControl);
            case HOUSEKEEPING:
                return new HousekeepingTaskHypertextControl(hypertextControl);
            default:
                return new TaskHypertextControl(hypertextControl);
        }
    }

    /**
     * Get a Hypertext Control object for an available task
     * @param baseURI the backup manager base URI
     * @param actionType the type of action
     * @param backupName the backup name
     * @return the hypertext control object
     */
    public TaskHypertextControl getAvailableTaskHyperTextControl(final String baseURI,
                                                                 final ActionType actionType,
                                                                 final Optional<String> backupName) {
        final String href = taskHrefBuilder.buildAvailableTaskHrefField(baseURI, actionType, backupName).toString();
        final HttpMethod method = actionType.equals(DELETE_BACKUP) ? DELETE : POST;
        final HypertextControl hypertextControl = new HypertextControl(href, method);
        return getTaskHypertextControl(hypertextControl, actionType);
    }

    /**
     * Get a Hypertext Control object for an ongoing task
     * @param baseURI the backup manager base URI
     * @param action the ongoing action
     * @return the hypertext control object
     */
    public TaskHypertextControl getOngoingTaskHyperTextControl(final String baseURI, final Action action) {
        final HypertextControl hypertextControl = new HypertextControl(taskHrefBuilder.buildOngoingTaskHrefField(baseURI, action).toString(), GET);
        return getTaskHypertextControl(hypertextControl, action.getName());
    }
}
