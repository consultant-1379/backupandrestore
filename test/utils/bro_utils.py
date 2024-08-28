#!/usr/bin/env python3
"""
This module provides various utility methods for the orchestrator
and the agents.
"""
import base64
import time
import re
import bro_data
import helm3procs
import k8sclient
import rest_utils
from bro_s3 import BroS3
import utilprocs
from utilprocs import log
import bro_redis_utils
import subprocess

# Instantiate a kube client
KUBE = k8sclient.KubernetesClient()

# Base REST URL
BASE_URL = "http://eric-ctrl-bro:7001/v1/backup-manager"

DEFAULT_SCOPE = "DEFAULT"

SIP_TLS_CA_SECRET_NAME = "eric-sec-sip-tls-trusted-root-cert"


def install_service_chart(repo_name, chart_name, chart_version, namespace,
                          timeout=120, install_secrets=True,
                          agent_discovery=False, enforce_rest_tls=True,
                          enable_global_tls=True,
                          pvc_size=None,
                          osmn_enabled=False, data=True, yang=False,
                          vbrm_enabled=True, osmn_mode="standalone",
                          kvdb_rd_format=None,
                          kvdb_new_operator=False,
                          parallel_actions=False, cpu_request_size=None,
                          mem_request_size=None, cpu_limit_size=None,
                          mem_limit_size=None, settings=None):
    # pylint: disable=R0912,R0913
    # Disabled R0912 (too-many-branches) due to the if per feature below
    # Disabled R0913 (too-many-arguments) due to the arguments per setting
    """
    Installs the service Chart

    :param repo_name: helm repo name
    :param chart_name: chart name
    :param chart_version: version of chart
    :param namespace : namespace to deploy
    :param timeout : time to wait for deployment
    :param install_secrets: specify if secrets must be set during installation
    :param agent_discovery: indicates if agent discovery is to be enabled
    :param enable_global_tls: indicates if global tls is to be disabled/enabled
    :param enforce_rest_tls: indicates if tls is to be optional/required for
                            the REST interface
    :param pvc_size: the PVC size required for the deployment,
                             if not set BRO is deployed with 15Gi
    :param osmn_enabled: Indicates if required the use of OSMN instead PVC
    :param data: Indicates if the tag data in the intchart be true or false
    :param yang: Indicates if the tag yang in the intchart to be true or false
    :param vbrm_enabled: Indicates if VBRM is enabled for DEFAULT BRM
    :param kvdb_rd_format: Indicates the format of the KVDB RD
                           notification
    :param kvdb_new_operator : It will set the parameters for in-house operator
    :param cpu_request_size: It will set cpu request limit for BRO pod.
    :param mem_request_size: It will set mem request limit for BRO pod.
    :param cpu_request_size: It will set cpu request limit for BRO pod.
    :param cpu_request_size: It will set mem request limit for BRO pod.
    :param settings: additional settings as a dictionary to install chart
    """
    # Set required secret values for service
    if settings is None:
        settings = {}

    if osmn_enabled:
        bros3 = BroS3()
        osmn_settings = bros3.get_settings()

        osmn_enabled = {"osmn.enabled": "true"}
        osmn_host = {"osmn.host": osmn_settings['endpoint']}
        osmn_port = {"osmn.port": osmn_settings['port']}
        settings.update(osmn_enabled)
        settings.update(osmn_host)
        settings.update(osmn_port)
        # If called with an integration chart we need to prepend
        # the settings keys with eric-ctrl-bro.
        # Here we iterate through the settings dictionary
        # and recreate the items
        if repo_name == "int_snap":
            for key in list(settings):
                new_key = "eric-ctrl-bro.{}".format(key)
                settings[new_key] = settings.pop(key)
            osmn_tag = {"tags.osmn_enabled": "true"}
            settings.update(osmn_tag)
            osmn_mode = {"eric-data-object-storage-mn.mode": osmn_mode}
            auto_encryption = {"eric-data-object-storage-mn.autoEncryption.\
enabled": "false"}
            settings.update(osmn_mode)
            settings.update(auto_encryption)

    if install_secrets:
        secrets = {"global.pullSecret": "armdocker"}
        settings.update(secrets)

    if not enable_global_tls:
        global_tls = {"global.security.tls.enabled": "false"}
        settings.update(global_tls)

    if not enforce_rest_tls:
        rest_tls = {"service.endpoints.restActions.tls.enforced": "optional"}
        settings.update(rest_tls)

    if pvc_size:
        pvc_claim_size = {"persistence.persistentVolumeClaim.size": pvc_size}
        settings.update(pvc_claim_size)

    if agent_discovery:
        discover = {"bro.enableAgentDiscovery": "true"}
        settings.update(discover)

    # Turn off BRO notification if not needed
    if not data:
        data_tag = {"tags.data": "false"}
        bro_notification = {"eric-ctrl-bro.bro.enableNotifications": "false"}
        settings.update(data_tag)
        settings.update(bro_notification)

    if yang:
        yang_tag = {"tags.yang": "true"}
        settings.update(yang_tag)

    if vbrm_enabled:
        vbrm_default = {"bro.vBRMAutoCreate": "DEFAULT"}
        settings.update(vbrm_default)

    if parallel_actions:
        parallel_actions_setting = {"bro.enableLimitedParallelActions": "true"}
        settings.update(parallel_actions_setting)

    if kvdb_rd_format:
        kvdb_rd_notification_setting = {"eric-ctrl-bro.keyValueDatabaseRd.\
notificationValueFormat": kvdb_rd_format}
        settings.update(kvdb_rd_notification_setting)

    if kvdb_new_operator:
        kvdb_rd_settings_dict = {}
        global_kvdb_new_operator = {"global.keyValueDatabaseRD.newOperator.\
enabled": "true"}
        kvdb_rd_settings_dict.update(global_kvdb_new_operator)
        local_new_operator = {"eric-ctrl-bro.keyValueDatabaseRD.\
enabled": "true"}
        kvdb_rd_settings_dict.update(local_new_operator)
        settings.update(kvdb_rd_settings_dict)

    if cpu_request_size:
        cpu_request_size_setting = {"resources.backupAndRestore.requests.cpu":
                                    cpu_request_size}
        settings.update(cpu_request_size_setting)

    if mem_request_size:
        mem_request_size_setting = {"resources.backupAndRestore.requests.\
memory": mem_request_size}
        settings.update(mem_request_size_setting)

    if cpu_limit_size:
        cpu_limit_size_setting = {"resources.backupAndRestore.limits.cpu":
                                  cpu_limit_size}
        settings.update(cpu_limit_size_setting)

    if mem_limit_size:
        mem_limit_size_setting = {"resources.backupAndRestore.limits.memory":
                                  mem_limit_size}
        settings.update(mem_limit_size_setting)

    # Avoid cluster role conflict
    cluster_role = {"eric-pm-server.clusterRoleName":
                    "bro-intchart-monitoring-{}".format(namespace)}
    settings.update(cluster_role)

    helm_release_name = \
        get_service_helm_release_name(chart_name, namespace)

    log("Deploy the service chart")
    helm3procs.helm_install_chart_from_repo_with_dict(
        chart_name,
        helm_release_name,
        namespace,
        helm_repo_name=repo_name,
        chart_version=chart_version,
        settings_dict=settings,
        timeout=timeout)
    if osmn_enabled and repo_name == "int_snap":
        bros3.start_client()
    wait_for_resources_to_deploy(helm_release_name, namespace)


