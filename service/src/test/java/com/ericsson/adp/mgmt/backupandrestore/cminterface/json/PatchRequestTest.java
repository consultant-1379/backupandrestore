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
package com.ericsson.adp.mgmt.backupandrestore.cminterface.json;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.List;

public class PatchRequestTest {
    @Test
    public void toString_valid() {
        List<PatchOperationJson> operations = List.of(new PatchOperationJson());
        PatchRequest patchRequest = new PatchRequest(operations);
        assertEquals("PatchRequest{operations=[PatchOperationJson [operation=null, path=null, value=null]], baseETag=''}", patchRequest.toString());
    }

}
