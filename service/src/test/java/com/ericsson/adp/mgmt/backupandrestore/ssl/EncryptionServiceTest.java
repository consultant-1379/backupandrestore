/**------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2021
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *------------------------------------------------------------------------------*/
package com.ericsson.adp.mgmt.backupandrestore.ssl;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class EncryptionServiceTest {

    @Test
    public void testEncryptDecrypt() {
        EncryptionService service = new EncryptionService();
        final String input = "TestString";
        assertNotEquals(input, service.encrypt(input)); // Check data is encrypted
        assertEquals(input, service.decrypt(service.encrypt(input))); // Check data can be decrypted
        final String ciphertext = service.encrypt(input);
        assertEquals( // Check there's nothing strange re: call ordering going on
                service.decrypt(ciphertext),
                service.decrypt(service.encrypt(input)));
    }
}
