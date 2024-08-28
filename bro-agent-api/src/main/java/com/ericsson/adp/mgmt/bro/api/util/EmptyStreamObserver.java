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
package com.ericsson.adp.mgmt.bro.api.util;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.protobuf.Empty;

import io.grpc.stub.StreamObserver;

/**
 * StreamObserver that handles Empty
 */
public class EmptyStreamObserver implements StreamObserver<Empty> {

    private static final Logger logger = LogManager.getLogger(EmptyStreamObserver.class);

    @Override
    public void onCompleted() {
        logger.info("Request completed");
    }

    @Override
    public void onError(final Throwable throwable) {
        logger.error("Request received error, ", throwable);
    }

    @Override
    public void onNext(final Empty response) {
        logger.info("Request received content");
    }

}