def get_service_helm_release_name(service_name, namespace):
    """
    provides a namespace specific release name to limit collision
    in the helm deployment
    :param service_name: the agent_name to be included in the release name
    :return service_helm_release_name: the relase name for the supplied
                                     agent name
    """
    return "{}-{}".format(service_name, namespace)


def install_bro_agents(repo_name, chart_name, chart_version,
                       agent_names, namespace, expect_failure=False,
                       enable_global_tls=True, generate_values_path=True,
                       values_path=None, backup_type_list=None,
                       agent_log_level="info", root_log_level="info"):
    """
    Installs test agents.
    Test Agent Chart name is set with nameOverride and
    the Agent Registration information is set with a specific values file.

    :param repo_name: helm repo name
    :param chart_name: chart name
    :param chart_version: version of chart
    :param agent_names: list of names of agents
    :param namespace: namespace to deploy agents
    :param expect_failure: indicates if deployment is expected to fail
    :param enable_global_tls: indicates if tls is to be disabled/enabled
    :param generate_values_path: Whether or not to generate values path
    :param values_path: Path of the values file to use
    :backup_type_list: a String of comma separated list of backup_types
            enclosed by curly brackets e.g. {configuration_data, subscriber}
    :agent_log_level: logging level for the test agent
    :root_log_level: logging level for the agent's third-party components
    """

    agent_helm_releases = []
    helm_settings = {"global.pullSecret": "armdocker"}

    if not enable_global_tls:
        global_tls = {"global.security.tls.enabled": "false"}
        helm_settings.update(global_tls)

    helm_settings.update({"brAgent.logging.level": agent_log_level})
    helm_settings.update({"brAgent.logging.rootLevel": root_log_level})

    # Use agent_name to derive the required parameters
    for agent_name in agent_names:
        helm_release_name = get_service_helm_release_name(
            agent_name, namespace)
        agent_helm_releases.append(helm_release_name)

        name_override = {"nameOverride": agent_name}
        helm_settings.update(name_override)
        if backup_type_list:
            bra_backup_type_list = {"brAgent.backupTypeList": backup_type_list}
            helm_settings.update(bra_backup_type_list)

        if generate_values_path:
            values_path = "{}_values.yaml.txt".format(agent_name)
        else:
            br_label_value = {"brAgent.brLabelValue": agent_name}
            helm_settings.update(br_label_value)

        log("Install test agent {}".format(agent_name))
        helm3procs.helm_install_chart_from_repo_with_dict(
            chart_name,
            helm_release_name,
            namespace,
            helm_repo_name=repo_name,
            chart_version=chart_version,
            settings_dict=helm_settings,
            values=values_path,
            should_wait=False)

    if not expect_failure:
        for agent_helm_release in agent_helm_releases:
            wait_for_resources_to_deploy(agent_helm_release, namespace)


