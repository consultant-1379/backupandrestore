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
package com.ericsson.adp.mgmt.backupandrestore.notification;

import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.KAFKA;
import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.REDIS;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.kafka.core.KafkaTemplate;

import com.ericsson.adp.mgmt.backupandrestore.ssl.KafkaKeyStoreConfiguration;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;
import com.ericsson.adp.mgmt.backupandrestore.ssl.RedisKeyStoreConfiguration;

public class NotificationConfigurationTest {
    private static final String CERTIFICATES_PATH = "src/test/resources/";
    private static final String keyStorePath = CERTIFICATES_PATH + "ca.p12";
    private static final String kafka_password = "broTestPassword";
    private static final String privateKeyPath = CERTIFICATES_PATH + "clientprivkey.key";
    private static final String certPath = CERTIFICATES_PATH + "clientcert.pem";
    private static final String rootCaPath = CERTIFICATES_PATH + "ca.pem";

    public static final String SAMPLE_TOPIC = "sample-topic";
    public static final String REDIS_HOSTNAME = "eric-data-key-value-database-rd";
    public static final String REDIS_PORT = "6379";
    public static final String REDIS_TLS_PORT = "6380";
    public static final boolean ACL_ENABLED = true;
    public static final String REDIS_USERNAME = "userbro";
    public static final String REDIS_PASSWORD = "somepassword";
    private NotificationConfiguration notificationConfiguration;
    private MockedStatic<Redisson> mockedRedisson;

    @Before
    public void setup() {
        mockedRedisson = mockStatic(Redisson.class);
        this.notificationConfiguration = new NotificationConfiguration();
    }

    @After
    public void tearDown() {
        // Close the static mock after each test
        mockedRedisson.close();
    }

    @Test
    public void getNotificationService_notificationsDisabled_noNotificationService() {
        assertTrue(notificationConfiguration.getNotificationService() instanceof NoNotifiersService);
    }

    @Test
    public void getNotificationService_notificationsEnabled_multipleNotifierService() {
        this.notificationConfiguration.setIsNotificationEnabled(true);

        assertTrue(notificationConfiguration.getNotificationService() instanceof NoNotifiersService);
    }

    @Test
    public void getNotificationService_onlyKafkaEnabled_multipleNotifierService() {
        this.notificationConfiguration.setIsNotificationEnabled(true);
        this.notificationConfiguration.setKafkaNotificationEnabled(true);
        KafkaTemplate<String,Notification> mockTemplate = Mockito.mock(KafkaTemplate.class);
        this.notificationConfiguration.setKafkaTemplate(mockTemplate);
        this.notificationConfiguration.setTopic(SAMPLE_TOPIC);

        KafkaConfiguration kafkaConfiguration = new KafkaConfiguration();
        kafkaConfiguration.setPartitions(1);
        kafkaConfiguration.setReplicationFactor((short) 3);
        kafkaConfiguration.setHostname("localhost");
        kafkaConfiguration.setPort("9092");
        this.notificationConfiguration.setKafkaConfiguration(kafkaConfiguration);

        KeyStoreService keyStoreService = createMock(KeyStoreService.class);
        notificationConfiguration.setKeyStoreService(keyStoreService);
        KafkaKeyStoreConfiguration kafkaKeyStoreConfiguration =
                new KafkaKeyStoreConfiguration(keyStorePath, kafka_password,
                        privateKeyPath, certPath, rootCaPath);
        expect(keyStoreService.getKeyStoreConfig(KAFKA)).andReturn(kafkaKeyStoreConfiguration);

        Notifier notifier = notificationConfiguration.getKafkaNotifier();
        assertTrue(notifier instanceof KafkaNotifier);

        NotificationService notificationService = notificationConfiguration.getNotificationService();
        assertTrue(notificationService instanceof MultipleNotifierService);

    }

    @Test
    public void getNotificationService_onlyKafkaEnabledSSl() {
        this.notificationConfiguration.setIsNotificationEnabled(true);
        this.notificationConfiguration.setKafkaNotificationEnabled(true);
        notificationConfiguration.setSSLEnabled(true);
        KafkaTemplate<String,Notification> mockTemplate = Mockito.mock(KafkaTemplate.class);
        this.notificationConfiguration.setKafkaTemplate(mockTemplate);
        this.notificationConfiguration.setTopic(SAMPLE_TOPIC);
        notificationConfiguration.setKafkaIdentificationAlgorithm("https");

        KafkaConfiguration kafkaConfiguration = new KafkaConfiguration();
        kafkaConfiguration.setPartitions(1);
        kafkaConfiguration.setReplicationFactor((short) 3);
        kafkaConfiguration.setHostname("localhost");
        kafkaConfiguration.setTlsPort("9093");
        this.notificationConfiguration.setKafkaConfiguration(kafkaConfiguration);

        KeyStoreService keyStoreService = createMock(KeyStoreService.class);
        notificationConfiguration.setKeyStoreService(keyStoreService);

        KafkaKeyStoreConfiguration kafkaKeyStoreConfiguration =
                new KafkaKeyStoreConfiguration(keyStorePath, kafka_password,
                        privateKeyPath, certPath, rootCaPath);
        expect(keyStoreService.getKeyStoreConfig(KAFKA)).andReturn(kafkaKeyStoreConfiguration);
        expect(keyStoreService.getKeyStoreType()).andReturn("PKCS12");
        replay(keyStoreService);

        Notifier notifier = notificationConfiguration.getKafkaNotifier();
        assertTrue(notifier instanceof KafkaNotifier);
    }

    @Test
    public void getNotificationService_onlyRedisEnabled_multipleNotifierService() {
        notificationConfiguration.setIsNotificationEnabled(true);
        notificationConfiguration.setRedisNotificationEnabled(true);


        // mock keystore service
        RedisKeyStoreConfiguration redisKeyStoreConfiguration = mock(RedisKeyStoreConfiguration.class);
        expect(redisKeyStoreConfiguration.getKeyStoreFile()).andReturn(Path.of("file")).anyTimes();
        expect(redisKeyStoreConfiguration.getKeyStorePassword()).andReturn("pw").anyTimes();
        replay(redisKeyStoreConfiguration);

        KeyStoreService keyStoreService = mock(KeyStoreService.class);
        expect(keyStoreService.getKeyStoreConfig(REDIS)).andReturn(
                redisKeyStoreConfiguration
        );
        keyStoreService.regenerateKeyStoreForAlias(REDIS);
        expectLastCall();
        replay(keyStoreService);

        // mock redisson client
        RedissonClient redissonClient = mock(RedissonClient.class);
        expect(redissonClient.getStream("bro")).andReturn(null);

        when(Redisson.create(anyObject())).thenReturn(redissonClient);

        Notifier notifier = notificationConfiguration.getRedisNotifier(REDIS_HOSTNAME, REDIS_PORT, REDIS_TLS_PORT,
                                                                       ACL_ENABLED, REDIS_USERNAME, REDIS_PASSWORD,
                                                                       "ASASTRING");
        assertTrue(notifier instanceof RedisNotifier);

        NotificationService notificationService = notificationConfiguration.getNotificationService();
        assertTrue(notificationService instanceof MultipleNotifierService);

    }


}
