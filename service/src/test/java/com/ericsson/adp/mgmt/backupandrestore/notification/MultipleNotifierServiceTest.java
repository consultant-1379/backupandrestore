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

import com.ericsson.adp.mgmt.backupandrestore.action.Action;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

public class MultipleNotifierServiceTest {
    private static final String ACTION_ID = "123";
    private static final String BACKUP_MANAGER_ID = "DEFAULT";

    private MultipleNotifierService multipleNotifierService;

    private KafkaNotifier mockedKafkaNotifier;
    private RedisNotifier mockedRedisNotifier;


    @Before
    public void setUp(){
        mockedKafkaNotifier = Mockito.mock(KafkaNotifier.class);
        mockedRedisNotifier = Mockito.mock(RedisNotifier.class);

        List<Notifier> notifierList  = Arrays.asList(mockedKafkaNotifier,mockedRedisNotifier);
        multipleNotifierService = new MultipleNotifierService();
        multipleNotifierService.setNotifiers(notifierList);
    }

    @Test
    public void verify_sends_notification_started_multiple_notifiers(){
        final Action action = Mockito.mock(Action.class);
        multipleNotifierService.notifyAllActionStarted(action);

        Mockito.verify(mockedKafkaNotifier, Mockito.times(1)).notifyActionStarted(action);
        Mockito.verify(mockedRedisNotifier, Mockito.times(1)).notifyActionStarted(action);
    }

    @Test
    public void verify_sends_notification_completed_multiple_notifiers(){
        final Action action = Mockito.mock(Action.class);
        multipleNotifierService.notifyAllActionCompleted(action);

        Mockito.verify(mockedKafkaNotifier, Mockito.times(1)).notifyActionCompleted(action);
        Mockito.verify(mockedRedisNotifier, Mockito.times(1)).notifyActionCompleted(action);
    }

    @Test
    public void verify_sends_notification_failed_multiple_notifiers(){
        final Action action = Mockito.mock(Action.class);
        multipleNotifierService.notifyAllActionFailed(action);

        Mockito.verify(mockedKafkaNotifier, Mockito.times(1)).notifyActionFailed(action);
        Mockito.verify(mockedRedisNotifier, Mockito.times(1)).notifyActionFailed(action);
    }
}