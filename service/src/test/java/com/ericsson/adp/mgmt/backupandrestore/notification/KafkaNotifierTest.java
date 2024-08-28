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

import static org.easymock.EasyMock.createMock;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.MockAdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.Node;
import org.easymock.EasyMock;
import org.easymock.IExpectationSetters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import com.ericsson.adp.mgmt.backupandrestore.action.ActionType;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreAlias;
import com.ericsson.adp.mgmt.backupandrestore.ssl.KeyStoreService;

public class KafkaNotifierTest {

    private static final String NOTIFICATION_VERSION = "1.0.0";
    private static final String TOPIC = "TOPIC";
    private static final String ACTION_ID = "123";
    private static final String BACKUP_MANAGER_ID = "BACKUP_MANAGER_ID";
    public static ProducerFactory<String, Notification> factory;

    private Node controller = new Node(0, "localhost", 8121);
    private List<Node> brokers = Arrays.asList(
            controller,
            new Node(1, "localhost", 8122),
            new Node(2, "localhost", 8123));

    private KafkaNotifier notificationService;

    private KeyStoreService keyStoreService;

    private KafkaTemplateStub kafkaTemplate;
    private Action action;
    private AdminClient mockAdminClient;
    private MockedStatic<Admin> mockedAdmin;


    @Before
    public void setup() {
        mockedAdmin = mockStatic(Admin.class);
        mockAdminClient = new MockAdminClient(brokers, controller);
        keyStoreService = createMock(KeyStoreService.class);
        keyStoreService.regenerateKeyStoreForAlias(KeyStoreAlias.KAFKA);
        factory = EasyMock.createNiceMock(ProducerFactory.class);
        factory.reset();

        EasyMock.expectLastCall().anyTimes();
        EasyMock.replay(factory, keyStoreService);
        kafkaTemplate = new KafkaTemplateStub();
        action = mockAction();
        notificationService = new KafkaNotifier();
        notificationService.setKeyStoreService(keyStoreService);
        notificationService.setTopic(TOPIC);
        notificationService.setKafkaTemplate(kafkaTemplate);
    }

    @After
    public void tearDown() {
        mockedAdmin.close();
    }

    @Test
    public void createTopic() throws ExecutionException, InterruptedException {

        // Update props
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "localhost:9092");
        notificationService.setProps(properties);
        notificationService.setPartitions(1);
        notificationService.setReplicationFactor((short) 1);
        when(Admin.create(properties)).thenReturn(mockAdminClient);
        
