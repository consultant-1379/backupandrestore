#!/usr/bin/env python3
"""
This module deploys the BRO integration chart with
DDC and CertM enabled and runs a yang command to verify
the BRO model deployed successfully without conflicts
"""
import os
import time
import bro_ctrl
import bro_utils
import bro_sftp
import helm3procs
import utilprocs
import bro_cmyp_cli_client
import yang_model_utils
from k8sclient import KubernetesClient

NAMESPACE = os.environ.get("kubernetes_namespace")
INT_SNAP_REPO = os.environ.get("helm_repo")
INT_SNAP_VERSION = os.environ.get('baseline_chart_version')
INT_SNAP_REPO_NAME = "int_snap"
INT_CHART_NAME = "eric-bro-int"
DEPLOY_TIMEOUT = 900

BRO_CTRL = bro_ctrl.BroCtrlClient()

BRO_POD_NAME = "eric-ctrl-bro-0"

# Define CMYP CLI data
CMYP_USER = "brocmyp"
CMYP_USER2 = "brocmyp1"
CMYP_PSW = "TeamPlanBYang$1"
CMYP = bro_cmyp_cli_client.CMYPCliClient(CMYP_USER, CMYP_PSW, BRO_CTRL)
CMYP2 = bro_cmyp_cli_client.CMYPCliClient(CMYP_USER2, CMYP_PSW, BRO_CTRL)

MODEL = yang_model_utils.YangModelUtils(CMYP, CMYP2, BRO_CTRL)

# Set BRO variables
BRO_NAME = "eric-ctrl-bro"

# SFTP data
SFTP_SERVER_PATH = "bro/backupManagers/DEFAULT/sftp-server"
SFTP_SERVER_NAME = "bro-test-sftp-0"
SFTP_SERVER_FILENAME = "bro-test-sftp-0.json"
SFTP_ENDPOINT = "bro-sftp"
SFTP_REMOTE_PORT = "22"
SFTP_REMOTE_PATH = "bro_test"
SFTP_USERNAME = "foo"
SFTP_PASSWORD = "planbsftp"

SFTP_SERVER = bro_sftp.SFTPServer(SFTP_USERNAME, SFTP_PASSWORD)

# dummy SFTP data
DUMMY_SFTP_NAME_1 = "bro_dummy_sftp_1"
DUMMY_SFTP_FILENAME_1 = DUMMY_SFTP_NAME_1 + ".json"
DUMMY_SFTP_NAME_2 = "bro_dummy_sftp_2"
DUMMY_SFTP_FILENAME_2 = DUMMY_SFTP_NAME_2 + ".json"
DUMMY_SFTP_ENDPOINT = "bro_dummy_sftp"
DUMMY_SFTP_REMOTE_ADDRESS = "1.2.3.4"
DUMMY_SFTP_REMOTE_PORT = "22"
DUMMY_SFTP_REMOTE_PATH = "bro_dummy"
DUMMY_SFTP_USERNAME = "dummy"
DUMMY_PRIVATE_KEY = 'LS0tLS1CRUdJTiBSU0EgUFJJVkFURSBLRVktLS0tLQpN'\
    'SUlDWEFJQkFBS0JnUURMd3FLOWFuTjdrV3d3UDBNNFRlK3V3VzhmbzFETUZKK2J3aEdy'\
    'b1BUVys3eTZHYWFmCms5Q1BnazZ0Z1p0bmZJaGVHNmpDTERZZ2JhUFRFMVJoU2xqcWUw'\
    'OEdSUDhnbWpJMStQbk5ySWRzc2N2UEdZUDIKZFdJQktuWEdBME9ERXkvSHVubytQdVZU'\
    'Q0Y4ZkVyTCsvdVlsZnl4SzAvUDZRSWU0bXV3R1E5TEhTd0lEQVFBQgpBb0dCQUtHcHlt'\
    'N1liOG1oaHFIOC8zdERzcUFIandBZUZuUGxCUUdTaDJ5UnB0Q1BLSnpLenoyYkRJVmdF'\
    'TVp1CnAzc0MvanNVWE4rNkxqT3FhYW1GazVRaFpjc3hKd0dvb2xsSENzZTdTVDJ0Yzh0'\
    'V1ZYZ3lvbXRZeG0rdkxORXIKcDFuSXFmT1pHM0ljWHNFbGd1N2ZFY1U3NlhhUXQvOSsz'\
    'RWhYOW5Memp4eFFWRW81QWtFQTlkQUR1bnljMFJ4aApGc0tKbTBVNkY5R1hoRzk0Ty9n'\
    'RjNTUU50Zis0WHRVemVYWDJzMy9QU0Y3WHVETnF1cGlWQlBDbUYzc0c0ZkdkCkhyQUZ1'\
    'eElsZndKQkFOUTBkaGxaN0E5ekhaaXd4NC9pbGtkNlJDVndsdXgvVlA0b3NSM0d4QTJQ'\
    'dnNXV2ZNbGgKMzYxOUplWTEyRzFvM0s5dW1xbnBtSjJ2WjIyWGpYZlkvRFVDUUJXZGxD'\
    'WmEvT002anFNVXVrc0lIR0twMEp3Wgp2VVh3dW50R3gxbmd3ZEs5QnNqQWdkQXBCZzVF'\
    'SU00VzMyVEhOQnQ2R2ZEb3BhTkt2dGxLKzBZSEEzc0NRQTJoCnVSeHp6RmhKdXRMK09E'\
    'T2tDS2RhMFZuZVc3T2hrbUlwVndka3AxOS9wbSttOVNreW9sM1F0a2lObVBPZVV0N3EK'\
    'S2dGVVFqdU84d1AxUWw4Nmp5a0NRSCthbmx4L2ZXbFNabi9HZmFxb0MyaHJKYktxLzBY'\
    'SUZQV0YyclYybjhocgo0bzE0cGxSZ3d5ZEFMa1NFQjVVaW94Y2x1ZDF3UWwxZ0JieHRP'\
    'UWZjb0FzPQotLS0tLUVORCBSU0EgUFJJVkFURSBLRVktLS0tLQo='
