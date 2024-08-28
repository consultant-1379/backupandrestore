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
package com.ericsson.adp.mgmt.backupandrestore.grpc.backup;

import org.junit.Test;

public class UnexpectedBackupDataStreamTest {

    @Test(expected = Test.None.class /* no exception expected */)
    public void onCompleted_notExpectedBackupDataStream_doesNothing(){
        new UnexpectedBackupDataStream().onCompleted();
    }

    @Test(expected = Test.None.class)
    public void onError_notExpectedBackupDataStream_doesNothing(){
        new UnexpectedBackupDataStream().onError(new RuntimeException("Boo"));
    }

    @Test(expected = Test.None.class)
    public void onNext_notExpectedBackupDataStream_doesNothing(){
        new UnexpectedBackupDataStream().onNext(null);
    }

}
