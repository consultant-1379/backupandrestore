package com.ericsson.adp.mgmt.backupandrestore.kms;

import com.ericsson.adp.mgmt.backupandrestore.exception.KMSRequestFailedException;
import org.easymock.EasyMock;
import org.junit.Test;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CMKeyExportPasswordServiceTest {

    @Test
    public void getPassword_retriesOnFailureTest() {
        final String returnedPassword = "returned_password";
        final KeyManagementService keyManagementService = EasyMock.createMock(KeyManagementService.class);
        expect(keyManagementService.decrypt(anyString(), anyObject(KeyManagementService.RequestSettings.class)))
                .andThrow(new KMSRequestFailedException("purposeful test failure")).once();
        keyManagementService.refreshToken(anyObject(KeyManagementService.RequestSettings.class));
        expectLastCall();
        expect(keyManagementService.decrypt(anyString(), anyObject(KeyManagementService.RequestSettings.class)))
                .andReturn(returnedPassword).once();

        final CMKeyPassphraseService service = new CMKeyPassphraseService();
        service.setEnabled(true);
        service.setKeyName("irrelevant_key_name");
        service.setRole("irrelevant_role");
        service.setServiceAccountRoleMount(Path.of("irrelevant_path"));
        service.setKeyManagementService(keyManagementService);

        replay(keyManagementService);
        final OffsetDateTime startTime = OffsetDateTime.now();
        assertEquals(returnedPassword, service.getPassphrase("irrelevant_encrypted_password"));
        // Assert service waits at least 3 seconds after a failed request
        assertTrue(ChronoUnit.SECONDS.between(startTime, OffsetDateTime.now()) >= 3);
        verify(keyManagementService);
    }
}