DUMMY_PUBLIC_KEY = 'c3NoLXJzYSBBQUFBQjNOemFDMXljMkVBQUFBREFRQUJBQ'\
    'UFBZ1FETHdxSzlhbk43a1d3d1AwTTRUZSt1d1c4Zm8xRE1GSitid2hHcm9QVFcrN3k2R'\
    '2FhZms5Q1BnazZ0Z1p0bmZJaGVHNmpDTERZZ2JhUFRFMVJoU2xqcWUwOEdSUDhnbWpJM'\
    'StQbk5ySWRzc2N2UEdZUDJkV0lCS25YR0EwT0RFeS9IdW5vK1B1VlRDRjhmRXJMKy91W'\
    'WxmeXhLMC9QNlFJZTRtdXdHUTlMSFN3PT0gcm9vdEBicm8tdGVzdC1zZnRwLTAK'
DUMMY_HOST_KEY = 'c3NoLXJzYSBBQUFBQjNOemFDMXljMkVBQUFBREFRQUJBQ'\
    'UFBZ1FETHdxSzlhbk43a1d3d1AwTTRUZSt1d1c4Zm8xRE1GSitid2hHcm9QVFcrN3k2R'\
    '2FhZms5Q1BnazZ0Z1p0bmZJaGVHNmpDTERZZ2JhUFRFMVJoU2xqcWUwOEdSUDhnbWpJM'\
    'StQbk5ySWRzc2N2UEdZUDJkV0lCS25YR0EwT0RFeS9IdW5vK1B1VlRDRjhmRXJMKy91W'\
    'WxmeXhLMC9QNlFJZTRtdXdHUTlMSFN3PT0gcm9vdEBicm8tdGVzdC1zZnRwLTAK'

# Scope
DEFAULT_SCOPE = "DEFAULT"
CONFIGURATION_SCOPE = "configuration-data"

# Define Backup Names
BACKUP_NAMES = ["backup_1", "backup_2"]

# Periodic event id
EVENT_ID = "id1"

# dummy periodic event data
DUMMY_EVENT_ID_1 = "dummy_id1"
DUMMY_EVENT_ID_2 = "dummy_id2"
DUMMY_EVENT_ID_3 = "dummy_id3"
DUMMY_EVENT_FILENAME_1 = DUMMY_EVENT_ID_1 + ".json"
DUMMY_EVENT_FILENAME_2 = DUMMY_EVENT_ID_2 + ".json"
DUMMY_EVENT_FILENAME_3 = DUMMY_EVENT_ID_3 + ".json"
PERIODIC_EVENT_PATH = "bro/backupManagers/DEFAULT/periodic-events"
PERIODIC_EVENT_PATH_CONFIG =\
    "bro/backupManagers/configuration-data/periodic-events"

KUBE = KubernetesClient()

TEST_POD_PREFIX = "test-yang-model-test"

SETTINGS_DICT = {"tags.data": "false",
                 "tags.logging": "true",
                 "tags.yang": "true",
                 "eric-ctrl-bro.bro.enableNotifications": "false"}

CONTAINER_NAMES = [
        "yang-engine",
        "yang-db-adapter",
        "schema-synchronizer"
    ]


# Test Cases
def test_clear_any_failed_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    utilprocs.log("Test Case: test_clear_any_failed_resources")

    bro_utils.remove_namespace_resources(NAMESPACE)
    utilprocs.log("Finished removing Kubernetes resources")


