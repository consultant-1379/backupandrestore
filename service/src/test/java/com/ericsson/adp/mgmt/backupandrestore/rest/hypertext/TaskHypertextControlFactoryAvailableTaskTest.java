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
package com.ericsson.adp.mgmt.backupandrestore.rest.hypertext;

import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.CREATE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.DELETE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.EXPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.IMPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.RESTORE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext.TaskHypertextControl;
import com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext.TaskHypertextControlFactory;

@RunWith(Parameterized.class)
public class TaskHypertextControlFactoryAvailableTaskTest {

    private final static String BACKUPMANAGER = "configuration-data";
    private final static String BACKUP = "myBackup";
    private final static Path BACKUPMANAGERURI = Paths.get("baseURI", "backup-managers", BACKUPMANAGER);
    private final static Path BACKUPSURI = BACKUPMANAGERURI.resolve("backups");
    private final static Path IMPORTSURI = BACKUPMANAGERURI.resolve("imports");
    private final static Path BACKUPURI = BACKUPSURI.resolve(BACKUP);
    private final static Path EXPORTS = BACKUPURI.resolve("exports");
    private final static Path RESTORES = BACKUPURI.resolve("restores");

    private TaskHypertextControlFactory factory;
    private final ActionType actionType;
    private final String backupName;
    private final String expectedHrefField;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { CREATE_BACKUP, null, BACKUPSURI.toString()},
            { EXPORT, BACKUP, EXPORTS.toString()},
            { DELETE_BACKUP, BACKUP, BACKUPURI.toString()},
            { IMPORT, null, IMPORTSURI.toString()},
            { RESTORE, BACKUP, RESTORES.toString()},
        });
    }

    public TaskHypertextControlFactoryAvailableTaskTest(final ActionType actionType, final String backupName, final String expectedHrefField) {
        super();
        this.actionType = actionType;
        this.backupName = backupName;
        this.expectedHrefField = expectedHrefField;
    }

    @Before
    public void setUp() {
        factory = new TaskHypertextControlFactory();
    }

    @Test
    public void getAvailableTaskHyperText() {
        TaskHypertextControl actual = factory.getAvailableTaskHyperTextControl(BACKUPMANAGERURI.toString(), actionType, Optional.ofNullable(backupName));
        assertEquals(expectedHrefField, actual.getOperation().getHref());
        assertEquals(actionType.equals(DELETE_BACKUP) ? DELETE : POST , actual.getOperation().getMethod());
    }
}
