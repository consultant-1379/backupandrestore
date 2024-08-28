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
package com.ericsson.adp.mgmt.backupandrestore.util;

import org.junit.Before;
import org.junit.Test;

import com.ericsson.adp.mgmt.backupandrestore.exception.InvalidIdException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IdValidatorTest {

    private IdValidator idValidator;

    @Before
    public void setup() {
        idValidator = new IdValidator();
    }

    @Test
    public void validateId_validId_allowsId() throws Exception {
        idValidator.validateId("validId");
        assertTrue(idValidator.isValidId("validId"));
    }

    @Test(expected = InvalidIdException.class)
    public void validateId_nullId_throwsException() throws Exception {
        assertFalse(idValidator.isValidId(null));
        idValidator.validateId(null);
    }

    @Test(expected = InvalidIdException.class)
    public void validateId_emptyId_throwsException() throws Exception {
        assertFalse(idValidator.isValidId(""));
        idValidator.validateId("");
    }

    @Test(expected = InvalidIdException.class)
    public void validateId_idWithSlash_throwsException() throws Exception {
        assertFalse(idValidator.isValidId("aa/aa"));
        idValidator.validateId("aa/aa");
    }

    @Test(expected = InvalidIdException.class)
    public void validateId_idWithBackslash_throwsException() throws Exception {
        assertFalse(idValidator.isValidId("aa\\aa"));
        idValidator.validateId("aa\\aa");
    }

    @Test(expected = InvalidIdException.class)
    public void validateId_idPointingToParentFolder_throwsException() throws Exception {
        assertFalse(idValidator.isValidId(".."));
        idValidator.validateId("..");
    }

    @Test(expected = InvalidIdException.class)
    public void validateId_idPointingToCurrentFolder_throwsException() throws Exception {
        assertFalse(idValidator.isValidId("."));
        idValidator.validateId(".");
    }

    @Test(expected = InvalidIdException.class)
    public void validateBackupName_pathTraversal_throwsException() {
        assertFalse(idValidator.isValidId("../.././.."));
        idValidator.validateId("../.././..");
    }

    @Test(expected = InvalidIdException.class)
    public void validateBackupName_singleSlash_throwsException() {
        assertFalse(idValidator.isValidId("/"));
        idValidator.validateId("/");
    }

    @Test(expected = InvalidIdException.class)
    public void validateBackupName_singleBackslash_throwsException() {
        assertFalse(idValidator.isValidId("\\"));
        idValidator.validateId("\\");
    }

    @Test(expected = InvalidIdException.class)
    public void validateBackupName_homeDirectory_throwsException() {
        assertFalse(idValidator.isValidId("~"));
        idValidator.validateId("~");
    }
}