def test_deploy_integration_chart():
    """
    Deploys BRO Integration Helm Chart.
    """

    utilprocs.log("Test Case: test_deploy_integration_chart")

    utilprocs.log(
        "Add the Int Snapshot helm repo {0} - {1}".format(INT_SNAP_REPO_NAME,
                                                          INT_SNAP_REPO))
    helm3procs.add_helm_repo(INT_SNAP_REPO, INT_SNAP_REPO_NAME)

    # Install Integration Chart with yang tag as true
    bro_utils.install_service_chart(INT_SNAP_REPO_NAME, INT_CHART_NAME,
                                    INT_SNAP_VERSION, NAMESPACE,
                                    DEPLOY_TIMEOUT,
                                    settings=SETTINGS_DICT)


def test_verify_models_are_loaded_in_cmyp():
    """
    Verifies that the BRM, DDC and CertM models deployed successfully
    """

    test_case = "test_verify_models_are_loaded_in_cmyp"
    utilprocs.log("Test Case: {}".format(test_case))

    # Prepares the cmyp user and connects to the server
    CMYP.prepare_cmyp(CMYP_PSW, CMYP_USER)

    # Verifies that brm config deployed successfully
    MODEL.verify_brm_config()

    # Verifies that ddc config deployed successfully
    ddc_model = MODEL.get_model_config(
        model="diagnostic-data-collection")
    assert "HelmChartValues" in ddc_model

    # Prepare a new connection to cmyp using the second user
    # with security privileges' to check CertM model
    CMYP2.prepare_cmyp(CMYP_PSW, CMYP_USER2,
                       user_idnumber="711",
                       user_role="system-security-admin")

    # Verifies that the certM model has deployed successfully
    truststore_response = MODEL.get_certm_model_config(
        model='truststore')
    assert "syntax error: incomplete path" \
        in truststore_response, "CertM model has not loaded"

    keystore_response = MODEL.get_certm_model_config(
        model='keystore')
    assert "syntax error: incomplete path" in keystore_response, \
        "CertM model has not loaded"

    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, test_case)


def test_start_sftp_server_pod():
    """
    Starts the SFTP server and waits for the server to be up and running
    """
    test_case = "test_start_sftp_server_pod"
    utilprocs.log("Test Case: {}".format(test_case))

    SFTP_SERVER.start_sftp_server()
    SFTP_SERVER.wait_to_start()

    KUBE.get_pod_logs(NAMESPACE, SFTP_SERVER_NAME, test_case)


def test_define_sftp_server_in_bro():
    """
    Generates a public and private key pair
    Adds the user to the SFTP server pod,
    gets the host key from the SFTP server pod and
    gets the IP of the SFTP server pod
    Add the SFTP Server Configuration to BRO
    """
    test_case = "test_define_sftp_server_in_bro"
    utilprocs.log("Test Case: {}".format(test_case))

    # Generate the public and private keys
    pods = bro_utils.get_pods_by_prefix(NAMESPACE, TEST_POD_PREFIX)
    utilprocs.log(pods)
    test_pod = pods[0]
    keys = SFTP_SERVER.generate_ssh_keys(filename="rsakeypair",
                                         pod=test_pod,
                                         namespace=NAMESPACE)

    # Add the user and the public key to the SFTP server
    SFTP_SERVER.add_user_to_sftp_server(
        SFTP_SERVER_NAME, NAMESPACE, keys[0])

    # Get host key from the SFTP server
    sftp_host_key = SFTP_SERVER.get_host_key_from_sftp_server(
        SFTP_SERVER_NAME, NAMESPACE)

    # Get the IP address of the SFTP server
    sftp_remote_address = SFTP_SERVER.get_sftp_ipaddress(
        namespace=NAMESPACE)

    # Defines an SFTP Server in BRO
    MODEL.define_sftp_server(SFTP_SERVER_NAME, SFTP_ENDPOINT,
                             sftp_remote_address, SFTP_REMOTE_PORT,
                             SFTP_REMOTE_PATH, SFTP_USERNAME,
                             keys[1], keys[3],
                             sftp_host_key)
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, test_case)
    collect_cmyp_logs(test_case)
    MODEL.verify_sftp_persisted_data_in_brm("create", SFTP_SERVER_FILENAME,
                                            SFTP_SERVER_PATH, 1,
                                            BRO_POD_NAME, BRO_NAME,
                                            NAMESPACE, [SFTP_SERVER_NAME,
                                                        SFTP_ENDPOINT,
                                                        SFTP_REMOTE_PORT,
                                                        SFTP_REMOTE_PATH,
                                                        keys[1],
                                                        sftp_host_key])
    # Will be investigated in  the ticket ADPPRG-183720
    # Not able to properly fetch the private and host keys
    # MODEL.verify_sftp_server_on_cmyp(SFTP_SERVER_NAME, SFTP_ENDPOINT,
    #                                  SFTP_REMOTE_PORT, SFTP_REMOTE_PATH,
    #                                  SFTP_USERNAME, keys[1],
    #                                  sftp_host_key,
    #                                  backup_manager=DEFAULT_SCOPE)


