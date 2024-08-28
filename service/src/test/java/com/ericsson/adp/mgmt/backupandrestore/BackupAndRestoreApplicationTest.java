/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2020
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore;


import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;


public class BackupAndRestoreApplicationTest {

    @Test(expected = BeanCreationException.class)
    public void test_backupAndRestoreApplication_main()
    {
        BackupAndRestoreApplication.main(new String[]{});
    }

    @Test
    public void test_backupAndRestoreApplication_ipv6_main()
    {
        BackupAndRestoreApplication.main(new String[]{"0:0:0:0:0:0:0:0"});
    }
}