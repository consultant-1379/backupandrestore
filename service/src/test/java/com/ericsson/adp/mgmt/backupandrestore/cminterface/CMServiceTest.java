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
package com.ericsson.adp.mgmt.backupandrestore.cminterface;

import org.junit.Test;

public class CMServiceTest {

    @Test
    public void test_thread_interruption_when_CMService_Sleeps() throws InterruptedException {
        ConcreteCMService conClass = new ConcreteCMService();
        // Create a new thread on which to run the candidate method.
        Thread thread = new Thread() {
            @Override
            public void run() {
                conClass.sleep();
            }
        };
        thread.start();
        thread.interrupt();
        thread.join();
    }
    class ConcreteCMService extends CMService {
    }

}