def test_create_backup_cmyp():
    """
    Creates a backup and waits until backup is complete
    """
    test_case = "test_create_backup_cmyp"
    utilprocs.log("Test Case: {}".format(test_case))

    # Update housekeeping config to set max-backups to 2
    # and auto-delete to enabled
    CMYP.set_housekeeping_configuration("enabled", "2",
                                        backup_manager=DEFAULT_SCOPE)

    bro_utils.assert_equal(
        CMYP.get_housekeeping_configuration(
            backup_manager=DEFAULT_SCOPE), ("2", "enabled"))

    progress_report_id = CMYP.create_backup(BACKUP_NAMES[0])

    wait_until_bro_cmyp_ready_after_action(
        progress_report_id, "CREATE_BACKUP", scope=DEFAULT_SCOPE,
        backup_name=BACKUP_NAMES[0])

    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, test_case)
    collect_cmyp_logs(test_case)


def test_export_backup_cmyp():
    """
    Test to ensure Export can be run using the configured SFTP server.
    Exports a backup and waits until the export is complete
    """
    test_case = "test_export_backup_cmyp"
    utilprocs.log("Test Case: {}".format(test_case))

    progress_report_id = CMYP.export_backup_key_auth(BACKUP_NAMES[0],
                                                     SFTP_SERVER_NAME)

    export_result = None
    while export_result is None:
        result = CMYP.get_progress_report_attribute(
            progress_report_id, DEFAULT_SCOPE, BACKUP_NAMES[0], "result")
        if result == "success":
            export_result = "success"
        elif result == "failure":
            raise Exception("Export has failed")

    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, test_case)
    collect_cmyp_logs(test_case)


def test_import_backup_cmyp():
    """
    Test to ensure Import backup can be run using the configured SFTP server
    Imports a backup and waits for the import to be complete
    """
    test_case = "test_import_backup_cmyp"
    utilprocs.log("Test Case: {}".format(test_case))

    # delete the backup before import
    progress_report_id = CMYP.delete_backup(
        BACKUP_NAMES[0], backup_manager=DEFAULT_SCOPE)

    wait_until_bro_cmyp_ready_after_action(
        progress_report_id, "DELETE_BACKUP", scope=DEFAULT_SCOPE,
        backup_name=BACKUP_NAMES[0])

    progress_report_id = CMYP.import_backup_key_auth(SFTP_SERVER_NAME,
                                                     BACKUP_NAMES[0])

    wait_until_bro_cmyp_ready_after_action(
        progress_report_id, "IMPORT", scope=DEFAULT_SCOPE,
        backup_name=BACKUP_NAMES[0])

    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, test_case)
    collect_cmyp_logs(test_case)


def test_auto_export_backup_cmyp():
    """
    Test to ensure Auto Export can be run using the configured SFTP server
    """
    test_case = "test_auto_export_backup_cmyp"
    utilprocs.log("Test Case: {}".format(test_case))

    # Create a Scheduled backup to use the SFTP Server
    CMYP.set_scheduler_configuration(BACKUP_NAMES[1],
                                     backup_manager=DEFAULT_SCOPE,
                                     auto_export="enabled",
                                     sftp_server_name=SFTP_SERVER_NAME)

    bro_utils.assert_equal(CMYP.get_scheduler_configuration(
        backup_manager=DEFAULT_SCOPE, sftp_credentials="sftp-server-name"),
                           ("unlocked", "enabled", BACKUP_NAMES[1],
                            SFTP_SERVER_NAME))

    # Create Periodic Event
    now = time.time()
    start_time = time.localtime(now)
    # stopTime is 2.5 hours from now
    stop_time = time.localtime(now + 60*(60+60+30))
    date_format = "%Y-%m-%dT%H:%M:%S"

    f_start_time = time.strftime(date_format, start_time)
    f_stop_time = time.strftime(date_format, stop_time)

    CMYP.create_or_update_periodic_event(
        EVENT_ID, "1", "0",
        DEFAULT_SCOPE,
        f_start_time,
        f_stop_time)

    bro_utils.assert_equal(CMYP.get_periodic_event(
        EVENT_ID, backup_manager=DEFAULT_SCOPE), ("1", "0"))
    # Wait for create backup action to run for scheduled event
    time.sleep(61)
    BRO_CTRL.wait_for_orchestrator_to_be_available()

    # Get the list of actions after scheduled event runs
    actions = BRO_CTRL.get_actions(DEFAULT_SCOPE)

    # Assert all CREATE_BACKUP actions were successful
    for action in actions:
        if action["name"] in ["CREATE_BACKUP"]:
            bro_utils.assert_equal(action["result"], "SUCCESS")

    # Assert all EXPORT actions were successful
    for action in actions:
        if action["name"] in ["EXPORT"]:
            assert action["result"] == "SUCCESS", \
                "Action " + str(action) + " failed"

    # delete the event
    CMYP.delete_periodic_event(
        EVENT_ID, backup_manager=DEFAULT_SCOPE)

    # disable the Scheduler
    CMYP.set_scheduler_configuration(BACKUP_NAMES[1],
                                     backup_manager=DEFAULT_SCOPE,
                                     auto_export="disabled",
                                     sftp_server_name=SFTP_SERVER_NAME)
    CMYP.remove_sftp_server_from_scheduler(backup_manager="DEFAULT")

    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, test_case)
    collect_cmyp_logs(test_case)


