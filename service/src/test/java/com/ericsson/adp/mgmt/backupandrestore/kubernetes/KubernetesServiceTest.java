package com.ericsson.adp.mgmt.backupandrestore.kubernetes;

import static org.mockito.ArgumentMatchers.anyString;

import org.apache.logging.log4j.LogManager;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.util.Config;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Config.class, LoggerFactory.class, LogManager.class, Configuration.class, ApiClient.class})
public class KubernetesServiceTest {

    @Before
    public void setupBeforeClass() throws Exception {
        PowerMock.mockStatic(LoggerFactory.class);
        Logger logger = PowerMock.createMock(Logger.class);
        EasyMock.expect(LoggerFactory.getLogger(anyString ())).andReturn(logger);

        PowerMock.mockStatic(LogManager.class);
        org.apache.logging.log4j.Logger loggerKubernete = PowerMock.createMock(org.apache.logging.log4j.Logger.class);
        EasyMock.expect(LogManager.getLogger(anyString ())).andReturn(loggerKubernete);

        PowerMock.mockStatic(ApiClient.class);
        ApiClient apiClient = PowerMock.createMock(ApiClient.class);
        PowerMock.expectNew(ApiClient.class).andReturn(apiClient);

        PowerMock.mockStatic(Config.class);
        EasyMock.expect(Config.defaultClient()).andReturn(apiClient);

        PowerMock.replay(Config.class, ApiClient.class);
    }

    @Test
    public void kubernetesService_newInstance_setKubernetesClient() {
        KubernetesService kubernetesService = new KubernetesService();
        PowerMock.verify(Config.class);
    }

}
