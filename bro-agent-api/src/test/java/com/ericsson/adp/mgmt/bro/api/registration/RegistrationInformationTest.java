/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2019
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.bro.api.registration;

import static org.junit.Assert.fail;

import com.ericsson.adp.mgmt.bro.api.exception.InvalidRegistrationInformationException;
import com.ericsson.adp.mgmt.bro.api.test.RegistrationInformationUtil;
import org.junit.Test;

public class RegistrationInformationTest {

    @Test(expected = InvalidRegistrationInformationException.class)
    public void validate_NullTestRegistrationInformation_InvalidRegistrationInformationException() {
        final RegistrationInformation registrationInfo = RegistrationInformationUtil
            .getNullTestRegistrationInformation();
        registrationInfo.validate();
    }

    @Test(expected = InvalidRegistrationInformationException.class)
    public void validate_SoftwareVersionNull_InvalidRegistrationInformationException() {
        final RegistrationInformation registrationInfo = RegistrationInformationUtil
            .getNullTestRegistrationInformation();
        registrationInfo.setAgentId("SpecialAgent");
        registrationInfo.setScope("Periscope");
        registrationInfo.setApiVersion("Apiary");
        registrationInfo.setSoftwareVersion(null);
        registrationInfo.validate();
    }

    @Test(expected = InvalidRegistrationInformationException.class)
    public void validate_ApiVersionNull_InvalidRegistrationInformationException() {
        final RegistrationInformation registrationInfo = RegistrationInformationUtil
            .getNullTestRegistrationInformation();
        registrationInfo.setAgentId("SpecialAgent");
        registrationInfo.setScope("Periscope");
        registrationInfo.setApiVersion(null);
        registrationInfo.setSoftwareVersion(null);
        registrationInfo.validate();
        fail();
    }

    @Test(expected = InvalidRegistrationInformationException.class)
    public void validate_NullScope_InvalidRegistrationInformationException() {
        final RegistrationInformation registrationInfo = RegistrationInformationUtil
            .getNullTestRegistrationInformation();
        registrationInfo.setAgentId("SpecialAgent");
        registrationInfo.setScope(null);
        registrationInfo.setApiVersion(null);
        registrationInfo.setSoftwareVersion(null);
        registrationInfo.validate();
    }

    @Test(expected = InvalidRegistrationInformationException.class)
    public void validate_BlankTestRegistrationInformation_InvalidRegistrationInformationException() {
        final RegistrationInformation registrationInfo = RegistrationInformationUtil
            .getBlankTestRegistrationInformation();
        registrationInfo.validate();
    }

    @Test
    public void validate_EmptyScope_NoExceptions() {
        final RegistrationInformation registrationInfo = RegistrationInformationUtil
            .getTestRegistrationInformation();
        registrationInfo.setScope("");
        registrationInfo.validate();
    }

    @Test
    public void validate_ValidTestRegistrationInformation_NoExceptions() {
        final RegistrationInformation registrationInfo = RegistrationInformationUtil
            .getTestRegistrationInformation();
        registrationInfo.validate();
    }
}