def test_remove_sftp_server_cmyp():
    """
    Test to remove sftp server from CYMP
    """

    test_case = "test_remove_sftp_server_cmyp"
    utilprocs.log("Test Case: {}".format(test_case))

    # add two dummy sftp servers
    MODEL.define_sftp_server(DUMMY_SFTP_NAME_1, DUMMY_SFTP_ENDPOINT,
                             DUMMY_SFTP_REMOTE_ADDRESS, DUMMY_SFTP_REMOTE_PORT,
                             DUMMY_SFTP_REMOTE_PATH, DUMMY_SFTP_USERNAME,
                             DUMMY_PUBLIC_KEY, DUMMY_PRIVATE_KEY,
                             DUMMY_HOST_KEY)
    MODEL.verify_sftp_persisted_data_in_brm("create", DUMMY_SFTP_FILENAME_1,
                                            SFTP_SERVER_PATH, 2,
                                            BRO_POD_NAME, BRO_NAME,
                                            NAMESPACE, [DUMMY_SFTP_NAME_1,
                                                        DUMMY_SFTP_ENDPOINT,
                                                        DUMMY_SFTP_REMOTE_PORT,
                                                        DUMMY_SFTP_REMOTE_PATH,
                                                        DUMMY_PUBLIC_KEY])
    MODEL.define_sftp_server(DUMMY_SFTP_NAME_2, DUMMY_SFTP_ENDPOINT,
                             DUMMY_SFTP_REMOTE_ADDRESS, DUMMY_SFTP_REMOTE_PORT,
                             DUMMY_SFTP_REMOTE_PATH, DUMMY_SFTP_USERNAME,
                             DUMMY_PUBLIC_KEY, DUMMY_PRIVATE_KEY,
                             DUMMY_HOST_KEY)
    MODEL.verify_sftp_persisted_data_in_brm("create", DUMMY_SFTP_FILENAME_2,
                                            SFTP_SERVER_PATH, 3,
                                            BRO_POD_NAME, BRO_NAME,
                                            NAMESPACE, [DUMMY_SFTP_NAME_2,
                                                        DUMMY_SFTP_ENDPOINT,
                                                        DUMMY_SFTP_REMOTE_PORT,
                                                        DUMMY_SFTP_REMOTE_PATH,
                                                        DUMMY_PUBLIC_KEY])
    # remove sftp server
    CMYP.remove_sftp_server(SFTP_SERVER_NAME, backup_manager="DEFAULT")

    # Verify that the sftp server data has been removed in the Orchestrator
    MODEL.verify_sftp_persisted_data_in_brm("remove", SFTP_SERVER_FILENAME,
                                            SFTP_SERVER_PATH, 2,
                                            BRO_POD_NAME, BRO_NAME,
                                            NAMESPACE)
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, test_case)


def test_readd_removed_sftp_server_cmyp():
    """
    Test to re-add sftp server which just removed from CYMP.
    """

    test_case = "test_readd_removed_sftp_server_cmyp"
    utilprocs.log("Test Case: {}".format(test_case))

    # Remove dummy sftp-1 server
    CMYP.remove_sftp_server(DUMMY_SFTP_NAME_1, backup_manager="DEFAULT")

    # Verify that the sftp server data has been removed in the Orchestrator
    MODEL.verify_sftp_persisted_data_in_brm("remove", DUMMY_SFTP_FILENAME_1,
                                            SFTP_SERVER_PATH, 1,
                                            BRO_POD_NAME, BRO_NAME,
                                            NAMESPACE)
    # add the dummy sftp-1 server back
    MODEL.define_sftp_server(DUMMY_SFTP_NAME_1, DUMMY_SFTP_ENDPOINT,
                             DUMMY_SFTP_REMOTE_ADDRESS, DUMMY_SFTP_REMOTE_PORT,
                             DUMMY_SFTP_REMOTE_PATH, DUMMY_SFTP_USERNAME,
                             DUMMY_PUBLIC_KEY, DUMMY_PRIVATE_KEY,
                             DUMMY_HOST_KEY)

    # Verify that the sftp-1 server data has created in the Orchestrator
    MODEL.verify_sftp_persisted_data_in_brm("create", DUMMY_SFTP_FILENAME_1,
                                            SFTP_SERVER_PATH, 2,
                                            BRO_POD_NAME, BRO_NAME,
                                            NAMESPACE, [DUMMY_SFTP_NAME_1,
                                                        DUMMY_SFTP_ENDPOINT,
                                                        DUMMY_SFTP_REMOTE_PORT,
                                                        DUMMY_SFTP_REMOTE_PATH,
                                                        DUMMY_PUBLIC_KEY])
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, test_case)
    collect_cmyp_logs(test_case)


