"""
Module with functions for interacting with Redis
"""
import json
import bro_utils
from rediscluster import RedisCluster
import utilprocs
from utilprocs import execute_command, log

podname = ""


def get_operand_pod_name(namespace):
    """
    Get the name of redis operand pods
    :param namespace: the namespace of the pods
    :return: the name of redis pods
    """
    global podname
    podname = "eric-data-key-value-database-rd-operand"
    response = execute_command(
        "kubectl -n {} get po -l app.kubernetes.io/"
        "name={} "
        "-o jsonpath={{..metadata.name}}".format(namespace, podname))

    if response == "":
        podname = "eric-cloud-native-kvdb-rd-operand"
        response = execute_command(
                "kubectl -n {} get po -l app.kubernetes.io/"
                "name={} "
                "-o jsonpath={{..metadata.name}}".format(namespace, podname))
    return response.split()


def delete_namespace_operator_rd(namespace):
    """
    delete the redis cluster in a specific namespace.

    :param namespace: namespace.
    """
    exception_count = 3
    while True:
        try:
            utilprocs.log(
                'Deleting the redis cluster in {}'.format(namespace))
            utilprocs.execute_command(
                "kubectl -n {} delete KVDBRDCluster --all"
                .format(namespace))
        except Exception as e_obj:
            utilprocs.log(
                (
                    "Exception when trying to delete the "
                    "redis cluster in namespace {}: {}".format(
                        namespace, e_obj)
                )
            )
            exception_count -= 1
            if exception_count <= 0:
                raise
            continue
        return


def get_ip_by_pod_name(pods, namespace):
    """
    Get the ips of the pods
    :param pods: the name of pods
    :param namespace: the namespace of the pods
    :return: the ips of pods
    """
    ips = []
    for pod in pods:
        res = execute_command(
            "kubectl -n {} get pods {} -o=jsonpath="
            "{{.status.podIP}}".format(namespace, pod))
        ips.append(res)
    return ips


def validate_json_notification(message, action_id,
                               backup_manager_id, action, status):
    """
    Validates a notification message to make sure it's in the
    JSON format

    :param message: the redis message
    :param action_id: the expected action id of the message
    :param backup_manager_id: the expected backup manager id of the message
    :param action: RESTORE or CREATE_BACKUP
    :param status: STARTED, FAILED or COMPLETED
    """

    expected_message = {
        'version': '1.0.0',
        'action': action,
        'actionId': action_id,
        'backupManagerId': backup_manager_id,
        'status': status
    }

    error = 'Invalid value for notification: Expected message: ' \
            '"{}", Actual message: "{}"'

    notification = json.loads(message)
    assert expected_message == notification, error.format(
        expected_message, notification)


def validate_json_notifications(bro_redis_client, action_id,
                                backup_manager_id, action, status):
    """
    Validates that restore or create backup notifications
    have been published by the Orchestrator and that they
    are in the correct format

    :param bro_redis_client: An instance of RedisClient
    :param action_id: the expected action id of the message
    :param backup_manager_id: the expected backup manager id of the message
    :param action: RESTORE or CREATE_BACKUP
    :param status: FAILED or COMPLETED
    """

    previous_message = bro_redis_client.get_previous_notification()
    validate_json_notification(
        previous_message, action_id, backup_manager_id, action, "STARTED")

    latest_message = bro_redis_client.get_latest_notification()
    validate_json_notification(
        latest_message, action_id, backup_manager_id, action, status)


class RedisClient:
    """
    This class is used for reading redis messages
    """

    def __init__(self, stream, namespace):
        self.stream = stream
        operand_ips = get_ip_by_pod_name(
            get_operand_pod_name(namespace), namespace)
        startup_nodes = []
        for operand_ip in operand_ips:
            startup_nodes.append(
                {"host": operand_ip, "port": "6379"}
            )
        secretname = "{}-secret-default".format(podname)
        SECRET = bro_utils.get_secret_data(namespace, secretname)
        PASS = SECRET['password']
        self.client = RedisCluster(
            startup_nodes=startup_nodes,
            decode_responses=True,
            skip_full_coverage_check=True,
            password=PASS
        )

    def get_latest_notification(self):
        """
        Reads the latest redis message
        """
        return self.client.xrevrange(self.stream,
                                     count=1)[0][1]['notification']

    def get_notifications(self, number_of_messages=1):
        """
        Gets a number of redis messages
        """
        messages = self.client.xrevrange(self.stream,
                                         count=number_of_messages)
        return messages

    def get_previous_notification(self):
        """
        Reads the previous redis messages.

        Count is equal to 2 to get the last two notifications.
        Gets the second notification from the list and then gets
        the values of the notification.
        """
        return self.client.xrevrange(self.stream,
                                     count=2)[1][1]['notification']

    def validate_notifications(self, action_id,
                               backup_manager_id, action, count=2):
        """
        Validates a number of notifications last in redis and confirms
        for an actions 'STARTED' and 'COMPLETED' are sent in the right format

        :param count: number of notifications to validate
        :param action_id: the expected action id of the message
        :param backup_manager_id: the expected backup manager id of the message
        :param action: RESTORE or CREATE_BACKUP
        """
        notifications = self.get_notifications(count)

        action_id_notifications = []
        for notification in notifications:
            log("notification {}".format(notification))
            notif = notification[1]['notification']
            if action_id in notif:
                action_id_notifications.append(notif)

        expected_message_s = \
            'Notification [version=1.0.0, action={}, actionId={},' \
            ' backupManagerId={}, status={}]'.format(
                action, action_id, backup_manager_id, 'STARTED')

        expected_message_c = \
            'Notification [version=1.0.0, action={}, actionId={},' \
            ' backupManagerId={}, status={}]'.format(
                action, action_id, backup_manager_id, 'COMPLETED')

        error = 'Invalid value for notification: Expected message: ' \
                '"{}", Actual message: "{}"'

        assert expected_message_s == action_id_notifications[1], \
            error.format(expected_message_s, action_id_notifications[0])

        assert expected_message_c == action_id_notifications[0], \
            error.format(expected_message_c, action_id_notifications[1])

    def validate_notification(self, action_id, backup_manager_id, action):
        """
        Validates a notification message to make sure it's in the
        correct format

        :param action_id: the expected action id of the message
        :param backup_manager_id: the expected backup manager id of the message
        :param action: RESTORE or CREATE_BACKUP
        """

        expected_message = \
            'Notification [version=1.0.0, action={}, actionId={},' \
            ' backupManagerId={}, status=COMPLETED]'.format(
                action, action_id, backup_manager_id)

        actual_message = self.get_latest_notification()

        error = 'Invalid value for notification: Expected message: ' \
                '"{}", Actual message: "{}"'

        assert expected_message == actual_message, \
            error.format(expected_message, actual_message)

    def validate_notification_failure(self, action_id,
                                      backup_manager_id, action):
        """
        Validates a notification message to make sure it's in the
        correct format

        :param action_id: the expected action id of the message
        :param backup_manager_id: the expected backup manager id of the message
        :param action: RESTORE or CREATE_BACKUP
        """

        expected_message = \
            'Notification [version=1.0.0, action={}, actionId={},' \
            ' backupManagerId={}, status=FAILED]'.format(
                action, action_id, backup_manager_id)

        actual_message = self.get_latest_notification()

        error = 'Invalid value for notification: Expected message: ' \
                '"{}", Actual message: "{}"'

        assert expected_message == actual_message, \
            error.format(expected_message, actual_message)

    def close(self):
        """
        Closes the redis client
        """
        self.client.close()
