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

import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.DELETE_BACKUP;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.EXPORT;
import static com.ericsson.adp.mgmt.backupandrestore.action.ActionType.RESTORE;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.rest.hyptertext.TaskHrefFieldBuilder;

@RunWith(Parameterized.class)
public class TaskHrefFieldBuilderTest {
    private static final Optional<String> EMPTY_BACKUP = Optional.empty();
    private final ActionType actionType;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            { EXPORT},
            { DELETE_BACKUP},
            { RESTORE},
        });
    }

    public TaskHrefFieldBuilderTest(final ActionType actionType) {
        this.actionType = actionType;
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildHrefFieldWhenBackupNameIsRequired_noBackupNameIsProvided() {
        TaskHrefFieldBuilder hrefBuilder = new TaskHrefFieldBuilder();
        hrefBuilder.buildAvailableTaskHrefField("baseURI", actionType, EMPTY_BACKUP);
    }

}