def test_create_remove_periodic_event_multi_brm_cmyp():
    """
    Test to create/remove periodic event from CYMP
    * Create 2 dummy periodic events in DEFAULT scope
    * Create 3 dummy periodic events in CONFIGURATION scope
    * one commit to
        add the periodic event-3 to DEFAULT scope
        remove the periodic event-2 to CONFIGURATION scope
    * Verify that the periodic event data has been removed correctly
    in the Orchestrator
    """

    test_case = "test_remove_periodic_event_cmyp"
    utilprocs.log("Test Case: {}".format(test_case))

    # Create 2 dummy periodic events in DEFAULT scope
    CMYP.create_or_update_periodic_event(DUMMY_EVENT_ID_1,
                                         "1", "1", DEFAULT_SCOPE)
    CMYP.create_or_update_periodic_event(DUMMY_EVENT_ID_2,
                                         "2", "2", DEFAULT_SCOPE)
    # Create 3 dummy periodic events in CONFIGURATION scope
    CMYP.create_or_update_periodic_event(DUMMY_EVENT_ID_1,
                                         "1", "1", CONFIGURATION_SCOPE)
    CMYP.create_or_update_periodic_event(DUMMY_EVENT_ID_2,
                                         "2", "2", CONFIGURATION_SCOPE)
    CMYP.create_or_update_periodic_event(DUMMY_EVENT_ID_3,
                                         "3", "3", CONFIGURATION_SCOPE)

    bro_utils.assert_equal(CMYP.get_periodic_event(
        DUMMY_EVENT_ID_1, backup_manager=DEFAULT_SCOPE), ("1", "1"))
    bro_utils.assert_equal(CMYP.get_periodic_event(
        DUMMY_EVENT_ID_2, backup_manager=DEFAULT_SCOPE), ("2", "2"))
    bro_utils.assert_equal(CMYP.get_periodic_event(
        DUMMY_EVENT_ID_1, backup_manager=CONFIGURATION_SCOPE), ("1", "1"))
    bro_utils.assert_equal(CMYP.get_periodic_event(
        DUMMY_EVENT_ID_2, backup_manager=CONFIGURATION_SCOPE), ("2", "2"))
    bro_utils.assert_equal(CMYP.get_periodic_event(
        DUMMY_EVENT_ID_3, backup_manager=CONFIGURATION_SCOPE), ("3", "3"))

    # add the periodic event-3 to DEFAULT scope
    # remove the periodic event-2 to CONFIGURATION scope
    CMYP.create_remove_periodic_event_multi_BRM(
        event_ids=[DUMMY_EVENT_ID_3, DUMMY_EVENT_ID_2],
        operations=["create", "remove"],
        backup_managers=[DEFAULT_SCOPE, CONFIGURATION_SCOPE])

    # Verify that the periodic event data has been
    # removed/added in the Orchestrator
    MODEL.verify_periodic_event_file_in_brm("create", DUMMY_EVENT_FILENAME_3,
                                            PERIODIC_EVENT_PATH, 3,
                                            BRO_POD_NAME, BRO_NAME,
                                            NAMESPACE)
    MODEL.verify_periodic_event_file_in_brm("remove", DUMMY_EVENT_FILENAME_2,
                                            PERIODIC_EVENT_PATH_CONFIG, 2,
                                            BRO_POD_NAME, BRO_NAME,
                                            NAMESPACE)
    # Verify that the periodic event data has been
    # removed/added in the CMYP
    bro_utils.assert_equal(CMYP.get_periodic_event(
        DUMMY_EVENT_ID_3, backup_manager=DEFAULT_SCOPE), ("1", "1"))
    bro_utils.assert_equal(
        CMYP.verify_periodic_event_not_exist_cmyp(
            DUMMY_EVENT_ID_2, backup_manager=CONFIGURATION_SCOPE), True)

    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, test_case)
    collect_cmyp_logs(test_case)


