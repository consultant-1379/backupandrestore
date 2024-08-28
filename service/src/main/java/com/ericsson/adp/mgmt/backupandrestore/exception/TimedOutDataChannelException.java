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
package com.ericsson.adp.mgmt.backupandrestore.exception;

/**
 * To be used when timing out waiting for a data channel to be ready.
 */
public class TimedOutDataChannelException extends RuntimeException {

    private static final long serialVersionUID = 7142872562241774742L;

    /**
     * Creates exception.
     * @param timeWaited in seconds
     */
    public TimedOutDataChannelException(final int timeWaited) {
        super("Waited " + timeWaited + " seconds for data channel to be ready");
    }

}