def get_helm_release_status(release):
    """
    Lists the status of Helm Release

    :param release: helm release name
    """

    # List helm release
    hlist = "helm ls -A --filter " + release
    utilprocs.execute_command(hlist)

    # Status of helm release
    status = "helm status " + release
    utilprocs.execute_command(status)


def wait_for_resources_to_deploy(release, namespace):
    """
    Waits for a k8s resources associated with a helm
    release to be up and running.
    This can be used at install, upgrade and rollback.

    :param release: helm release name
    :param namespace : namespace to run commands
    """

    log("Wait for all k8s resources to be up")
    helm3procs.helm_wait_for_deployed_release_to_appear(
        release, namespace)

    # List status of the helm release
    # (commented for helm3 issue, to be fixed in ADPPRG-40833)
    # get_helm_release_status(release)


def remove_namespace_resources(namespace,
                               remove_redis_cluster=True,
                               remove_kvdb_cluster=False):

    """
    Removes all resources from the BRO namespace.

    :remove_redis_cluster : removes kvdbrdcluster custom object
    :remove_kvdb_cluster : removes rediscluster custom object
    :param namespace : namespace to run commands
    """
    log("Remove all helm releases from namespace")
    helm3procs.helm_cleanup_namespace(namespace)

    try:
        log("Remove all PVCs from namespace")
        # delete pvc with 10 minute timeout
        KUBE.delete_all_pvc_namespace(namespace, 120)
    except Exception as e:
        get_pod_logs(namespace, "eric-ctrl-bro", "pvc_deletion_failed_bro")
        get_pod_logs(namespace, "bravo-agent-0", "pvc_deletion_failed_bravo")
        pods = KUBE.list_pods_from_namespace(namespace)
        log("PVC deletion fails. Current list of pods: {}".format(pods))
        raise

    if remove_redis_cluster:
        log("Remove redis cluster")
        KUBE.delete_namespace_rediscluster(namespace)

    if remove_kvdb_cluster:
        log("Remove KVDB RD cluster")
        bro_redis_utils.delete_namespace_operator_rd(namespace)