        notificationService.createTopicIfNotExists(new Notification());
    }

    @Test(expected = NotificationFailedException.class)
    public void failsOnSend_topicDoesntExist_createsTopicFails() {
        kafkaTemplate.setShouldThrowExceptionOnSend(true);
        // Update props
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        notificationService.setProps(properties);
        notificationService.setPartitions(1);
        notificationService.setReplicationFactor((short) 1);

        Admin admin = createMock(Admin.class);
        when(Admin.create(properties)).thenReturn(admin);
        notificationService.notifyActionStarted(action);
    }

    @Test(expected = NotificationFailedException.class)
    public void failsOnSend_topicExists() {
        kafkaTemplate.setShouldThrowExceptionOnSend(true);
        // Update props
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "localhost:9092");
        notificationService.setProps(properties);
        notificationService.setPartitions(1);
        notificationService.setReplicationFactor((short) 1);

        final NewTopic newTopic = new NewTopic(TOPIC, 1, (short) 1);
        mockAdminClient.createTopics(List.of(newTopic));

        Admin admin = createMock(Admin.class);
        when(Admin.create(properties)).thenReturn(admin);

        notificationService.notifyActionStarted(action);
    }

    @Test
    public void notifyActionStarted_kafkaIsPresent_sendsNotification() throws Exception {
        notificationService.notifyActionStarted(action);

        assertEquals(TOPIC, kafkaTemplate.getTopic());
        assertEquals(NOTIFICATION_VERSION, kafkaTemplate.getNotification().getVersion());
        assertEquals(ActionType.RESTORE, kafkaTemplate.getNotification().getAction());
        assertEquals(ACTION_ID, kafkaTemplate.getNotification().getActionId());
        assertEquals(BACKUP_MANAGER_ID, kafkaTemplate.getNotification().getBackupManagerId());
        assertEquals(NotificationStatus.STARTED, kafkaTemplate.getNotification().getStatus());
    }

    @Test(expected = NotificationFailedException.class)
    public void notifyActionStarted_kafkaIsNotPresent_throwsException() {
        kafkaTemplate.setShouldThrowExceptionOnSend(true);

        notificationService.notifyActionStarted(action);
    }

    @Test(expected = NotificationFailedException.class)
    public void notifyActionStarted_messageFailsToBeDelivered_throwsException() throws Exception {
        kafkaTemplate.setShouldThrowExceptionOnMessage(true);

        notificationService.notifyActionStarted(action);
    }

    @Test
    public void notifyActionCompleted_kafkaIsPresent_sendsNotification() throws Exception {
        notificationService.notifyActionCompleted(action);

        assertEquals(TOPIC, kafkaTemplate.getTopic());
        assertEquals(NOTIFICATION_VERSION, kafkaTemplate.getNotification().getVersion());
        assertEquals(ActionType.RESTORE, kafkaTemplate.getNotification().getAction());
        assertEquals(ACTION_ID, kafkaTemplate.getNotification().getActionId());
        assertEquals(BACKUP_MANAGER_ID, kafkaTemplate.getNotification().getBackupManagerId());
        assertEquals(NotificationStatus.COMPLETED, kafkaTemplate.getNotification().getStatus());
    }

    @Test(expected = NotificationFailedException.class)
    public void notifyActionCompleted_kafkaIsNotPresent_doesNotThrowException() throws Exception {
        kafkaTemplate.setShouldThrowExceptionOnSend(true);

        notificationService.notifyActionCompleted(action);
    }

    @Test(expected = NotificationFailedException.class)
    public void notifyActionCompleted_messageFailsToBeDelivered_doesNotThrowException() throws Exception {
        kafkaTemplate.setShouldThrowExceptionOnMessage(true);

        notificationService.notifyActionCompleted(action);
    }

    @Test
    public void notifyActionFailed_kafkaIsPresent_sendsNotification() throws Exception {
        notificationService.notifyActionFailed(action);

        assertEquals(TOPIC, kafkaTemplate.getTopic());
        assertEquals(NOTIFICATION_VERSION, kafkaTemplate.getNotification().getVersion());
        assertEquals(ActionType.RESTORE, kafkaTemplate.getNotification().getAction());
        assertEquals(ACTION_ID, kafkaTemplate.getNotification().getActionId());
        assertEquals(BACKUP_MANAGER_ID, kafkaTemplate.getNotification().getBackupManagerId());
        assertEquals(NotificationStatus.FAILED, kafkaTemplate.getNotification().getStatus());
    }

    @Test(expected = NotificationFailedException.class)
    public void notifyActionFailed_kafkaIsNotPresent_doesNotThrowException() throws Exception {
        kafkaTemplate.setShouldThrowExceptionOnSend(true);

        notificationService.notifyActionFailed(action);
    }

    @Test(expected = NotificationFailedException.class)
    public void notifyActionFailed_messageFailsToBeDelivered_doesNotThrowException() throws Exception {
        kafkaTemplate.setShouldThrowExceptionOnMessage(true);

        notificationService.notifyActionFailed(action);
    }

    private Action mockAction() {
        final Action action = createMock(Action.class);
        EasyMock.expect(action.getActionId()).andReturn(ACTION_ID);
        EasyMock.expect(action.getBackupManagerId()).andReturn(BACKUP_MANAGER_ID);
        EasyMock.expect(action.getName()).andReturn(ActionType.RESTORE);
        EasyMock.replay(action);
        return action;
    }

    private class KafkaTemplateStub extends KafkaTemplate<String, Notification> {

        private String topic;
        private Notification notification;
        private boolean shouldThrowExceptionOnMessage;
        private boolean shouldThrowExceptionOnSend;

        @SuppressWarnings("unchecked")
        public KafkaTemplateStub() {
            super(factory);
        }

        @Override
        @SuppressWarnings("unchecked")
        public CompletableFuture<SendResult<String, Notification>> send(final String topic, final Notification data) {
            this.topic = topic;
            notification = data;

            if(shouldThrowExceptionOnSend) {
                throw new RuntimeException("something something");
            }

            final CompletableFuture<SendResult<String, Notification>> future = createMock(CompletableFuture.class);
            try {
                final IExpectationSetters<SendResult<String, Notification>> expectation = EasyMock.expect(future.get());
                if(shouldThrowExceptionOnMessage) {
                    expectation.andThrow(new RuntimeException());
                } else {
                    expectation.andReturn(null);
                }

            } catch (final Exception e) {
                throw new RuntimeException(e);
            }

            EasyMock.replay(future);

            return future;
        }

        public String getTopic() {
            return topic;
        }

        public Notification getNotification() {
            return notification;
        }

        public void setShouldThrowExceptionOnMessage(final boolean shouldThrowExceptionOnMessage) {
            this.shouldThrowExceptionOnMessage = shouldThrowExceptionOnMessage;
        }

        public void setShouldThrowExceptionOnSend(final boolean shouldThrowExceptionOnSend) {
            this.shouldThrowExceptionOnSend = shouldThrowExceptionOnSend;
        }

    }


}
