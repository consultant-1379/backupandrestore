/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/

package com.ericsson.adp.mgmt.backupandrestore.util;

import com.ericsson.adp.mgmt.backupandrestore.agent.exception.InvalidAPIVersionStringException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class ApiVersionTest {
    @Test
    public void fromString_stringRepresentation_validStringRepresentations() {
        for(final ApiVersion apiVersion : ApiVersion.values()) {
            assertEquals(apiVersion,ApiVersion.fromString(apiVersion.stringRepresentation));
        }
    }

    @Test(expected = InvalidAPIVersionStringException.class)
    public void fromString_invalidStringRepresentation_ExceptionThrown() {
        ApiVersion.fromString(RandomStringUtils.randomAlphanumeric(10));
    }

}
