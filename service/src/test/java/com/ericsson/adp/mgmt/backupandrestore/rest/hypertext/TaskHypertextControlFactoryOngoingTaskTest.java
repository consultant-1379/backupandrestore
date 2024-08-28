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
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.HOUSEKEEPING;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.IMPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.RESTORE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext.TaskHypertextControl;
import com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext.TaskHypertextControlFactory;

@RunWith(Parameterized.class)
public class TaskHypertextControlFactoryOngoingTaskTest {

    private final static String BACKUPMANAGER = "configuration-data";
    private final static String BACKUP = "myBackup";
    private final static String ACTIONID = "123456";
    private final static Path BACKUPMANAGERURI = Path.of("baseURI", "backup-managers", BACKUPMANAGER);
    private final static Path BACKUPSURI = BACKUPMANAGERURI.resolve("backups");
    private final static String IMPORTSURI = BACKUPMANAGERURI.resolve("imports").toString();
    private final static String BACKUPURI = BACKUPSURI.resolve(BACKUP).toString();
    private final static String EXPORTSURI = Path.of(BACKUPURI, "exports").toString();
    private final static String RESTORESURI = Path.of(BACKUPURI, "restores").toString();
    private final static String EXPORTURI = Path.of(EXPORTSURI, ACTIONID).toString();
    private final static String IMPORTURI = Path.of(IMPORTSURI, ACTIONID).toString();
    private final static String RESTOREURI = Path.of(RESTORESURI, ACTIONID).toString();

    private TaskHypertextControlFactory factory;
    private final ActionType actionType;
    private final String backupName;
    private final String expectedHrefField;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { CREATE_BACKUP, BACKUP, BACKUPURI},
            { EXPORT, BACKUP, EXPORTURI},
            { DELETE_BACKUP, BACKUP, BACKUPURI},
            { IMPORT, BACKUP, IMPORTURI},
            { RESTORE, BACKUP, RESTOREURI},
            { HOUSEKEEPING, BACKUP, BACKUPMANAGERURI.toString()},
        });
    }

    public TaskHypertextControlFactoryOngoingTaskTest(final ActionType actionType, final String backupName, final String expectedHrefField) {
        this.actionType = actionType;
        this.backupName = backupName;
        this.expectedHrefField = expectedHrefField;
    }

    @Before
    public void setUp() {
        factory = new TaskHypertextControlFactory();
    }

    @Test
    public void getOngoingTaskHyperText() {
        final Action action = mock(Action.class);
        when(action.getName()).thenReturn(actionType);
        when(action.isPartOfHousekeeping()).thenReturn(actionType.equals(HOUSEKEEPING));
        when(action.getBackupName()).thenReturn(backupName);
        when(action.getActionId()).thenReturn(ACTIONID);
        TaskHypertextControl actual = factory.getOngoingTaskHyperTextControl(BACKUPMANAGERURI.toString(), action);
        assertEquals(expectedHrefField, actual.getOperation().getHref());
        assertEquals(GET, actual.getOperation().getMethod());
    }
}