def test_readd_periodic_event_cmyp():
    """
    Test to remove periodic event and add back from CYMP
    * remove the event-3 from DEFAULT & CONFIGURATION scope in one commit
    * Create event-3 dummy periodic events into DEFAULT & CONFIGURATION scope
    in one commit
    * Verify that the periodic event data has been re-add correctly
    """

    test_case = "test_readd_periodic_event_cmyp"
    utilprocs.log("Test Case: {}".format(test_case))

    # remove the event-3 from DEFAULT & CONFIGURATION scope in one commit
    CMYP.delete_periodic_event_multi_BRM(
        DUMMY_EVENT_ID_3,
        backup_managers=[CONFIGURATION_SCOPE, DEFAULT_SCOPE])

    # Verify that the periodic event data has been removed in the Orchestrator
    MODEL.verify_periodic_event_file_in_brm("remove", DUMMY_EVENT_FILENAME_3,
                                            PERIODIC_EVENT_PATH, 1,
                                            BRO_POD_NAME, BRO_NAME,
                                            NAMESPACE)
    MODEL.verify_periodic_event_file_in_brm("remove", DUMMY_EVENT_FILENAME_3,
                                            PERIODIC_EVENT_PATH_CONFIG, 1,
                                            BRO_POD_NAME, BRO_NAME,
                                            NAMESPACE)
    # Verify that the periodic event data has been removed in the CMYP
    bro_utils.assert_equal(
        CMYP.verify_periodic_event_not_exist_cmyp(
            DUMMY_EVENT_ID_3, backup_manager=DEFAULT_SCOPE), True)
    bro_utils.assert_equal(
        CMYP.verify_periodic_event_not_exist_cmyp(
            DUMMY_EVENT_ID_3, backup_manager=CONFIGURATION_SCOPE), True)

    # Create event-3 dummy periodic events into DEFAULT & CONFIGURATION scope
    CMYP.create_or_update_periodic_event_multi_BRM(
        DUMMY_EVENT_ID_3, "3", "3",
        backup_managers=[CONFIGURATION_SCOPE, DEFAULT_SCOPE])

    bro_utils.assert_equal(CMYP.get_periodic_event(
        DUMMY_EVENT_ID_3, backup_manager=DEFAULT_SCOPE), ("3", "3"))
    bro_utils.assert_equal(CMYP.get_periodic_event(
        DUMMY_EVENT_ID_3, backup_manager=CONFIGURATION_SCOPE), ("3", "3"))
    # Verify that the periodic event data has been added in the Orchestrator
    MODEL.verify_periodic_event_file_in_brm("create", DUMMY_EVENT_FILENAME_3,
                                            PERIODIC_EVENT_PATH, 2,
                                            BRO_POD_NAME, BRO_NAME,
                                            NAMESPACE)
    MODEL.verify_periodic_event_file_in_brm("create", DUMMY_EVENT_FILENAME_3,
                                            PERIODIC_EVENT_PATH_CONFIG, 2,
                                            BRO_POD_NAME, BRO_NAME,
                                            NAMESPACE)
    # Verify that the periodic event data has been added in the CMYP
    bro_utils.assert_equal(CMYP.get_periodic_event(
        DUMMY_EVENT_ID_3, backup_manager=DEFAULT_SCOPE), ("3", "3"))
    bro_utils.assert_equal(CMYP.get_periodic_event(
        DUMMY_EVENT_ID_3, backup_manager=CONFIGURATION_SCOPE), ("3", "3"))

    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, test_case)
    collect_cmyp_logs(test_case)


def test_remove_all_periodic_event_multi_brm_cmyp():
    """
    Test to remove all periodic event(s) from CYMP
    * remove all existing periodic event(s) in DEFAULT &
    CONFIGURATION scope in one commit
    * Verify that all periodic event data has been removed correctly
    """

    test_case = "test_remove_all_periodic_event_cmyp"
    utilprocs.log("Test Case: {}".format(test_case))

    # remove all periodic event from DEFAULT & CONFIGURATION scope
    CMYP.delete_all_periodic_event_multi_BRM(
        backup_managers=[CONFIGURATION_SCOPE, DEFAULT_SCOPE])

    # Verify that all periodic event data has been
    # removed in the Orchestrator
    MODEL.verify_periodic_event_file_in_brm("clear", DUMMY_EVENT_FILENAME_3,
                                            PERIODIC_EVENT_PATH, 0,
                                            BRO_POD_NAME, BRO_NAME,
                                            NAMESPACE)
    MODEL.verify_periodic_event_file_in_brm("clear", DUMMY_EVENT_FILENAME_3,
                                            PERIODIC_EVENT_PATH_CONFIG, 0,
                                            BRO_POD_NAME, BRO_NAME,
                                            NAMESPACE)
    # Verify that all periodic event data has been added in the CMYP
    bro_utils.assert_equal(
        CMYP.verify_no_periodic_event_exist_cmyp(
            backup_manager=DEFAULT_SCOPE), True)
    bro_utils.assert_equal(
        CMYP.verify_no_periodic_event_exist_cmyp(
            backup_manager=CONFIGURATION_SCOPE), True)

    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, test_case)
    collect_cmyp_logs(test_case)


