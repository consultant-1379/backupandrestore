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
package com.ericsson.adp.mgmt.backupandrestore.notification;


import static com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias.REDIS;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.powermock.api.easymock.PowerMock.mockStatic;
import static org.powermock.api.easymock.PowerMock.replayAll;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;
import com.ericsson.adp.mgmt.backupandrestore.ssl.RedisKeyStoreConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.redisson.Redisson;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;

import java.nio.file.Path;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Redisson.class)
public class RedisNotifierTest {

    private static final String ACTION_ID = "123";
    private static final String BACKUP_MANAGER_ID = "DEFAULT";
    private static final String TOPIC = "bro-notification";
    private static final String REDIS_NOTIFICATION_FORMAT = RedisNotifier.JSONSTRINGFORMAT;
    public static final String REDIS_USERNAME = "userbro";
    public static final String REDIS_PASSWORD = "somepassword";
    public static final boolean ACL_Enabled = true;

    private RedisNotifier notifier;
    private Action action;
    private RedisClient client;
    private RedissonClient redissonClient;
    private RStream mockedStream;
    private RedisKeyStoreConfiguration redisKeyStoreConfiguration;
    private KeyStoreService keyStoreService;
    final boolean isSSLEnabled = true;
    final String hostName = "localhost";
    final String port = "6379";
    final String tlsPort = "6380";

    @Before
    public void setup() {
        this.action = mockAction();
        redissonClient = mock(RedissonClient.class);
        mockedStream = mock(RStream.class);
        redisKeyStoreConfiguration = mock(RedisKeyStoreConfiguration.class);
        keyStoreService = mock(KeyStoreService.class);

        expect(redisKeyStoreConfiguration.getKeyStoreFile()).andReturn(Path.of("file")).anyTimes();
        expect(redisKeyStoreConfiguration.getKeyStorePassword()).andReturn("pw").anyTimes();
        replay(redisKeyStoreConfiguration);

        // mock redisson client
        expect(redissonClient.getStream(anyString())).andReturn(mockedStream);
        expect(mockedStream.add(anyObject())).andReturn(mock(StreamMessageId.class));

        replay(redissonClient, mockedStream);
        mockStatic(Redisson.class);
        expect(Redisson.create(anyObject())).andReturn(redissonClient).anyTimes();

        expect(keyStoreService.getKeyStoreConfig(REDIS)).andReturn(redisKeyStoreConfiguration).anyTimes();
        keyStoreService.regenerateKeyStoreForAlias(REDIS);
        expectLastCall().anyTimes();
        replay(keyStoreService);

        replayAll();
        client = new RedisClient(hostName, port, tlsPort,isSSLEnabled, keyStoreService, ACL_Enabled, REDIS_USERNAME, REDIS_PASSWORD);
        notifier = new RedisNotifier(client, TOPIC, REDIS_NOTIFICATION_FORMAT);
    }

    @Test
    public void getRedissonClient_noSSL_valid() {
        final boolean isSSLEnabled = false;
        final String hostName = "localhost";
        final String port = "6379";
        final String tlsPort = "6380";

        RedissonClient redissonClient = mock(RedissonClient.class);

        RStream mockedStream = mock(RStream.class);

        expect(redissonClient.getStream(anyString())).andReturn(mockedStream);
        expect(mockedStream.add(anyObject())).andReturn(mock(StreamMessageId.class));

        // mock redisson client
        mockStatic(Redisson.class);
        expect(Redisson.create(anyObject())).andReturn(redissonClient).anyTimes();

        replayAll();
        client = new RedisClient(hostName, port, tlsPort,isSSLEnabled, keyStoreService, ACL_Enabled, REDIS_USERNAME, REDIS_PASSWORD);
        notifier = new RedisNotifier(client, TOPIC, REDIS_NOTIFICATION_FORMAT);
    }


    @Test(expected = Test.None.class)
    public void notifyActionStarted_sendsNotification()  {
        this.notifier.notifyActionStarted(action);

    }

    @Test(expected = Test.None.class)
    public void notifyActionCompleted_sendsNotification() {
        this.notifier.notifyActionCompleted(action);
    }

    @Test(expected = Test.None.class)
    public void notifyActionFailed_sendsNotification() {
        this.notifier.notifyActionFailed(action);
    }

    @Test(expected = NotificationFailedException.class)
    public void sendNotification_exception() {
        redissonClient = mock(RedissonClient.class);
        mockedStream = mock(RStream.class);
        expect(redissonClient.getStream(anyString())).andReturn(mockedStream);
        expect(mockedStream.add(anyObject())).andThrow(new RuntimeException());

        replay(redissonClient, mockedStream);
        mockStatic(Redisson.class);
        expect(Redisson.create(anyObject())).andReturn(redissonClient).anyTimes();

        replayAll();
        client.setRedissonClient();
        notifier.sendNotification(new Notification());
    }

    @Test(expected = NotificationFailedException.class)
    public void getRedissonTLSClient_exception() {
        redisKeyStoreConfiguration = mock(RedisKeyStoreConfiguration.class);
        keyStoreService = mock(KeyStoreService.class);

        expect(redisKeyStoreConfiguration.getKeyStoreFile()).andReturn(null).anyTimes();
        expect(redisKeyStoreConfiguration.getKeyStorePassword()).andReturn("pw").anyTimes();
        replay(redisKeyStoreConfiguration);

        client = new RedisClient(hostName, port, tlsPort,isSSLEnabled, keyStoreService, ACL_Enabled, REDIS_USERNAME, REDIS_PASSWORD);
        notifier = new RedisNotifier(client, TOPIC, REDIS_NOTIFICATION_FORMAT);
        client.setRedissonClient();

        expect(keyStoreService.getKeyStoreConfig(REDIS)).andReturn(redisKeyStoreConfiguration);
        keyStoreService.regenerateKeyStoreForAlias(REDIS);
        expectLastCall();

        replay(keyStoreService);
        client.getRedissonClient();
    }

    private Action mockAction() {
        final Action action = Mockito.mock(Action.class);
        Mockito.when(action.getActionId()).thenReturn(ACTION_ID);
        Mockito.when(action.getBackupManagerId()).thenReturn(BACKUP_MANAGER_ID);
        return action;
    }

}