def wait_for_agent_to_be_removed(agent_id, namespace):
    """
    Waits until the agent has been removed from the deployment

    :param agent_id: id of agent
    :param namespace: namespace of agent
    """
    count = 0
    agent_pod = "{}-agent-0".format(agent_id)
    log("Waiting until agent {} is no longer deployed".format(agent_id))
    while count < 120:
        podlist = []
        pod_data = KUBE.list_pods_from_namespace(namespace)
        podlist += (item.metadata.name for item in pod_data.items)
        if agent_pod in podlist:
            log("Agent is still deployed")
            count = count + 1
            time.sleep(1)
        else:
            break
    assert agent_id not in podlist, \
        "Timeout waiting for agent {} to undeploy".format(agent_id)


def get_cmyp_pod_logs(test_pod_prefix, namespace, test_case_name,
                      container_names=[]):
    pods = get_pods_by_prefix(namespace, test_pod_prefix)
    utilprocs.log(pods)
    cmyp_pod_name = "eric-cm-yang-provider-"
    cmyp_pod = get_pods_by_prefix(namespace, cmyp_pod_name)[0]

    for container_name in container_names:
        command = [
            "sh", "-c",
            f"kubectl logs {cmyp_pod} -n {namespace} -c {container_name}"
            f" > /var/log/{cmyp_pod}_{container_name}_{test_case_name}.log"
        ]
        KUBE.exec_cmd_on_pod(pods[0], namespace, command)


def get_pod_logs(namespace, pod_name_prefix, log_file_suffix):
    """
    Gets the pod logs for all pods whose name starts with the pod_name_prefix

    :param namespace : The namespace that the pods are in
    :param pod_name_prefix : The prefix for the pods we want to get logs for
    :param log_file_suffix : A string to append to the name of the log files
    """
    pods = get_pods_by_prefix(namespace, pod_name_prefix)

    for pod in pods:
        KUBE.get_pod_logs(namespace, pod, log_file_suffix)


def get_pods_by_prefix(namespace, prefix):
    """
    Gets the names of all the pods that start with a given prefix

    :param namespace : The namespace that the pods are in
    :param prefix: The prefix for the pods we want to return

    :return: List of pod names
    """
    all_pods = KUBE.list_pods_from_namespace(namespace).items
    filtered_pods = []

    for pod in all_pods:
        pod_name = pod.metadata.name
        if prefix in pod_name:
            filtered_pods.append(pod_name)
    return filtered_pods


def secret_exists(secret_name, namespace):
    """
    Checks if a secret exists in a given namespace

    :param secret_name : The name of the secret
    :param namespace : The kubernetes namespace

    :returns: A boolean indicating whether or not the secret exists
    """
    log("Checking if secret {} exists in namespace {}"
        .format(secret_name, namespace))
    exists = len(KUBE.get_namespace_secrets(namespace, [secret_name])) == 1
    log("Secret {} exists: {}".format(secret_name, exists))
    return exists


def create_secret(namespace, secret_name, cert_file_names):
    """
    Creates the secret containing the certificate using a dummy
    certificate file

    :param namespace : The Kubernetes namespace
    :param secret_name: The name of the secret to create
    :param cert_file_names: name/s of the certificates to use in creation
    """
    log("Creating the {} secret".format(secret_name))
    secret_data = {}
    for cert_file_name in cert_file_names:
        file_content = get_file_contents_encoded_as_base64(cert_file_name)
        secret_data[cert_file_name] = file_content

    KUBE.create_namespace_secret(secret_name, namespace,
                                 "generic", secret_data)


def create_secret_plain(namespace, secret_name, data_content):
    """
    Creates secret containing plain data content

    :param namespace : The Kubernetes namespace
    :param secret_name: The name of the secret to create
    :param data_content: plain data content

    """
    log("Creating the {} secret".format(secret_name))
    KUBE.create_namespace_secret(secret_name, namespace,
                                 "generic", data_content)