def test_restore_backup_configuration():
    """
    Test to restore backup manager configuration
    """
    test_case = "test_restore_backup_configuration"
    utilprocs.log("Test Case: {}".format(test_case))

    progress_report_id = CMYP.restore_backup(
        backup_name=BACKUP_NAMES[0], backup_manager="DEFAULT-bro")

    wait_until_bro_cmyp_ready_after_action(
        progress_report_id, "RESTORE", scope="DEFAULT-bro",
        backup_name=BACKUP_NAMES[0])
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, test_case)
    collect_cmyp_logs(test_case)
    MODEL.verify_sftp_persisted_data_in_brm("remove", SFTP_SERVER_FILENAME,
                                            SFTP_SERVER_PATH, 2,
                                            BRO_POD_NAME, BRO_NAME,
                                            NAMESPACE)


def test_readd_sftp_server_in_bro():
    """
    Re-adds the sftp server in BRO
    """
    test_case = "test_readd_sftp_server_pods_in_bro"
    utilprocs.log("Test Case: {}".format(test_case))
    # Get the IP address of the SFTP server
    pods = bro_utils.get_pods_by_prefix(NAMESPACE, TEST_POD_PREFIX)
    utilprocs.log(pods)
    test_pod = pods[0]
    keys = SFTP_SERVER.generate_ssh_keys(filename="rsakeypair",
                                         pod=test_pod,
                                         namespace=NAMESPACE)
    sftp_remote_address = SFTP_SERVER.get_sftp_ipaddress(
        namespace=NAMESPACE)
    sftp_host_key = SFTP_SERVER.get_host_key_from_sftp_server(
        SFTP_SERVER_NAME, NAMESPACE)
    # Defines an SFTP Server in BRO
    MODEL.define_sftp_server(SFTP_SERVER_NAME, SFTP_ENDPOINT,
                             sftp_remote_address, SFTP_REMOTE_PORT,
                             SFTP_REMOTE_PATH, SFTP_USERNAME,
                             keys[1], keys[3],
                             sftp_host_key)
    MODEL.verify_sftp_persisted_data_in_brm("create", SFTP_SERVER_FILENAME,
                                            SFTP_SERVER_PATH, 3,
                                            BRO_POD_NAME, BRO_NAME,
                                            NAMESPACE, [SFTP_SERVER_NAME,
                                                        SFTP_ENDPOINT,
                                                        SFTP_REMOTE_PORT,
                                                        SFTP_REMOTE_PATH,
                                                        keys[1]])
    MODEL.verify_sftp_server_on_cmyp(SFTP_SERVER_NAME, SFTP_ENDPOINT,
                                     SFTP_REMOTE_PORT, SFTP_REMOTE_PATH,
                                     SFTP_USERNAME, keys[1],
                                     sftp_host_key,
                                     backup_manager=DEFAULT_SCOPE)

    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, test_case)
    KUBE.get_pod_logs(NAMESPACE, SFTP_SERVER_NAME, test_case)


def test_remove_k8s_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    utilprocs.log("Test Case: test_remove_k8s_resources")

    # Close the CMYP connection
    CMYP.close()

    # Close the second CMYP connection
    CMYP2.close()

    # stop sftp server
    SFTP_SERVER.stop_sftp_server()

    bro_utils.remove_namespace_resources(NAMESPACE)
    utilprocs.log("Finished removing Kubernetes resources")


def collect_cmyp_logs(test_case_name=""):
    """
    Collection of specific cmyp pod container logs ,
    as defined by global CONTAINER_NAMES parameter
    """
    bro_utils.get_cmyp_pod_logs(
        TEST_POD_PREFIX,
        NAMESPACE,
        test_case_name,
        CONTAINER_NAMES
    )


def wait_until_bro_cmyp_ready_after_action(progress_report_id,
                                           action_name,
                                           scope="DEFAULT",
                                           expected_result="SUCCESS",
                                           backup_name=None):
    """
    wait until both BRO and CMYP is ready
    :param progress_report_id : the id of progress report
    :param action_name : the name of action
    :param scope : the BRM
    :param expected_result : the expected result
    :param backup_name : the name of backup
    """
    BRO_CTRL.wait_for_action_complete(
        action_id=progress_report_id,
        action_type=action_name,
        scope=scope,
        expected_result=expected_result,
        backup_name=backup_name
    )
    time.sleep(10)
    CMYP.wait_until_progress_report_is_complete(
        progress_report_id=progress_report_id,
        action_name=action_name,
        scope=scope,
        expected_result=expected_result,
        backup_name=backup_name
    )
