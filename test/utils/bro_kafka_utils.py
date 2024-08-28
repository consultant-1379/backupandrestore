"""
Module with functions for interacting with Kafka
"""
import json

from kafka import KafkaConsumer
import utilprocs
import time


def validate_notifications(bro_kafka_consumer, action_id,
                           backup_manager_id, action):
    """
    Validates that restore or create backup notifications
    have been published by the Orchestrator and that they
    are in the correct format

    :param bro_kafka_consumer : An instance of BroKafkaConsumer
    :param action_id: the expected action id of the message
    :param backup_manager_id: the expected backup manager id of the message
    :param action: RESTORE or CREATE_BACKUP
    """
    messages = bro_kafka_consumer.get_messages(2)
    _validate_message(messages[0], action_id, backup_manager_id, action,
                      'STARTED')
    _validate_message(messages[1], action_id, backup_manager_id, action,
                      'COMPLETED')


def check_for_action_start_completed_notification(bro_kafka_consumer,
                                                  action_id,
                                                  backup_manager_id,
                                                  action, retry=3):
    """
    Validates that restore or create backup notifications
    have been published by the Orchestrator and that they
    are in the correct format. Using this for scheduling events where
    new event could be triggered

    :param bro_kafka_consumer : An instance of BroKafkaConsumer
    :param action_id: the expected action id of the message
    :param backup_manager_id: the expected backup manager id of the message
    :param action: RESTORE or CREATE_BACKUP
    :param retry: times to retry to look for complete message
    """

    action_id_messages = []
    while retry > 0 and len(action_id_messages) < 2:
        messages = bro_kafka_consumer.get_messages(4)
        utilprocs.log('Messages: {}'.format(messages))

        for message in messages:
            message_dict = json.loads(message)
            if message_dict["actionId"] == action_id:
                action_id_messages.append(message)
        time.sleep(30)
        retry -= 1

    if len(action_id_messages) < 2:
        raise Exception('Failed to retrieve started/completed'
                        ' message after {} sec', retry*30)
    else:
        _validate_message(action_id_messages[0], action_id,
                          backup_manager_id, action, 'STARTED')
        _validate_message(action_id_messages[1], action_id,
                          backup_manager_id, action, 'COMPLETED')


def _validate_message(message, action_id, backup_manager_id, action, status):
    """
    Validates a restore notification message to make sure it's in the
    correct format

    :param message : the kafka message to validate
    :param action_id: the expected action id of the message
    :param backup_manager_id: the expected backup manager id of the message
    :param action: RESTORE or CREATE_BACKUP
    :param status: the expected status of the message
    """

    utilprocs.log('Validating notification message: {}'
                  .format(message))

    expected_message = {
        'version': '1.0.0',
        'action': action,
        'actionId': action_id,
        'backupManagerId': backup_manager_id,
        'status': status
    }

    error = 'Invalid value for notification: Expected message: ' \
            '"{}", Actual message: "{}"'

    restore_notification = json.loads(message)
    assert expected_message == restore_notification, error.format(
        expected_message, restore_notification)


class BroKafkaConsumer:
    """
    This class is used for reading kafka messages
    """

    def __init__(self, kafka_service_name, topic):
        self.consumer = KafkaConsumer(
            topic, bootstrap_servers=[kafka_service_name],
            consumer_timeout_ms=3000, auto_offset_reset='earliest')

    def get_messages(self, number_of_messages, raw=False):
        """
        Reads kafka messages from a topic

        :param number_of_messages: The number of messages to read
        :param raw: Whether the messages should be returned in their raw format
        or parsed into strings

        :return: A list of messages
        """
        messages = []

        for raw_message in self.consumer:
            if raw:
                messages.append(raw_message)
            else:
                messages.append(str(raw_message.value, 'UTF-8'))
        return messages[-number_of_messages:]

    def close(self):
        """
        Closes the kafka consumer
        """
        self.consumer.close()
