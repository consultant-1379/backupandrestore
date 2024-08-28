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


public class BRMProgressReportJsonTest {
    @Test
    public void toString_valid() {
        BRMProgressReportJson brmProgressReportJson = new BRMProgressReportJson();
        assertEquals("BRMProgressReportJson{cmRepresentationOfResult='null', cmRepresentationOfState='null', progressPercentage=null, startTime='null', completionTime='null', lastUpdateTime='null', actionId='null', name=null, result=null, additionalInfo='null', progressInfo='null', resultInfo='null', state=null}",
                brmProgressReportJson.toString());
    }
}
