/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2023
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.job;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;

/**
 * Utility class used to define the import/restore logic for parallel actions
 *
 */
public class ImportRestorePredicatesUtils {

    protected static final Predicate<Action> isNotNullAction = currentAction -> currentAction != null;
    protected static final Predicate<Action> isImport = currentAction -> isNotNullAction.test(currentAction)
            && currentAction.isImport();
    protected static final Predicate<Action> isRestore = currentAction -> isNotNullAction.test(currentAction)
                    && currentAction.isRestore();
    protected static final BiPredicate<Action, Action> isImportRestore = (currentAction, newAction) ->
        (isImport.test(currentAction) && isRestore.test(newAction)) ||
        (isImport.test(newAction) && isRestore.test(currentAction));
    protected static final BiPredicate<Action, Action> ifDifferentBackupName = (currentAction, newAction) ->
            !newAction.hasSameBackupName(currentAction);
    protected static final BiPredicate<Action, Action> ifSameBRMdifferentBackupName = (currentAction, newAction) ->
        newAction.hasSameBRMId(currentAction) && ifDifferentBackupName.test(currentAction, newAction);

    protected static final BiPredicate<Action, Action> checkKinBRM = (currentAction, newAction) ->
            ifDifferentBackupName.test(currentAction, newAction) || !newAction.hasKinBRM(currentAction);

    protected static final BiPredicate<Action, Action> ifValidActionOnConfigVBRM = (currentAction, newAction) ->
            ifDifferentBackupName.test(currentAction, newAction) &&
            !(newAction.isConfigBRMOf(currentAction) && newAction.isRestore()) &&
                    !(currentAction.isConfigBRMOf(newAction) && currentAction.isRestore());

    protected static final BiPredicate<Action, Action> isConfigVBRM = (currentAction, newAction) ->
            newAction.isConfigBRMOf(currentAction) || currentAction.isConfigBRMOf(newAction);

    protected static final BiPredicate<Action, Action> ifValidDifferentBRM = (currentAction, newAction) ->
            !newAction.hasSameBRMId(currentAction) &&
                    checkKinBRM.test(currentAction, newAction) &&
                    (!isConfigVBRM.test(currentAction, newAction) || ifValidActionOnConfigVBRM.test(currentAction, newAction));

    protected static final BiPredicate<Action, Action> isValidImportRestore =
            isImportRestore.and(ifSameBRMdifferentBackupName.or(ifValidDifferentBRM));

    private ImportRestorePredicatesUtils() {
        // Empty
    }

}
