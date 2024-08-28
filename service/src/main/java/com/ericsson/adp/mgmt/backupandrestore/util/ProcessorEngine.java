/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2022
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.util;

/**
 * Interface to define processor used by REST
 * @param <T> item to be processed
 *
 */
public interface ProcessorEngine<T> {

    /**
     * Push the request to a element processor
     * @param message the message to be processed
     * @return ServiceState service state after the processed message
     */
    T transferMessage(final T message);
}