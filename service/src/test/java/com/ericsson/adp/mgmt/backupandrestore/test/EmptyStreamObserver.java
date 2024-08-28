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
package com.ericsson.adp.mgmt.backupandrestore.test;

import com.google.protobuf.Empty;

import io.grpc.stub.StreamObserver;

public class EmptyStreamObserver implements StreamObserver<Empty> {

    @Override
    public void onCompleted() {
        //Not needed
    }

    @Override
    public void onError(final Throwable throwable) {
        //Not needed
    }

    @Override
    public void onNext(final Empty response) {
        //Not needed
    }

}
