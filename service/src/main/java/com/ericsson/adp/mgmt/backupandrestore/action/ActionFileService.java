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
package com.ericsson.adp.mgmt.backupandrestore.action;

import java.io.IOException;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.ericsson.adp.mgmt.backupandrestore.persist.version.Version;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import com.ericsson.adp.mgmt.backupandrestore.exception.FilePersistenceException;
import com.ericsson.adp.mgmt.backupandrestore.util.DateTimeUtils;
import com.ericsson.adp.mgmt.backupandrestore.util.OrchestratorDataFileService;

/**
 * Responsible for writing/reading actions to/from json files.
 */
@Service
public class ActionFileService extends OrchestratorDataFileService<PersistedAction> {
    public static final String ACTIONS_FOLDER = "actions";
    private static final Logger log = LogManager.getLogger(ActionFileService.class);

    private final List<Version<PersistedAction>> versions =
            List.of(
                    getDefaultVersion(this::parseLegacyJsonStringToPersistedObject, p -> p.getFileName().toString().endsWith(JSON_EXTENSION)),
                    new Version<>(
                        // We'll store actions that have timezone information under "v2/" within the actions folder
                        p -> p.getParent().resolve("v2").resolve(p.getFileName()),
                        // Since v2 persisted actions have timezone information now we can just directly parse the object
                        s -> jsonService.parseJsonString(s, PersistedAction.class),
                        // Since we're adding "v2" to the path we need to look 1 level deeper than normal
                        d -> d + 1,
                        // We only want to treat PersistedAction files in the "v2" subdirectory as matching this version
                        p -> p.getFileName().toString().endsWith(JSON_EXTENSION) && p.toString().contains("/v2/"),
                        // Although this could be 1 since the default version starts at 0, making it 2 matches with
                        // other terminology used here
                        2
                )
            );

    /**
     * Writes action to file.
     * @param action to be written.
     * @throws FilePersistenceException On error throws exception
     */
    public void writeToFile(final Action action) {
        final Path actionFolder = getActionFolder(action.getBackupManagerId());
        final Path actionFile = actionFolder.resolve(getFile(action.getActionId()));

        // If this action is being persisted for the first time, give it a version. In all other cases, the action will
        // have a known version, since it will have been constructed from a PersistedAction
        if (action.getVersion() == null) {
            action.setVersion(getLatestVersion());
        }

        try {
            writeFile(actionFolder, actionFile, toJson(action).getBytes(), action.getVersion());
        } catch (Exception exception) {
            if (action.getName() == ActionType.DELETE_BACKUP) {
                try {
                    deleteDummyFile();
                    // Try to write again
                    writeFile(actionFolder, actionFile, toJson(action).getBytes(), action.getVersion());
                } catch (Exception e) {
                    log.warn("The space reserved for execution support cannot be recovered.");
                    throw new FilePersistenceException(exception);
                }
            } else {
                // On any other action requested to be persisted
                throw new FilePersistenceException(exception);
            }
        }
    }

    /**
     * Performs a cleanup if required
     * @param action action to be removed
     */
    public void performCleanup(final Action action) {
        final Path actionFolder = getActionFolder(action.getBackupManagerId());
        Path actionFile = actionFolder.resolve(getFile(action.getActionId()));
        actionFile = action.getVersion().fromBase(actionFile);
        try {
            delete(actionFile);
        } catch (IOException e) {
            //NOTE: We don't really care if this fails
            log.warn("Failed to delete action file for action {}, message: {}", action.getActionId(), e.getMessage());
        }
    }

    /**
     * Gets persisted actions from a backupManager.
     * @param backupManagerId owner of actions.
     * @return list of actions.
     */
    public List<PersistedAction> getActions(final String backupManagerId) {
        final Path actionsFolder = getActionFolder(backupManagerId);
        if (exists(actionsFolder)) {
            return readObjectsFromFiles(actionsFolder);
        }
        return new ArrayList<>();
    }

    private Path getActionFolder(final String backupManagerId) {
        return backupManagersLocation.resolve(backupManagerId).resolve(ACTIONS_FOLDER);
    }

    private String toJson(final Action action) {
        return jsonService.toJsonString(new PersistedAction(action));
    }

    /**
     * Legacy action parser
     * Parse legacy json action
     * @param jsonString json to be parsed
     * @return Persistent action
     */
    protected Optional<PersistedAction> parseLegacyJsonStringToPersistedObject(final String jsonString) {
        final Optional<PersistedAction> action = jsonService.parseJsonString(jsonString, PersistedAction.class);
        if (action.isPresent() && (getTimeZone(jsonString) != null)) {
            // LEGACY TIMEZONE FOUND FOR ACTION
            final ZoneOffset offset = ZoneOffset.of(getTimeZone(jsonString));
            action.get().setStartTime(DateTimeUtils.offsetDateTimeFrom(action.get().getStartTime(), offset));
            action.get().setLastUpdateTime(DateTimeUtils.offsetDateTimeFrom(action.get().getLastUpdateTime(), offset));
            if (action.get().getCompletionTime() != null) {
                action.get().setCompletionTime(DateTimeUtils.offsetDateTimeFrom(action.get().getCompletionTime(), offset));
            }
        }
        return action;
    }

    @Override
    protected List<Version<PersistedAction>> getVersions() {
        return versions;
    }
}