def delete_secret(namespace, secret_name):
    """
    Deletes the secret containing the certificate, if it exists

    :param namespace: the namespace to delete the secret from
    :param secret_name: The name of the secret to create
    """
    if secret_exists(secret_name, namespace):
        log("Deleting the {} secret".format(secret_name))
        KUBE.delete_namespace_secret(namespace, secret_name)


def get_file_contents_encoded_as_base64(filename):
    """
    Reads a file and returns its contents encoded as base64

    :param filename : The name of the file

    :return: A base64 encoded string
    """
    with open(filename) as file:
        file_contents = bytes(file.read(), "utf-8")
        return base64.b64encode(file_contents).decode("utf-8")


def retrieve_secret(namespace, secret_name):
    """
    Retrieves the secret from a given namespace
    :param namespace: the namespace retrieve the secret from
    :param secret_name: the name of the secret
    """
    log(f"Retrieving the secret {secret_name} from {namespace}")
    secrets = KUBE.get_namespace_secrets(namespace, [secret_name])
    if secrets:
        return secrets[0]
    else:
        raise Exception(f"{secret_name} secret not found in {namespace}")


def base64_decode_val(value):
    """
    Base 64 decodes the value of a nested data structure
    :param value a nested data structure
    """
    if isinstance(value, dict):
        return base64_decode_dict(value)
    elif isinstance(value, list):
        return [base64_decode_val(v) for v in value]
    elif isinstance(value, str):
        return base64.b64decode(value).decode()
    else:
        return value


def base64_decode_dict(d):
    """
    Base64 decodes the values in a dictionary
    :param value the dictionary
    """
    return {k: base64_decode_val(v) for k, v in d.items()}


def get_secret_data(namespace, secret_name):
    """
    Retrieves a base64 decoded secret data
    :param namespace: the namespace retrieve the secret from
    :param secret_name: the name of the secret
    """
    secret = retrieve_secret(namespace, secret_name)
    return base64_decode_val(secret.data)


def retrieve_sip_tls_root_cert(namespace, filename):
    """
    Retrieves the SIP TLS trusted root certificate from the kubernetes secret
    and saves it to a file
    :param namespace: the namespace retrieve the secret from
    :param filename: the name of the file to store the certificate in
    """
    log("Retrieving the SIP TLS trusted root cert")
    secret = retrieve_secret(namespace, SIP_TLS_CA_SECRET_NAME)
    cert_data = base64.b64decode(secret.data["cacertbundle.pem"])
    with open(filename, "wb") as file:
        file.write(cert_data)


def restart_deployment(prefix_deployment, namespace, timeout=120):
    """
    Restarts all the pods in a deployment and waits for it to restore

    :param prefix_deployment: prefix name of deployment to restart
    :param namespace: namespace where pod is located in
    :param timeout: the number of seconds to wait for the existing pods to
                    be deleted
    """
    existing_pods = set(get_pods_by_prefix(namespace, prefix_deployment))
    for pod in existing_pods:
        KUBE.delete_pod(pod, namespace, wait_for_terminating=False)

    new_pods = set(get_pods_by_prefix(namespace, prefix_deployment))

    remaining_wait_time = timeout
    # Wait until the existing pods have terminated
    # and the new pods are starting up
    while (existing_pods & new_pods) or not new_pods:
        if remaining_wait_time < 0:
            raise Exception("The {} did not restart after {}s.".format(
                existing_pods, timeout))
        log("Waiting for the existing pods {} to restart".format(
            existing_pods))
        time.sleep(1)
        remaining_wait_time -= 1
        new_pods = set(get_pods_by_prefix(namespace, prefix_deployment))

    for pod in new_pods:
        KUBE.wait_for_pod_to_start(pod, namespace)


