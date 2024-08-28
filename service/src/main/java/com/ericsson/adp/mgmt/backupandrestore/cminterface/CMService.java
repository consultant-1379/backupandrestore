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
package com.ericsson.adp.mgmt.backupandrestore.cminterface;

import static com.ericsson.adp.mgmt.backupandrestore.util.ApplicationConstantsUtils.DELAY_IN_SECONDS_BETWEEN_UPLOAD_ATTEMPTS;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.ericsson.adp.mgmt.backupandrestore.exception.CMMediatorException;

/**
 * A service for interacting with the CMM REST API.
 */
public abstract class CMService {
    public static final String REST_TEMPLATE_ID = "CMM";
    private static final Logger log = LogManager.getLogger(CMService.class);
    private static final String PSW_REGEX = "'(.*)'\\s";
    private CMMClient cmmClient;
    private CMMMessageFactory cmmMessageFactory;

    public CMMClient getCMMClient() {
        return this.cmmClient;
    }

    public CMMMessageFactory getCMMessageFactory() {
        return cmmMessageFactory;
    }

    /**
     Makes sure the password is hidden
     @param errorMessage the error message with the password present
     @return the error message to be logged after password was hidden
     */
    protected static String hidePassword(final String errorMessage) {
        if (errorMessage.contains("is not a 'eric-adp-cm-secret'")) {
            log.debug("Request failed with response <{}>", errorMessage);
            return (errorMessage.replaceFirst(PSW_REGEX, "'supplied information' "));
        } else {
            return errorMessage;
        }
    }

    /**
     * Specify if it was initialized, used mainly in test
     * @param initialize true it was initiated
     */
    protected void setInitialize(final boolean initialize) {
        getCMMClient().setInitialized(initialize);
    }

    /**
     * Performs a thread sleep
     */
    protected void sleep() {
        sleep(DELAY_IN_SECONDS_BETWEEN_UPLOAD_ATTEMPTS * 1_000L, "Upload interrupted");
    }

    /**
     * Performs a thread sleep in a specified time delay
     * @param delayMs the sleep duration in milliseconds
     * @param threadInterruptErrorMessage the error message when the thread is interrupted while sleeping
     */
    protected void sleep(final long delayMs, final String threadInterruptErrorMessage) {
        try {
            log.debug("Waiting {} milli seconds.", delayMs);
            TimeUnit.MILLISECONDS.sleep(delayMs);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CMMediatorException(MessageFormat.format("{0}: {1}", threadInterruptErrorMessage, e.getMessage()));
        }
    }

    public void setCmmClient(final CMMClient cmmClient) {
        this.cmmClient = cmmClient;
    }

    public void setCMMMessageFactory(final CMMMessageFactory cmmMessageFactory) {
        this.cmmMessageFactory = cmmMessageFactory;
    }

}