def restart_pod(pod_name, namespace):
    """
    Restarts pod and waits for it to restore

    :param pod_name: name of pod to restart
    :param namespace: namespace where pod is located in
    """
    log("Delete {} pod".format(pod_name))
    KUBE.shoot_pod(pod_name, namespace)

    time.sleep(5)

    log("Waiting for {} to restart".format(pod_name))
    KUBE.wait_for_pod_to_start(pod_name, namespace)


def calc_time(start_time, pod_name, namespace):
    """
    Calculates the time taken for a pod to become ready.
    :param start_time: The time the method was called
    :param pod_name: The name of the pod to wait until ready
    :param namespace: namespace where the pod is located
    """
    KUBE.wait_for_pod_to_start(pod_name, namespace)
    end_time = time.perf_counter()
    return end_time - start_time


def get_brm_view_from_cmm_by_name(brm_name=DEFAULT_SCOPE):
    """
    Get the current view of BRM from CMM
    :return: the current BRM view from CMM
    """
    # Get the object of the ericsson-brm from CMM
    ericsson_brm = rest_utils.get_resource(
        'http://eric-cm-mediator:5003/cm/api/v1/configurations/ericsson-brm'
    )

    # Get the list of BRM from CMM
    brms = ericsson_brm['data']['ericsson-brm:brm']['backup-manager']

    # Find the brm and return it
    for brm in brms:
        if brm['id'] == brm_name:
            return brm
    return ''


def assert_equal(actual, expected):
    """
    Assert the actual equals the expected.
    :param actual: The actual object
    :param expected: The expected object
    """
    # Get the object of the ericsson-brm from CMM
    assert actual == expected, "Expected:{}, Actual:{}"\
        .format(expected, actual)


def calculate_time_in_timezone(timezone, time_in_utc):
    """
    Calculate time in timezone
    :param timezone: timezone
    :param time_in_utc: time in UTC
    """
    if timezone == "Z":
        time_in_timezone = time.localtime(time_in_utc)
    elif timezone[0] == "+":
        '''
        Example: If timezone is +01:00, int(timezone[1:3])
        will extract the number of hours (01) and multiply by 60 * 60
        to get the number of seconds.

        UC: If the current time in the system is 9:30Z and the users timezone
        is +01:00 we will add an hour
        so the time returned will be '10:00+01:00'.
        '''
        timezone_diff = 60 * (60 * int(timezone[1:3]))
        time_in_timezone = time.localtime(time_in_utc + timezone_diff)
    elif timezone[0] == "-":
        timezone_diff = 60 * (60 * int(timezone[1:3]))
        time_in_timezone = time.localtime(time_in_utc - timezone_diff)

    return time_in_timezone


def remove_trailing_zeros(timestamp):
    """
    Removes the trailing zeros in the microseconds
    portion of the timestamp.
    For example,
    Input: 2023-01-06T06:41:02.12300+01:00
    Output: 2023-01-06T06:41:02.123+01:00
    OR
    Input: 2023-01-06T06:41:02.12300
    Output: 2023-01-06T06:41:02.123
    :param timestamp the timestamp string with or without a timezone
    """
    # The regex pattern captures the microseconds portion
    # and the optional timezone portion of the inputted timestamp.
    # The captured pattern is divided into 3 groups.
    # For an input: 2023-01-06T06:41:02.12300000+01:00
    # The 3 captured groups are:
    # 1. .123
    # 2. 00000
    # 3. +01:00
    # This method removes the second group (00000)
    # by replacing r"\1\2\3" with r"\1\3"
    capture_group = r"(\.\d*[1-9])(0*)(Z|[\+\-]\d{2}:\d{2})?$"
    replacement = r"\1\3"
    return re.sub(capture_group, replacement, timestamp)


def assert_time_matches_regex_eoi(value):
    """
    Checks the time provided matches
    regex in EOI for startTime, stopTime of
    scheduler events (periodic, calendar_event)
    :param time - time
    """
    error_message = '{} does not match regex in EOI model!'.format(value)
    regex_eoi = \
        r'\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?(Z|[\+\-]\d{2}:\d{2})'
    assert re.match(regex_eoi, value), error_message


def check_periodic_event_contents(expected,
                                  actual):
    """
    Checks contents of periodic event from CMYP
    :param expected: expected periodic values tuple
                     (minutes, hours, start_time, stop_time)
    :param actual: actual periodic values tuple
                   (minutes, hours, start_time, stop_time)
    """
    for i in range(len(expected) - 2):
        assert_equal(expected[i], actual[i])

    # Check start_time
    check_time_and_timezone(expected[2], actual[2])

    # Check stop_time
    check_time_and_timezone(expected[3], actual[3])


def check_time_and_timezone(expected, actual):
    """
    Checks time and timezone separately
    :param expected: datetime string
    :param actual: datetime string
    """
    timestamp_re = r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}"
    timezone_re = r"Z|[\+\-]\d{2}:\d{2}"

    # Strip timezone value. Replaces regex match with second parameter
    expected_datetime = re.sub(timezone_re, '', expected)
    actual_datetime = re.sub(timezone_re, '', actual)
    assert_equal(expected_datetime, actual_datetime)

    expected_timezone = re.sub(timestamp_re, '', expected)

    if expected_timezone[1:] != "00:00":
        # Check timestamp matches
        actual_timezone = re.sub(timestamp_re, '', actual)
        assert_equal(expected_timezone, actual_timezone)
    else:
        # else check actual is either +/- 00:00
        actual_timezone = re.sub(timestamp_re, '', actual)
        assert_equal(actual_timezone[1:], "00:00")


def generate_backup_manager_task(task,
                                 scope,
                                 method,
                                 task_state,
                                 backup_name=None,
                                 action_id=None):
    """
    Generates backup-manager task for comparision
    :param task: the name of the expected task
    :param scope: the name of the expected backup manager
    :param method: the method expected in the task
    :param task_state: the task is either Available or Ongoing
    :param backup_name: name of the backup
    :param action_id: the id of the action
    """
    endpoint = "/backup-restore/v4/backup-managers"
    if task_state == 'Available':
        if task == 'import':
            href = "{}/{}/imports".format(endpoint, scope)
        elif task == 'create':
            href = "{}/{}/backups".format(endpoint, scope)
        elif task == 'restore':
            href = "{}/{}/backups/{}/restores".format(endpoint, scope,
                                                      backup_name)
        elif task == 'export' and backup_name:
            href = "{}/{}/backups/{}/exports".format(endpoint, scope,
                                                     backup_name)
        elif task == 'delete' and backup_name:
            href = "{}/{}/backups/{}".format(endpoint, scope, backup_name)
        else:
            raise Exception("Task is not the expected Availabletasks")
    elif task_state == 'Ongoing':
        if task == 'import':
            href = "{}/{}/imports/{}".format(endpoint, scope, action_id)
        elif task == 'create':
            href = "{}/{}/backups/{}".format(endpoint, scope, backup_name)
        elif task == 'restore':
            href = "{}/{}/backups/{}/restores/{}".format(
                endpoint, scope, backup_name, action_id)
        elif task == 'export':
            href = "{}/{}/backups/{}/exports/{}".format(
                endpoint, scope, backup_name, action_id)
        else:
            raise Exception("Task is not the expected OngoingTasks")
    else:
        raise Exception("Task type is not Available or Ongoing")

    task_endpoint = {
        task:
            {'href': href,
                'method': method}
        }

    return task_endpoint


def validate_checksum_size(path, pod_name, namespace, container):
    """
    Fetches the checksum for the file in the provided path from the
    container and verfies that the checksum is always 16 bits
    in length

    :param path: path on pod
    :param pod: pod holding the files
    :param namespace: namespace where pods deployed
    :param container: container on the pod
    """

    checksum = bro_data.read_container_content(path,
                                               pod_name,
                                               namespace,
                                               container)

    utilprocs.log("Checksum: {}".format(checksum))

    assert_equal(len(checksum), 16), \
        "Checksum is not 16 bits in length"

    utilprocs.log("Checksum is 16 bits in length")

    leading_characters = checksum[:2]
    if leading_characters[:1] == "0" or leading_characters == "00":
        utilprocs.log("Checksum has leading zeros: {}".format(checksum))
