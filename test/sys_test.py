#!/usr/bin/env python3
"""
This module provides test cases to verify the Backup and Restore Orchestrator
features (Kafka Notification and Metrics) and also verifies
integration with real BR Agents
"""
import os
import time
import json
from utilprocs import log
import k8sclient
import helm3procs
import bro_ctrl
# import bro_db
import bro_utils
from bro_utils import assert_equal
import bro_kafka_utils
import bro_redis_utils
import bro_cmyp_cli_client
import bro_search_engine_client
import bro_sftp
import rest_utils
import sys_cm
import bro_pm
from globals import (V4_FRAG_CM,
                     V4_FRAG_CM_NAME,
                     TLS_AGENT,
                     DDPG_AGENT)
# snapshot repo
INT_SNAP_REPO = os.environ.get("helm_repo")
# chart_version
INT_SNAP_VERSION = os.environ.get('baseline_chart_version')

INT_SNAP_REPO_NAME = "int_snap"

NAMESPACE = os.environ.get('kubernetes_namespace')

# Set BRO variables
INT_CHART_NAME = "eric-bro-int"

# test service helm repo
TEST_REPO = os.environ.get("test_helm_repo")
TEST_CHART_NAME = "eric-test-service-basic"
TEST_REPO_NAME = "test_svc"
TEST_CHART_VERSION = ""

# scope
DEFAULT_SCOPE = "DEFAULT"
CONFIGURATION_SCOPE = "configuration-data"
# the vBRM used in vBRM tests

# set integration chart deployment timeout
DEPLOY_TIMEOUT = 900

BRO_POD_NAME = "eric-ctrl-bro-0"

KUBE = k8sclient.KubernetesClient()
BRO_CTRL = bro_ctrl.BroCtrlClient()

HELM_RELEASE_NAME = bro_utils.get_service_helm_release_name(
    INT_CHART_NAME, NAMESPACE)

VBRM_SCOPE = "DEFAULT-" + V4_FRAG_CM_NAME

# AGENTS = [DDPG_AGENT, KVDB_AGENT]
AGENTS = [V4_FRAG_CM, DDPG_AGENT, TLS_AGENT]

# Define Backup Name
BACKUP_NAME_LIST = ["backup_1", "backup_2"]
BACKUPS = ["bu1", "bu2", "bu3", "bu4", "bu5"]

# Define number of records to insert_delete on test
NUMBER_RECORDS = 10

NUM_EXPECTED_METRICS = 82

# Kafka details
KAFKA_SERVICE_NAME = 'eric-data-message-bus-kf'
BRO_TOPIC = 'bro-notification'
KAFKA_CONSUMER = None

# Redis details
REDIS_CLIENT = None

# Define CMYP CLI data
CMYP_USER = "brocmyp"
CMYP_PSW = "TeamPlanBYang$1"
CMYP = bro_cmyp_cli_client.CMYPCliClient(CMYP_USER, CMYP_PSW, BRO_CTRL)

# Define Search Engine data
SEARCH_ENGINE = bro_search_engine_client.SearchEngineClient()

# SFTP Server USER and password
SFTP_USER = "brsftp"
SFTP_PASSWORD = "planbsftp"
SFTP_PATH = "bro_test/1/3/"

SFTP_SERVER = bro_sftp.SFTPServer(SFTP_USER, SFTP_PASSWORD)

TLS_TEST_CHART_VERSION = "2.5.0-0"

# Periodic event id
EVENT_ID = "id1"

EVENT_ID_2 = "id2"

EVENT_ID_3 = "id3"

# PM metric validator
METRIC_VALIDATOR = bro_pm.PMClient()

# BRO configmap name
BRO_PRODUCT_CONFIGMAP = "product-info-conf"

GENERAL_SETTINGS_DICT = {
    "global.pullSecret": "armdocker",
    "eric-ctrl-bro.bro.vBRMAutoCreate": "DEFAULT",
    "eric-pm-server.clusterRoleName":
        "bro-intchart-monitoring-{}".format(NAMESPACE),
    "tags.logging": "false"
}
SEMANTIC_SETTINGS_DICT = {
    "eric-ctrl-bro.bro.selectedMatchType": "SEMVER",
    "eric-ctrl-bro.bro.semVerMatchType": "GREATER_THAN",
    "eric-ctrl-bro.bro.productLowestAllowedVersion": "",
    "eric-ctrl-bro.bro.appProductInfoConfigMap": BRO_PRODUCT_CONFIGMAP,
}


# Test Cases
def test_clear_any_failed_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    log("Test Case: test_clear_any_failed_resources")
    bro_utils.remove_namespace_resources(NAMESPACE)
    log("Finished removing Kubernetes resources")


def test_deploy_integration_chart():
    """
    Deploys BRO Integration Helm Chart.
    """
    test_case = "test_deploy_integration_chart"
    log("Test Case: {}".format(test_case))

    log("Add the Int Snapshot helm repo {0} - {1}".format(INT_SNAP_REPO_NAME,
                                                          INT_SNAP_REPO))
    helm3procs.add_helm_repo(INT_SNAP_REPO, INT_SNAP_REPO_NAME)

    settings = {
        "tags.logging": "true",
        "eric-ctrl-bro.log.streamingMethod": "dual"
    }
    # Install Integration Chart
    bro_utils.install_service_chart(INT_SNAP_REPO_NAME, INT_CHART_NAME,
                                    INT_SNAP_VERSION, NAMESPACE,
                                    DEPLOY_TIMEOUT,
                                    settings=settings)

    # Starts the sftp server, doesn't wait for the service to be up and running
    SFTP_SERVER.start_sftp_server()

    # Verify that the Orchestrator has all expected agents registered
    # While real agents are disabled we do not expect any at this point
    try:
        BRO_CTRL.wait_for_bro_agents_to_reconnect([DDPG_AGENT["AgentId"]])

        # Verify PM metrics initial values
        """
        3 metrics were not validated due to arbitrary original value
        -bro_volume_stats_available_bytes
        -bro_volume_stats_capacity_bytes
        -bro_volume_stats_used_bytes
        """
        METRIC_VALIDATOR.validate_specific_metric(
            "0", "bro_granular_operations_total")
        METRIC_VALIDATOR.validate_specific_metric("0", "bro_operations_total")
        METRIC_VALIDATOR.validate_specific_metric("0", "bro_stored_backups")
        METRIC_VALIDATOR.validate_specific_metric(
            "0", "bro_scheduled_operation_error")
        METRIC_VALIDATOR.validate_specific_metric(
            "0", "bro_scheduled_backup_missed_total")
        METRIC_VALIDATOR.validate_specific_metric(
            "0", "bro_granular_end_time")
        METRIC_VALIDATOR.validate_specific_metric(
            "0", "bro_granular_stage_duration_seconds")
        METRIC_VALIDATOR.validate_specific_metric(
            "0", "bro_operation_end_time")
        METRIC_VALIDATOR.validate_specific_metric(
            "0", "bro_operation_stage_duration_seconds")
        METRIC_VALIDATOR.validate_specific_metric(
            "0", "bro_operation_transferred_bytes")
        METRIC_VALIDATOR.validate_specific_metric(
            "0",
            "bro_granular_stage_info"
        )
        METRIC_VALIDATOR.validate_specific_metric("0", "bro_operation_info")
        METRIC_VALIDATOR.validate_bro_registered_agents(
            ["eric-data-document-database-pg"]
        )
        METRIC_VALIDATOR.validate_disk_usage_bytes([
            {"BRM_id": "DEFAULT", "Value": "0"},
            {"BRM_id": "DEFAULT-bro", "Value": "0"},
            {"BRM_id": "configuration-data", "Value": "0"},
            {"BRM_id": "configuration-data-bro", "Value": "0"}])
    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_deploy_test_agent():
    """
    Deploys Test Agent Helm Chart.
    """
    test_case = "test_deploy_test_agent"
    log("Test Case: {}".format(test_case))

    log("Add the test service helm repo")
    global TEST_REPO
    global TEST_CHART_VERSION
    helm3procs.add_helm_repo(TEST_REPO, TEST_REPO_NAME)

    TEST_CHART_VERSION = \
        helm3procs.get_latest_chart_version(TEST_CHART_NAME,
                                            helm_repo_name=TEST_REPO_NAME,
                                            development_version=True)
    # Deploy latest test agent
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION,
                                 [V4_FRAG_CM["AgentId"]], NAMESPACE,
                                 agent_log_level="debug",
                                 root_log_level="debug")

    # Deploy a TLS test agent
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TLS_TEST_CHART_VERSION,
                                 [TLS_AGENT["AgentId"]], NAMESPACE,
                                 agent_log_level="debug",
                                 root_log_level="debug")
    try:
        # Verify that Orchestrator has all expected agents registered
        BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())

        # Verify the BRM configuration has been pushed to CM
        prepare_cmyp()
        time.sleep(10)
        CMYP.verify_brm_config()

        assert_equal(CMYP.get_housekeeping_configuration(
            backup_manager=CONFIGURATION_SCOPE), ("1", "enabled"))

        assert_equal(CMYP.get_scheduler_configuration(
            backup_manager=CONFIGURATION_SCOPE),
                     ("unlocked", "disabled", "SCHEDULED_BACKUP", ""))

        assert_equal(CMYP.get_housekeeping_configuration(
            backup_manager=DEFAULT_SCOPE), ("1", "enabled"))

        assert_equal(CMYP.get_scheduler_configuration(
            backup_manager=DEFAULT_SCOPE),
                     ("unlocked", "disabled", "SCHEDULED_BACKUP", ""))

        METRIC_VALIDATOR.validate_bro_registered_agents(
            [V4_FRAG_CM["AgentId"], DDPG_AGENT["AgentId"],
             TLS_AGENT["AgentId"]])
    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_restore_configuration():
    """
    In this test we will test restoring backup manager DEFAULT configuration by
    1. Creating a backup to capture the default configuration
    2. Perform updates to housekeeping, scheduling and create a periodic event
    3. Restore backup using vbrm DEFAULT-bro
    """
    test_case = "test_restore_configuration"
    log("Test Case: {}".format(test_case))
    global KAFKA_CONSUMER
    # Create a KafkaConsumer Instance
    KAFKA_CONSUMER = bro_kafka_utils.BroKafkaConsumer(
        KAFKA_SERVICE_NAME, BRO_TOPIC)

    global REDIS_CLIENT
    REDIS_CLIENT = bro_redis_utils.RedisClient(BRO_TOPIC, NAMESPACE)
    try:
        backup_name = "for-restore"
        time.sleep(1)
        event_id = CMYP.create_backup(
            backup_name, backup_manager=DEFAULT_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            event_id, "CREATE_BACKUP", scope=DEFAULT_SCOPE,
            backup_name=backup_name, agents=AGENTS)

        # Check that the backup status is updated to "backup-complete" in vbrm
        get_attribute = CMYP.get_backup_status(backup_name,
                                               DEFAULT_SCOPE + "-bro")
        assert_equal(get_attribute, "backup-complete")

        # Verify backup notifications
        bro_kafka_utils.validate_notifications(
            KAFKA_CONSUMER, event_id, DEFAULT_SCOPE,
            "CREATE_BACKUP")
        REDIS_CLIENT.validate_notification(
            event_id, DEFAULT_SCOPE, "CREATE_BACKUP")

        # Update configuration
        CMYP.config_commands([CMYP.get_housekeeping_configuration_command(
            "enabled", "3")])
        BRO_CTRL.wait_for_orchestrator_to_be_available()

        dummy_sftp_uri = "sftp://test@192.168.153.116:22/bro_tests"
        CMYP.set_scheduler_configuration("SCHEDULED", admin_state="locked",
                                         auto_export="enabled",
                                         backup_manager=DEFAULT_SCOPE,
                                         auto_export_uri=dummy_sftp_uri,
                                         auto_export_password=SFTP_PASSWORD)

        CMYP.create_or_update_periodic_event(EVENT_ID, "30", hours="1",
                                             backup_manager=DEFAULT_SCOPE)
        # Adding sleep to make sure the restore
        # doesn't run before all the configurations have finished updating
        time.sleep(5)

        assert_equal(CMYP.get_housekeeping_configuration(
            backup_manager=DEFAULT_SCOPE), ("3", "enabled"))

        assert_equal(CMYP.get_scheduler_configuration(
            backup_manager=DEFAULT_SCOPE),
                     ("locked", "enabled", "SCHEDULED", dummy_sftp_uri))

        assert_equal(CMYP.get_periodic_event(
            EVENT_ID, backup_manager=DEFAULT_SCOPE), ("30", "1"))

        # Restore backup under DEFAULT-bro
        progress_report_id = CMYP.restore_backup(
            backup_name, backup_manager=DEFAULT_SCOPE + "-bro")

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id,
            "RESTORE",
            expected_result="SUCCESS",
            backup_name=backup_name,
            scope=DEFAULT_SCOPE + "-bro",
            agents=AGENTS)

        # Check the configurations have been restored
        assert_equal(CMYP.get_housekeeping_configuration(
            backup_manager=DEFAULT_SCOPE), ("1", "enabled"))

        assert_equal(CMYP.get_scheduler_configuration(
            backup_manager=DEFAULT_SCOPE),
                     ("unlocked", "disabled", "SCHEDULED_BACKUP", ""))

        response = BRO_CTRL.get_scheduled_periodic_event(EVENT_ID)
        assert response["statusCode"] == 404, \
            "Periodic event " + event_id + " was not removed after restore"

        # After the test， delete the backup for-restore
        progress_report_id = CMYP.delete_backup(
            backup_name, backup_manager=DEFAULT_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id, "DELETE_BACKUP", scope=DEFAULT_SCOPE,
            backup_name=backup_name, agents=AGENTS)
    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_verify_logs_in_search_engine():
    """
    This test case verifies that backup operation log is present in Search
    Engine and checks if BRO initialization log is present.
    The initialization
    log is not asserted as it does not currently reliably appear in SE.
    """
    test_case = "test_verify_logs_in_search_engine"
    log("Test Case: {}".format(test_case))

    try:
        retrieved_logs_dict = \
            SEARCH_ENGINE.retrieve_logs_from_service("eric-ctrl-bro", 10000)

        expected_init_log_found = check_log_is_present(
            retrieved_logs_dict, ["Initialization finished"])
        expected_backup_log_found = check_log_is_present(
            retrieved_logs_dict, ["CREATE_BACKUP", "COMPLETED"])

        # If the two logs aren't found in the first 10,000 logs, do a wider
        # search.
        if not (expected_init_log_found and expected_backup_log_found):
            log("logs not found in first 10,000 - extending search")
            retrieved_logs_dict = SEARCH_ENGINE.\
                retrieve_logs_from_service_scroll("eric-ctrl-bro")

            if not expected_init_log_found:
                expected_init_log_found = check_log_is_present(
                    retrieved_logs_dict, ["Initialization finished"])

            if not expected_backup_log_found:
                expected_backup_log_found = check_log_is_present(
                    retrieved_logs_dict, ["CREATE_BACKUP", "COMPLETED"])

        # The BRO "Initialization finished" log is often
        # not being received in SE due to a known issue.
        # As such, the test does not assert that it is found.
        log("Is expected initialization log found: {}"
            .format(expected_init_log_found))

        assert expected_backup_log_found,\
            "Expected backup log not found in retrieved logs \n: {}".format(
                json.dumps(retrieved_logs_dict['hits']['hits'], indent=4))
    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


# This test case is temporarily removed until full security towards CMM has
# been implemented. It is likely not needed going forward but can be
# when security is in place.
# def test_cm_configuration():
#    """
#    Deletes the CM Configuration and verifies it gets re-added
#    after an update.
#    """
#    sys_cm.delete_bro_configuration()
#
#   BRO_CTRL.update_backup_manager({"backupType": "ConfigTest"})
#
#    CMYP.verify_brm_config()
#    assert CMYP.get_housekeeping_configuration(
#        backup_manager=CONFIGURATION_SCOPE) == ("1", "enabled"), \
#        "Unexpected housekeeping configuration"


def test_set_housekeeping_max_backups():
    """
    Changes the value of the max number of backups that can be stored under
    a backup manager and verifies the change was applied.
    """
    test_case = "test_set_housekeeping_max_backups"
    log("Test Case: {}".format(test_case))

    CMYP.verify_brm_config()

    try:
        commands = []
        commands.append(CMYP.get_housekeeping_configuration_command(
            "enabled", "3"))
        commands.append(CMYP.get_housekeeping_configuration_command(
            "enabled", "2", backup_manager=CONFIGURATION_SCOPE))
        # excute the above commands in a single commit
        CMYP.config_commands(commands)

        BRO_CTRL.wait_for_orchestrator_to_be_available()
        assert_equal(CMYP.get_housekeeping_configuration(), ("3", "enabled"))

        assert_equal(CMYP.get_housekeeping_configuration(
            backup_manager=CONFIGURATION_SCOPE), ("2", "enabled"))
    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_create_backup_cmyp():
    """
     Creates a backup and verifies its presence
     in the Orchestrator and CMYP.
    """
    test_case = "test_create_backup_cmyp"
    log("Test Case: {}".format(test_case))

    try:
        # Prepare a temporary database/table to backup
        # Inserting $NUMBER_RECORDS random records
        # bro_db.setup_pg_db(NUMBER_RECORDS)

        # populate kvdb with some data
        # sys_kvdb.setup_kvdb(NAMESPACE)

        # Populate CM with test data
        sys_cm.create_test_data()

        # Create backup
        progress_report_id = CMYP.create_backup(
            BACKUP_NAME_LIST[0], backup_manager=CONFIGURATION_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id, "CREATE_BACKUP", scope=CONFIGURATION_SCOPE,
            backup_name=BACKUP_NAME_LIST[0], agents=AGENTS)

        # Once the backup is performed, clean the database
        # bro_db.clean_pg_db()
        # Delete few records from kvdb
        # sys_kvdb.delete_data()

        # Delete test data from CM
        sys_cm.remove_test_data()

        bro_kafka_utils.validate_notifications(
            KAFKA_CONSUMER, progress_report_id, CONFIGURATION_SCOPE,
            "CREATE_BACKUP")
        REDIS_CLIENT.validate_notification(
            progress_report_id, CONFIGURATION_SCOPE, "CREATE_BACKUP")

        assert CMYP.backup_manager_has_backup(
            BACKUP_NAME_LIST[0], backup_manager=CONFIGURATION_SCOPE), \
            "Backup {} was not found in the backup list".format(
                BACKUP_NAME_LIST[0])

        assert_equal(CMYP.get_backup_status(
            BACKUP_NAME_LIST[0], backup_manager=CONFIGURATION_SCOPE
        ), "backup-complete")

        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1", "bro_stored_backups", "backup_type", CONFIGURATION_SCOPE)
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "0", "bro_stored_backups", "backup_type", DEFAULT_SCOPE)
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "0", "bro_stored_backups", "backup_type", DEFAULT_SCOPE + "-bro")

        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            3, "bro_operations_total", "backup_type", CONFIGURATION_SCOPE)
        METRIC_VALIDATOR.validate_bro_granular_stage_info_for_action_type(
            "9", "CREATE_BACKUP")
        METRIC_VALIDATOR.validate_bro_disk_usage_bytes_for_specified_brm(
            CONFIGURATION_SCOPE)

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_export_backup_cmyp():
    """
    This test uses a single backup and export the
    content into an external SFTP server.
    """
    test_case = "test_export_backup_cmyp"
    log("Test Case: {}".format(test_case))

    try:
        # Waits until the ftp server is up and running
        SFTP_SERVER.wait_to_start()

        uri = SFTP_SERVER.get_sftp_uri_for_path(SFTP_PATH)

        log("Exporting {} from {}".format(BACKUP_NAME_LIST[0], uri))

        progress_report_id = CMYP.export_backup(
            BACKUP_NAME_LIST[0], uri, SFTP_PASSWORD,
            backup_manager=CONFIGURATION_SCOPE)

        # wait_until_bro_cmyp_ready_after_action(
        #     progress_report_id, "EXPORT", backup_name=BACKUP_NAME_LIST[0],
        #     scope=CONFIGURATION_SCOPE)
        # Would use wait_until_bro_cmyp_ready_after_action but need to carry
        # out the check for progress percetnage while the action is running
        CMYP.wait_until_progress_report_is_complete(
            progress_report_id, 'EXPORT', scope=CONFIGURATION_SCOPE,
            backup_name=BACKUP_NAME_LIST[0],
            agents=AGENTS
        )

        # Allow time for PM to update
        time.sleep(3)
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1", "bro_operations_total", "action", "EXPORT")
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1", "bro_operation_info", "action", "EXPORT")

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_create_second_backup_cmyp():
    """
     Creates a second backup and verifies its presence
     in the Orchestrator and CMYP.
     Updates the housekeeping configuration to set auto-delete to disabled
     and to set max-stored-manual-backups to 1, thus deleting oldest backup.
    """
    test_case = "test_create_second_backup_cmyp"
    log("Test Case: {}".format(test_case))

    try:

        # Create backup
        progress_report_id = CMYP.create_backup(
            BACKUP_NAME_LIST[1], backup_manager=CONFIGURATION_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id, "CREATE_BACKUP", scope=CONFIGURATION_SCOPE,
            backup_name=BACKUP_NAME_LIST[1], agents=AGENTS)

        bro_kafka_utils.validate_notifications(
            KAFKA_CONSUMER, progress_report_id, CONFIGURATION_SCOPE,
            "CREATE_BACKUP")
        REDIS_CLIENT.validate_notification(
            progress_report_id, CONFIGURATION_SCOPE, "CREATE_BACKUP")

        assert CMYP.backup_manager_has_backup(
            BACKUP_NAME_LIST[1], backup_manager=CONFIGURATION_SCOPE), \
            "Backup {} was not found in the backup list".format(
                BACKUP_NAME_LIST[1])

        assert_equal(CMYP.get_backup_status(
            BACKUP_NAME_LIST[1], backup_manager=CONFIGURATION_SCOPE
        ), "backup-complete")

        # Verify PM metrics
        time.sleep(3)
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "2", "bro_stored_backups", "backup_type", CONFIGURATION_SCOPE)
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "0", "bro_stored_backups", "backup_type", DEFAULT_SCOPE)
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "0", "bro_stored_backups", "backup_type", DEFAULT_SCOPE + "-bro")

        # Update housekeeping config to set max-backups to 1
        # and auto-delete to disabled
        CMYP.set_housekeeping_configuration("disabled", "1",
                                            backup_manager=CONFIGURATION_SCOPE)
        BRO_CTRL.wait_for_orchestrator_to_be_available()
        assert_equal(CMYP.get_housekeeping_configuration(
            backup_manager=CONFIGURATION_SCOPE), ("1", "disabled"))

        # Verify PM metrics
        # 3 seconds sometimes are not enough for the
        # HOUSEKEEPING_DELETE_BACKUP action and updating the metrics
        time.sleep(5)
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1", "bro_stored_backups", "backup_type", CONFIGURATION_SCOPE)

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_fail_import_backup_action_on_limit_reached():
    """
    Tries to import a backup with housekeeping disabled and max backups reached
    Results in failed import action.
    Updates the housekeeping configuration to set auto-delete enabled.
    """
    test_case = "test_fail_import_backup_action_on_limit_reached"
    log("Test Case: {}".format(test_case))

    try:
        log("Import {}".format(BACKUP_NAME_LIST[0]))
        path = "{}{}/{}".format(SFTP_PATH, CONFIGURATION_SCOPE,
                                BACKUP_NAME_LIST[0])
        uri = SFTP_SERVER.get_sftp_uri_for_path(path)

        log("Importing {} from {}".format(BACKUP_NAME_LIST[0], uri))

        progress_report_id = CMYP.import_backup(
            uri, SFTP_PASSWORD, backup_manager=CONFIGURATION_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id, "IMPORT", scope=CONFIGURATION_SCOPE,
            expected_result="FAILURE", backup_name=BACKUP_NAME_LIST[0],
            agents=AGENTS)

        assert CMYP.backup_manager_has_backup(
            BACKUP_NAME_LIST[0],
            backup_manager=CONFIGURATION_SCOPE) is False, \
            "Backup {} has been imported.".format(BACKUP_NAME_LIST[0])

        # Update housekeeping config to set auto-delete to enabled
        CMYP.set_housekeeping_configuration("enabled", "1",
                                            backup_manager=CONFIGURATION_SCOPE)
        BRO_CTRL.wait_for_orchestrator_to_be_available()

        assert_equal(CMYP.get_housekeeping_configuration(
            backup_manager=CONFIGURATION_SCOPE), ("1", "enabled"))

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_import_backup_cmyp():
    """
    Imports a backup from a sftp server.
    """
    test_case = "test_import_backup_cmyp"
    log("Test Case: {}".format(test_case))
    log("Import {}".format(BACKUP_NAME_LIST[0]))

    try:
        path = "{}{}/{}".format(SFTP_PATH, CONFIGURATION_SCOPE,
                                BACKUP_NAME_LIST[0])
        uri = SFTP_SERVER.get_sftp_uri_for_path(path)

        log("Importing {} from {}".format(BACKUP_NAME_LIST[0], uri))

        progress_report_id = CMYP.import_backup(
            uri, SFTP_PASSWORD, backup_manager=CONFIGURATION_SCOPE)

        # wait_until_bro_cmyp_ready_after_action(
        #     progress_report_id, "IMPORT", scope=CONFIGURATION_SCOPE,
        #     backup_name=BACKUP_NAME_LIST[0])

        # Would use wait_until_bro_cmyp_ready_after_action but need to carry
        # out the check for progress percetnage while the action is running
        CMYP.wait_until_progress_report_is_complete(
            progress_report_id, 'IMPORT', scope=CONFIGURATION_SCOPE,
            backup_name=BACKUP_NAME_LIST[0],
            agents=AGENTS
        )

        assert CMYP.backup_manager_has_backup(
            BACKUP_NAME_LIST[0], backup_manager=CONFIGURATION_SCOPE), \
            "Backup {} has not been imported.".format(BACKUP_NAME_LIST[0])

        # Allow time for PM to update
        time.sleep(3)
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "2", "bro_operations_total", "action", "IMPORT")
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1", "bro_operation_info", "action", "IMPORT")

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_restore_backup_cmyp():
    """
    This test restores a backup through CMYP 3 times
    """
    test_case = "test_restore_backup_cmyp"
    log("Test Case: {}".format(test_case))

    # Update housekeeping config to set max-backups to 5
    # and auto-delete disabled
    CMYP.set_housekeeping_configuration("disabled", "5",
                                        backup_manager=CONFIGURATION_SCOPE)
    BRO_CTRL.wait_for_orchestrator_to_be_available()

    number_of_restores = 3
    for count in range(number_of_restores):
        try:
            # Restore backups
            log("Executing restore {} of {}"
                .format(count + 1, number_of_restores))
            progress_report_id = CMYP.restore_backup(
                BACKUP_NAME_LIST[0], backup_manager=CONFIGURATION_SCOPE)

            wait_until_bro_cmyp_ready_after_action(
                progress_report_id,
                "RESTORE",
                backup_name=BACKUP_NAME_LIST[0],
                scope=CONFIGURATION_SCOPE,
                agents=AGENTS)
            # Need to reconnect after the restore process
            # bro_db.setup()
            # bro_db.validate_db(NUMBER_RECORDS)
            # bro_db.teardown_pg_db()
            # Verify data in kvdb after restore
            # sys_kvdb.verify_data()
            # Verify the test data in CM
            sys_cm.verify_test_data()

            bro_kafka_utils.validate_notifications(
                KAFKA_CONSUMER, progress_report_id, CONFIGURATION_SCOPE,
                "RESTORE")
            REDIS_CLIENT.validate_notification(
                progress_report_id, CONFIGURATION_SCOPE, "RESTORE")

            # Allow time for cmyp schema synchronizer container to come up
            time.sleep(30)

            # Check backup limit remains same after restore
            assert_equal(CMYP.get_housekeeping_configuration(
                backup_manager=CONFIGURATION_SCOPE), ("5", "disabled"))

            get_all_pod_logs(test_case)

        except Exception:
            get_all_pod_logs(test_case)
            raise

    # Verify PM metrics
    time.sleep(5)
    METRIC_VALIDATOR.validate_bro_granular_stage_info_for_action_type(
        "9", "RESTORE")
    METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
        4, "bro_operations_total", "action", "RESTORE")

    get_all_pod_logs(test_case)


def test_upgrade_bro_exact_match_for_product_match_type_restore():
    """
    This test upgrades the BRO chart with productmatchtype as EXACT_MATCH
    and verfies if restore is successful
    """
    test_case = "test_upgrade_bro_exact_match_for_product_match_type_restore"
    log("Test Case: {}".format(test_case))

    try:
        # Take a note of the metric numbers before upgrade
        configuration_scope = \
            METRIC_VALIDATOR.get_specific_metric_for_specified_label(
                "bro_operations_total", "backup_type", CONFIGURATION_SCOPE)
        default = \
            METRIC_VALIDATOR.get_specific_metric_for_specified_label(
                "bro_operations_total", "backup_type", DEFAULT_SCOPE)
        default_bro = \
            METRIC_VALIDATOR.get_specific_metric_for_specified_label(
                "bro_operations_total", "backup_type", DEFAULT_SCOPE + "-bro")

        log("Upgrade BRO property productMatchType to EXACT_MATCH")
        helm3procs.helm_upgrade_with_chart_repo_with_dict(
            HELM_RELEASE_NAME,
            INT_CHART_NAME,
            NAMESPACE,
            INT_SNAP_REPO_NAME,
            INT_SNAP_VERSION,
            settings_dict={
                "global.pullSecret": "armdocker",
                "eric-ctrl-bro.bro.productMatchType": "EXACT_MATCH",
                "eric-ctrl-bro.bro.vBRMAutoCreate": "DEFAULT",
                "eric-pm-server.clusterRoleName":
                "bro-intchart-monitoring-{}".format(NAMESPACE)},
            timeout=DEPLOY_TIMEOUT)

        bro_utils.wait_for_resources_to_deploy(HELM_RELEASE_NAME, NAMESPACE)

        # Verify Orchestrator status and check registered agents
        BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())

        configuration_scope_2 = \
            METRIC_VALIDATOR.get_specific_metric_for_specified_label(
                "bro_operations_total", "backup_type", CONFIGURATION_SCOPE)
        default_2 = \
            METRIC_VALIDATOR.get_specific_metric_for_specified_label(
                "bro_operations_total", "backup_type", DEFAULT_SCOPE)
        default_bro_2 = \
            METRIC_VALIDATOR.get_specific_metric_for_specified_label(
                "bro_operations_total", "backup_type", DEFAULT_SCOPE + "-bro")

        # Assert the metrics are the same after upgrade
        assert_equal(configuration_scope_2, configuration_scope)
        assert_equal(default_2, default)
        assert_equal(default_bro_2, default_bro)

        # Restore backups
        progress_report_id = CMYP.restore_backup(
            BACKUP_NAME_LIST[0], backup_manager=CONFIGURATION_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id,
            "RESTORE",
            backup_name=BACKUP_NAME_LIST[0],
            scope=CONFIGURATION_SCOPE,
            agents=AGENTS)
        # Need to reconnect after the restore process
        # bro_db.setup()
        # bro_db.validate_db(NUMBER_RECORDS)
        # bro_db.teardown_pg_db()
        # Verify data in kvdb after restore
        # sys_kvdb.verify_data()

        # Verify the test data in CM
        sys_cm.verify_test_data()

        bro_kafka_utils.validate_notifications(
            KAFKA_CONSUMER, progress_report_id, CONFIGURATION_SCOPE,
            "RESTORE")
        REDIS_CLIENT.validate_notification(
            progress_report_id, CONFIGURATION_SCOPE, "RESTORE")

        # Allow CMYP time to come back online after DocDB restore
        time.sleep(20)

        assert_equal(CMYP.get_housekeeping_configuration(
            backup_manager=CONFIGURATION_SCOPE), ("5", "disabled"))

        # Check scheduler configuration remains same after upgrade
        # uri = SFTP_SERVER.get_sftp_uri_for_path(SFTP_PATH)
        # assert_equal(CMYP.get_scheduler_configuration(
        #     backup_manager=CONFIGURATION_SCOPE),
        #              ("unlocked", "enabled", "SCHEDULED", uri))

        # Changing expected configuration due to commented out test cases
        assert_equal(CMYP.get_scheduler_configuration(
            backup_manager=CONFIGURATION_SCOPE),
                     ("unlocked", "disabled", "SCHEDULED_BACKUP", ""))

        # 3 minutes sleep waiting for operand to come up
        time.sleep(180)

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_upgrade_bro_greater_than_for_semver_match_type_restore():
    """
    This test verifies the semantic software version check is functioning
    correctly by creating and restoring a backup.
    Lowest allowed version is set to 1.0.0 in the BRO Helm Chart.
    Semantic version is set to 6.5.0 in the product-info configmap.
    Backup to be restored contains application semantic version 6.5.0.
    """
    test_case = "test_upgrade_bro_greater_than_for_semver_match_type_restore"
    log("Test Case: {}".format(test_case))

    try:
        log(
            "Initiate upgrade with selectedMatchType set to SEMVER " +
            "and semVerMatchType set to GREATER_THAN " +
            "and productLowestAllowedVersion set to 1.0.0" +
            "and appProductInfoConfigMap set to {}"
            .format(BRO_PRODUCT_CONFIGMAP))
        helm3procs.helm_upgrade_with_chart_repo_with_dict(
            HELM_RELEASE_NAME,
            INT_CHART_NAME,
            NAMESPACE,
            INT_SNAP_REPO_NAME,
            INT_SNAP_VERSION,
            settings_dict={
                "global.pullSecret": "armdocker",
                "bro.selectedMatchType": "SEMVER",
                "bro.semVerMatchType": "GREATER_THAN",
                "bro.productLowestAllowedVersion": "1.0.0",
                "bro.appProductInfoConfigMap": BRO_PRODUCT_CONFIGMAP,
                "eric-ctrl-bro.bro.vBRMAutoCreate": "DEFAULT",
                "eric-pm-server.clusterRoleName":
                    "bro-intchart-monitoring-{}".format(NAMESPACE)},
            timeout=DEPLOY_TIMEOUT)

        bro_utils.wait_for_resources_to_deploy(HELM_RELEASE_NAME, NAMESPACE)

        # Verify Orchestrator status and check registered agents
        BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())

        # Restore backup
        progress_report_id = CMYP.restore_backup(
            BACKUP_NAME_LIST[0], backup_manager=CONFIGURATION_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id, "RESTORE", scope=CONFIGURATION_SCOPE,
            backup_name=BACKUP_NAME_LIST[0], agents=AGENTS)

        # Verify the test data in CM
        sys_cm.verify_test_data()

        # Allow CMYP time to come back online after DocDB restore
        time.sleep(20)

        assert_equal(CMYP.get_housekeeping_configuration(
            backup_manager=CONFIGURATION_SCOPE), ("5", "disabled"))

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_fail_restore_semantic_version_below_lowest_allowed():
    """
    This test verifies that attempting to restore a backup with a version
    lower than the lowest allowed version in the BRO Helm Chart results
    in a failure.
    Lowest allowed version is set to 7.6.1 in the BRO Helm Chart.
    Backup to be restored contains application semantic version 6.5.0
    The semantic version of the running application defined in the configMap
    is 6.5.0
    The test performs the following steps:
    1. Initiates an upgrade using the specified settings.
    2. Waits for the upgrade to complete and for resources to deploy.
    3. Verifies Orchestrator status and checks registered agents.
    4. Restores a backup.
    5. Waits until the backup operation completes and verifies the result.
    """
    test_case = "test_backup_version_below_semantic_lowest_allowed"
    log("Test Case: {}".format(test_case))

    try:
        specific_settings = {
            "eric-ctrl-bro.bro.productLowestAllowedVersion": "7.6.1"
        }
        merged_settings_dict = {
            **GENERAL_SETTINGS_DICT,
            **SEMANTIC_SETTINGS_DICT,
            **specific_settings
        }
        log(
            "Initiate upgrade with selectedMatchType set to SEMVER " +
            "and semVerMatchType set to GREATER_THAN " +
            "and productLowestAllowedVersion set to 7.6.1 " +
            "and appProductInfoConfigMap set to {}"
            .format(BRO_PRODUCT_CONFIGMAP))

        # An upgrade is initiated on BRO with new values
        helm3procs.helm_upgrade_with_chart_repo_with_dict(
            HELM_RELEASE_NAME,
            INT_CHART_NAME,
            NAMESPACE,
            INT_SNAP_REPO_NAME,
            INT_SNAP_VERSION,
            settings_dict=merged_settings_dict,
            timeout=DEPLOY_TIMEOUT)

        bro_utils.wait_for_resources_to_deploy(HELM_RELEASE_NAME, NAMESPACE)

        # Verify Orchestrator status and check registered agents
        BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())

        # Restore backup
        progress_report_id = CMYP.restore_backup(
            BACKUP_NAME_LIST[0], backup_manager=CONFIGURATION_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id, "RESTORE", scope=CONFIGURATION_SCOPE,
            expected_result="FAILURE", backup_name=BACKUP_NAME_LIST[0],
            failure_info="Trying to restore backup with "
                         "incompatible Software Version")

    except Exception:
        get_all_pod_logs(test_case)
        raise


def test_fail_restore_with_backup_version_above_running_semver():
    """
    This test verifies that attempting to restore a backup with a version
    higher than the currently running application's semantic version
    results in a failure.
    Semantic version is set to 5.4.1 in the product-info configmap.
    Backup to be restored contains application semantic version 6.5.0

    The test performs the following steps:
    1. Initiates an upgrade using the specified settings.
    2. Annotates the product-info configmap with semantic version 5.4.1.
    3. Waits for the upgrade to complete and for resources to deploy.
    4. Verifies Orchestrator status and checks registered agents.
    5. Restores a backup.
    6. Waits until the backup operation completes and verifies the result.
    """
    test_case = "test_backup_version_higher_than_running_semantic_version"
    log("Test Case: {}".format(test_case))

    try:
        specific_settings = {
            "eric-ctrl-bro.bro.productLowestAllowedVersion": "1.2.3"
        }
        merged_settings_dict = {
            **GENERAL_SETTINGS_DICT,
            **SEMANTIC_SETTINGS_DICT,
            **specific_settings
        }
        log(
            "Initiate upgrade with selectedMatchType set to SEMVER " +
            "and semVerMatchType set to GREATER_THAN " +
            "and productLowestAllowedVersion set to 1.2.3 " +
            "and appProductInfoConfigMap set to {}"
            .format(BRO_PRODUCT_CONFIGMAP))

        # An upgrade is initiated on BRO with new values
        helm3procs.helm_upgrade_with_chart_repo_with_dict(
            HELM_RELEASE_NAME,
            INT_CHART_NAME,
            NAMESPACE,
            INT_SNAP_REPO_NAME,
            INT_SNAP_VERSION,
            settings_dict=merged_settings_dict,
            timeout=DEPLOY_TIMEOUT)

        # Annotates the product-info configmap with semantic version 5.4.1
        KUBE.annotate_configmap(NAMESPACE, "ericsson.com/semantic-version",
                                "5.4.1", "product-info-conf")
        bro_utils.wait_for_resources_to_deploy(HELM_RELEASE_NAME, NAMESPACE)

        # Verify Orchestrator status and check registered agents
        BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())

        # Restore backup
        progress_report_id = CMYP.restore_backup(
            BACKUP_NAME_LIST[0], backup_manager=CONFIGURATION_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id, "RESTORE", scope=CONFIGURATION_SCOPE,
            expected_result="FAILURE", backup_name=BACKUP_NAME_LIST[0],
            failure_info="Trying to restore backup with "
                         "incompatible Software Version")

    except Exception:
        get_all_pod_logs(test_case)
        raise


def test_delete_backup_cmyp():
    """
    This test verifies that a backup can be deleted.
    Uses v2 endpoint.
    """
    test_case = "test_delete_backup_cmyp"
    log("Test Case: {}".format(test_case))

    try:
        progress_report_id = CMYP.delete_backup(
            BACKUP_NAME_LIST[0], backup_manager=CONFIGURATION_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id, "DELETE_BACKUP", scope=CONFIGURATION_SCOPE,
            backup_name=BACKUP_NAME_LIST[0], agents=AGENTS)

        assert CMYP.backup_manager_has_backup(
            BACKUP_NAME_LIST[0],
            backup_manager=CONFIGURATION_SCOPE) is False, \
            "Backup {} was found in the backup list.".format(
                BACKUP_NAME_LIST[0])

        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "0", "bro_stored_backups", "backup_type", CONFIGURATION_SCOPE)
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "0", "bro_stored_backups", "backup_type", DEFAULT_SCOPE)
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "0", "bro_stored_backups", "backup_type", DEFAULT_SCOPE + "-bro")
        METRIC_VALIDATOR.validate_disk_usage_bytes([
            {'BRM_id': 'DEFAULT', 'Value': '0'},
            {'BRM_id': 'DEFAULT-' + V4_FRAG_CM["AgentId"], 'Value': '0'},
            {'BRM_id': 'DEFAULT-bro', 'Value': '0'},
            {'BRM_id': 'DEFAULT-eric-data-document-database-pg', 'Value': '0'},
            {'BRM_id': 'DEFAULT-tls', 'Value': '0'},
            {'BRM_id': 'configuration-data', 'Value': '0'},
            {'BRM_id': 'configuration-data-bro', 'Value': '0'}])

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_default_create_backup1_cmyp():
    """
    Using DEFAULT BRM create a backup_1.
    backup_1 should appear under BRM and vBRM, at index 0.
    """
    test_case = "test_default_create_backup_cmyp"
    log("Test Case: {}".format(test_case))

    try:
        # Create backup_1 by DEFAULT BRM
        progress_report_id = CMYP.create_backup(BACKUP_NAME_LIST[0])
        wait_until_bro_cmyp_ready_after_action(
            progress_report_id, "CREATE_BACKUP",
            backup_name=BACKUP_NAME_LIST[0], agents=AGENTS)

        brm_view = bro_utils.get_brm_view_from_cmm_by_name(DEFAULT_SCOPE)
        assert_equal(brm_view['backup'][0]['id'], BACKUP_NAME_LIST[0])

        vbrm_view = bro_utils.get_brm_view_from_cmm_by_name(VBRM_SCOPE)
        assert_equal(vbrm_view['backup'][0]['id'], BACKUP_NAME_LIST[0])

        # Validate PM metrics
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1", "bro_stored_backups", "backup_type", DEFAULT_SCOPE)
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "2", "bro_operations_total", ["backup_type", "action"],
            [DEFAULT_SCOPE, "CREATE_BACKUP"])
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "0", "bro_stored_backups", "backup_type", VBRM_SCOPE)

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_vbrm_create_backup2_cmyp():
    """
    Using VBRM create a backup_2.
    backup_2 should just appear under vBRM, at index 1.
    """
    test_case = "test_vbrm_create_backup_cmyp"
    log("Test Case: {}".format(test_case))

    try:
        # Create backup_2 by vBRM
        progress_report_id = CMYP.create_backup(
            BACKUP_NAME_LIST[1], VBRM_SCOPE)
        wait_until_bro_cmyp_ready_after_action(
            progress_report_id, "CREATE_BACKUP", VBRM_SCOPE,
            backup_name=BACKUP_NAME_LIST[1], agents=AGENTS[:1])

        brm_view = bro_utils.get_brm_view_from_cmm_by_name(DEFAULT_SCOPE)
        assert_equal(len(brm_view['backup']), 1)

        vbrm_view = bro_utils.get_brm_view_from_cmm_by_name(VBRM_SCOPE)
        assert_equal(vbrm_view['backup'][1]['id'], BACKUP_NAME_LIST[1])

        # Validate PM metrics
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1", "bro_stored_backups", "backup_type", DEFAULT_SCOPE)
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "2", "bro_operations_total", ["backup_type", "action"],
            [DEFAULT_SCOPE, "CREATE_BACKUP"])
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1", "bro_stored_backups", "backup_type", VBRM_SCOPE)
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1", "bro_operations_total", ["backup_type", "action"],
            [VBRM_SCOPE, "CREATE_BACKUP"])

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_brm_restore_backup1_cmyp():
    """
    restore backup_1 through the DEFAULT brm
    progress report under BRM/backup/0, no change under vBRM
    """
    test_case = "test_brm_restore_backup1_cmyp"
    log("Test Case: {}".format(test_case))

    try:
        # Restore backup_1 by DEFAULT BRM
        progress_report_id = CMYP.restore_backup(BACKUP_NAME_LIST[0],
                                                 backup_manager=DEFAULT_SCOPE)
        wait_until_bro_cmyp_ready_after_action(
            progress_report_id,
            "RESTORE",
            scope=DEFAULT_SCOPE,
            backup_name=BACKUP_NAME_LIST[0],
            agents=AGENTS)

        brm_view = bro_utils.get_brm_view_from_cmm_by_name(DEFAULT_SCOPE)
        assert_equal(
            brm_view['backup'][0]['progress-report'][0]['action-name'],
            'RESTORE')

        vbrm_view = bro_utils.get_brm_view_from_cmm_by_name(VBRM_SCOPE)
        # assert that progress-report is empty
        assert "progress-report" not in vbrm_view.get("backup", [{}])[0], \
            "The progress report should not appear under {}/{}." \
            .format(VBRM_SCOPE, BACKUP_NAME_LIST[0])

        # Validate PM metrics
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1", "bro_operations_total", ["backup_type", "action"],
            [DEFAULT_SCOPE, "RESTORE"])
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "0", "bro_operations_total", ["backup_type", "action"],
            [VBRM_SCOPE, "RESTORE"])

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_vbrm_restore_backup1_cmyp():
    """
    restore backup_1 through the DEFAULT-v4-fragment-and-custom-metadata brm
    progress report under vBRM/backup/0, no change under BRM
    """
    test_case = "test_vbrm_restore_backup1_cmyp"
    log("Test Case: {}".format(test_case))

    try:
        # Restore backup_1 by VBRM
        progress_report_id = CMYP.restore_backup(BACKUP_NAME_LIST[0],
                                                 backup_manager=VBRM_SCOPE)
        wait_until_bro_cmyp_ready_after_action(
            progress_report_id,
            "RESTORE",
            scope=VBRM_SCOPE,
            backup_name=BACKUP_NAME_LIST[0],
            agents=AGENTS[:1])

        brm_view = bro_utils.get_brm_view_from_cmm_by_name(DEFAULT_SCOPE)
        assert_equal(len(brm_view['backup'][0]['progress-report']), 1)

        vbrm_view = bro_utils.get_brm_view_from_cmm_by_name(VBRM_SCOPE)
        assert_equal(
            vbrm_view['backup'][0]['progress-report'][0]['action-name'],
            'RESTORE')

        # Validate PM metrics
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1", "bro_operations_total", ["backup_type", "action"],
            [DEFAULT_SCOPE, "RESTORE"])
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1", "bro_operations_total", ["backup_type", "action"],
            [VBRM_SCOPE, "RESTORE"])

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_vbrm_restore_backup2_cmyp():
    """
    restore backup_2 through the DEFAULT-v4-fragment-and-custom-metadata
    brm progress report under vBRM/backup/1, no change under BRM
    """
    test_case = "test_vbrm_restore_backup2_cmyp"
    log("Test Case: {}".format(test_case))

    try:
        # Restore backup_2 by VBRM
        progress_report_id = CMYP.restore_backup(BACKUP_NAME_LIST[1],
                                                 backup_manager=VBRM_SCOPE)
        wait_until_bro_cmyp_ready_after_action(
            progress_report_id,
            "RESTORE",
            scope=VBRM_SCOPE,
            backup_name=BACKUP_NAME_LIST[1],
            agents=AGENTS[:1])

        brm_view = bro_utils.get_brm_view_from_cmm_by_name(DEFAULT_SCOPE)
        assert brm_view['backup'][0]['progress-report'][0]['action-id'] != \
            progress_report_id, "The action should not be under BRM"

        vbrm_view = bro_utils.get_brm_view_from_cmm_by_name(VBRM_SCOPE)
        assert_equal(
            vbrm_view['backup'][1]['progress-report'][0]['action-name'],
            'RESTORE')

        # Validate PM metrics
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1", "bro_operations_total", ["backup_type", "action"],
            [DEFAULT_SCOPE, "RESTORE"])
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "2", "bro_operations_total", ["backup_type", "action"],
            [VBRM_SCOPE, "RESTORE"])

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_brm_export_backup1_cmyp():
    """
    export backup_1 through the DEFAULT brm
    progress report under BRM/backup/0
    """
    test_case = "test_brm_export_backup1_cmyp"
    log("Test Case: {}".format(test_case))

    try:
        # Waits until the ftp server is up and running
        SFTP_SERVER.wait_to_start()
        uri = SFTP_SERVER.get_sftp_uri_for_path(SFTP_PATH)
        log("Exporting {} from {}".format(BACKUP_NAME_LIST[0], uri))
        # Export backup_1 by DEFAULT BRM
        progress_report_id = CMYP.export_backup(BACKUP_NAME_LIST[0],
                                                uri,
                                                SFTP_PASSWORD,
                                                backup_manager=DEFAULT_SCOPE)
        # Would use wait_until_bro_cmyp_ready_after_action but need to carry
        # out the check for progress percetnage while the action is running
        CMYP.wait_until_progress_report_is_complete(
            progress_report_id,
            "EXPORT",
            scope=DEFAULT_SCOPE,
            backup_name=BACKUP_NAME_LIST[0],
            agents=AGENTS)

        brm_view = bro_utils.get_brm_view_from_cmm_by_name(DEFAULT_SCOPE)
        assert_equal(
            brm_view['backup'][0]['progress-report'][0]['action-name'],
            'EXPORT')

        # Allow time for PM to update
        time.sleep(3)
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1", "bro_operations_total", ["backup_type", "action"],
            [DEFAULT_SCOPE, "EXPORT"])
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1", "bro_operation_info", ["backup_type", "action"],
            [DEFAULT_SCOPE, "EXPORT"])
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "0", "bro_operations_total", ["backup_type", "action"],
            [VBRM_SCOPE, "EXPORT"])
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "0", "bro_operation_info", ["backup_type", "action"],
            [VBRM_SCOPE, "EXPORT"])

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_brm_delete_backup1_cmyp():
    """
    delete backup_1 through the DEFAULT brm
    progress report under BRM
    BRM backup list is empty
    backup_2 is under vBRM/bakcup/0
    """
    test_case = "test_brm_delete_backup1_cmyp"
    log("Test Case: {}".format(test_case))

    try:
        progress_report_id = CMYP.delete_backup(BACKUP_NAME_LIST[0],
                                                backup_manager=DEFAULT_SCOPE)

        wait_until_bro_cmyp_ready_after_action(
            progress_report_id,
            "DELETE_BACKUP",
            scope=DEFAULT_SCOPE,
            backup_name=BACKUP_NAME_LIST[0],
            agents=AGENTS
        )

        brm_view = bro_utils.get_brm_view_from_cmm_by_name(DEFAULT_SCOPE)
        assert_equal(brm_view['progress-report'][0]['action-name'],
                     'DELETE_BACKUP')
        # assert that backup list is empty
        assert not brm_view['backup'], \
            "The backup under {} should be empty.".format(DEFAULT_SCOPE)

        vbrm_view = bro_utils.get_brm_view_from_cmm_by_name(VBRM_SCOPE)
        assert_equal(vbrm_view['backup'][0]['id'], BACKUP_NAME_LIST[1])

        # Allow time for PM to update
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "2", "bro_operations_total", ["backup_type", "action"],
            [DEFAULT_SCOPE, "DELETE_BACKUP"])
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1", "bro_operation_info", ["backup_type", "action"],
            [DEFAULT_SCOPE, "DELETE_BACKUP"])
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "0", "bro_operations_total", ["backup_type", "action"],
            [VBRM_SCOPE, "DELETE_BACKUP"])
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "0", "bro_operation_info", ["backup_type", "action"],
            [VBRM_SCOPE, "DELETE_BACKUP"])

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_metrics_available_in_pm():
    """
    This test verifies the BRO metrics are available in PM Server
    """
    log("Test Case: test_metrics_available_in_pm")

    query_url = \
        "http://eric-pm-server:9090/api/v1/query?query={job=\"bro-metrics\"}"

    # Query the endpoint for count of BRO metrics
    response = rest_utils.rest_request(
        rest_utils.REQUESTS.get, "Retrieve metric count from PM", query_url)

    # Retrieve number of metric blocks from the array
    metrics_array = response["data"]["result"]
    num_metrics = len(metrics_array)
    log("Number of metrics retrieved was {}".format(num_metrics))

    # Verify count of BRO metrics is the expected amount
    assert num_metrics >= NUM_EXPECTED_METRICS, \
        "The expected number of metrics was not found, expected at " \
        "least {} but was {}".format(NUM_EXPECTED_METRICS, num_metrics)


def test_auto_export_failure():
    """
    Update scheduler configuration with incorrect auto-export-password.
    Create a periodic-event and verify that scheduled auto-export action
    fails
    """

    test_case = "test_auto_export_failure"
    log("Test Case: {}".format(test_case))

    try:
        # Get the list of action before scheduled event runs
        actions = BRO_CTRL.get_actions(CONFIGURATION_SCOPE)
        action_count_before = \
            len([x for x in actions if x["name"] in ["CREATE_BACKUP",
                                                     "EXPORT"]])

        # Update Scheduler with incorrect auto-export-password
        uri = SFTP_SERVER.get_sftp_uri_for_path(SFTP_PATH)
        incorrect_pwd = "incorrect"
        CMYP.set_scheduler_configuration("SCHEDULED", admin_state="unlocked",
                                         auto_export="enabled",
                                         auto_export_uri=uri,
                                         auto_export_password=incorrect_pwd,
                                         backup_manager=CONFIGURATION_SCOPE)

        assert_equal(CMYP.get_scheduler_configuration(
            backup_manager=CONFIGURATION_SCOPE),
                     ("unlocked", "enabled", "SCHEDULED", uri))

        # Create Periodic Event
        now = time.time()
        start_time = time.localtime(now)
        # stopTime is 2.5 hours from now
        stop_time = time.localtime(now + 60*(60+60+30))
        date_format = "%Y-%m-%dT%H:%M:%S"

        f_start_time = time.strftime(date_format, start_time)
        f_stop_time = time.strftime(date_format, stop_time)

        CMYP.create_or_update_periodic_event(
            EVENT_ID, "30", "1",
            CONFIGURATION_SCOPE,
            f_start_time,
            f_stop_time)

        expected = ("30", "1", f_start_time + "+00:00",
                    f_stop_time + "+00:00")

        actual = CMYP.get_periodic_event(EVENT_ID, CONFIGURATION_SCOPE,
                                         True, True)
        bro_utils.check_periodic_event_contents(expected, actual)

        # Update Periodic Event with hours '0' and minutes '1'
        CMYP.create_or_update_periodic_event(EVENT_ID,
                                             "1", "0",
                                             CONFIGURATION_SCOPE)
        expected = ("1", "0", f_start_time + "+00:00",
                    f_stop_time + "+00:00")
        actual = CMYP.get_periodic_event(EVENT_ID, CONFIGURATION_SCOPE,
                                         True, True)
        bro_utils.check_periodic_event_contents(expected, actual)

        # Wait for create backup action to run for scheduled event
        time.sleep(61)
        BRO_CTRL.wait_for_orchestrator_to_be_available()

        # Get the list of actions after scheduled event runs
        actions_after = BRO_CTRL.get_actions(CONFIGURATION_SCOPE)
        action_count_after = \
            len([x for x in actions_after if x["name"] in ["CREATE_BACKUP",
                                                           "EXPORT"]])

        # Assert create backup and export action ran
        assert_equal(action_count_after - action_count_before, 2)

        # Assert all CREATE_BACKUP actions were successful
        for action in actions_after:
            if action["name"] in ["CREATE_BACKUP"]:
                assert_equal(action["result"], "SUCCESS")

        # Assert EXPORT action failed
        export_actions = []
        for action in actions_after:
            if action["name"] in ["EXPORT"]:
                export_actions.append(action)

        auto_export_action = export_actions[len(export_actions) - 1]

        assert_equal(auto_export_action["result"], "FAILURE")

        action_id = CMYP.get_scheduled_action_id(
            backup_manager=CONFIGURATION_SCOPE)

        bro_kafka_utils.validate_notifications(
            KAFKA_CONSUMER, action_id, CONFIGURATION_SCOPE,
            "CREATE_BACKUP")
        REDIS_CLIENT.validate_notification(
            action_id, CONFIGURATION_SCOPE, "CREATE_BACKUP")

        # Allow time for PM to update
        time.sleep(15)

        # metric validations
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1",
            "bro_scheduled_operation_error",
            ["action", "backup_type"],
            ["EXPORT", CONFIGURATION_SCOPE],
        )

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_successful_scheduled_backup_and_auto_export():
    """
    Update scheduler configuration with correct auto-export-password.
    Verify scheduled create backup and auto-export actions are successful.
    """
    test_case = "test_successful_schedule_backup_and_auto_export"
    log("Test Case: {}".format(test_case))

    try:
        # Get the list of actions before scheduled event runs
        BRO_CTRL.wait_for_orchestrator_to_be_available()
        actions = BRO_CTRL.get_actions(CONFIGURATION_SCOPE)

        action_count_before = \
            len([x for x in actions if x["name"] in ["CREATE_BACKUP",
                                                     "EXPORT"]])

        uri = SFTP_SERVER.get_sftp_uri_for_path(SFTP_PATH)
        # Update Scheduler
        CMYP.set_scheduler_configuration("SCHEDULED", admin_state="unlocked",
                                         auto_export="enabled",
                                         auto_export_uri=uri,
                                         auto_export_password=SFTP_PASSWORD,
                                         backup_manager=CONFIGURATION_SCOPE)

        assert_equal(CMYP.get_scheduler_configuration(
            backup_manager=CONFIGURATION_SCOPE),
                     ("unlocked", "enabled", "SCHEDULED", uri))

        # Wait for create backup action to run for scheduled event
        time.sleep(61)
        BRO_CTRL.wait_for_orchestrator_to_be_available()

        action_id = CMYP.get_scheduled_action_id(
            backup_manager=CONFIGURATION_SCOPE)

        # Get list of actions after scheduled event runs
        actions_after = BRO_CTRL.get_actions(CONFIGURATION_SCOPE)

        action_count_after = \
            len([x for x in actions_after if x["name"]
                 in ["CREATE_BACKUP", "EXPORT"]])

        # Assert create backup and export action ran (the condition
        # is to account for if an additional schedule event runs
        assert action_count_after - action_count_before >= 2

        # Assert all CREATE_BACKUP actions were successful
        for action in actions_after:
            if action["name"] in ["CREATE_BACKUP"]:
                # scheduled event's result could be not-availble
                # we will check the actions run are not failed
                assert action["result"] != "FAILURE"

        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "0",
            "bro_scheduled_operation_error",
            ["action", "backup_type"],
            ["CREATE_BACKUP", CONFIGURATION_SCOPE],
        )
        # Assert EXPORT action was successful
        export_actions = []
        for action in actions_after:
            if action["name"] in ["EXPORT"]:
                export_actions.append(action)

        auto_export_action = export_actions[len(export_actions) - 1]

        assert_equal(auto_export_action["result"], "SUCCESS")

        # verify the metric value after a successful action
        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "0",
            "bro_scheduled_backup_missed_total",
            ["event_id"],
            ["id1"]
        )

        bro_kafka_utils.check_for_action_start_completed_notification(
            KAFKA_CONSUMER, action_id, CONFIGURATION_SCOPE,
            "CREATE_BACKUP")
        REDIS_CLIENT.validate_notifications(
            action_id, CONFIGURATION_SCOPE, "CREATE_BACKUP", count=4)

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_delete_periodic_event():
    """
    Delete periodic event and verify scheduled event did not run
    """
    test_case = "test_delete_periodic_event"
    log("Test Case: {}".format(test_case))

    try:
        # Get the list of actions before deleting periodic event
        BRO_CTRL.wait_for_orchestrator_to_be_available()
        actions = BRO_CTRL.get_actions(CONFIGURATION_SCOPE)
        action_count_before = \
            len([x for x in actions if x["name"] in ["CREATE_BACKUP",
                                                     "EXPORT"]])

        CMYP.delete_periodic_event(
            EVENT_ID, backup_manager=CONFIGURATION_SCOPE)

        time.sleep(80)

        # Get the list of actions after deleting periodic event
        after_delete = BRO_CTRL.get_actions(CONFIGURATION_SCOPE)
        action_count_after = \
            len([x for x in after_delete if x["name"] in ["CREATE_BACKUP",
                                                          "EXPORT"]])

        # Assert no new backup was created after periodic event is deleted
        assert_equal(action_count_after - action_count_before, 0)

    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_scheduled_backup_failure():
    """
    Testing the counter bro_scheduled_backup_missed_total by triggering
    two periodic events at the same time
    """
    test_case = "test_scheduled_backup_failure"
    log("Test Case: {}".format(test_case))
    try:
        BRO_CTRL.wait_for_orchestrator_to_be_available()

        uri = SFTP_SERVER.get_sftp_uri_for_path(SFTP_PATH)
        CMYP.set_scheduler_configuration("SCHEDULED", admin_state="unlocked",
                                         auto_export="enabled",
                                         auto_export_uri=uri,
                                         auto_export_password=SFTP_PASSWORD,
                                         backup_manager=CONFIGURATION_SCOPE)

        # create two periodic create backup events at the same time
        CMYP.create_or_update_periodic_event(
            EVENT_ID_2, "1", hours="0",
            backup_manager=CONFIGURATION_SCOPE)
        CMYP.create_or_update_periodic_event(
            EVENT_ID_3, "1", hours="0",
            backup_manager=CONFIGURATION_SCOPE)

        # wait for both the events to run
        time.sleep(61)
        BRO_CTRL.wait_for_orchestrator_to_be_available()

        action_id = CMYP.get_scheduled_action_id(
            backup_manager=CONFIGURATION_SCOPE)

        bro_kafka_utils.check_for_action_start_completed_notification(
            KAFKA_CONSUMER, action_id, CONFIGURATION_SCOPE,
            "CREATE_BACKUP")
        REDIS_CLIENT.validate_notifications(action_id,
                                            CONFIGURATION_SCOPE,
                                            "CREATE_BACKUP", count=4)

        # Allow time for PM to update
        time.sleep(15)

        METRIC_VALIDATOR.validate_specific_metric_for_specified_label(
            "1",
            "bro_scheduled_backup_missed_total",
            ["event_id"],
            ["id3"]
        )

        # delete both the events
        CMYP.delete_periodic_event(
            EVENT_ID_2, backup_manager=CONFIGURATION_SCOPE)
        # Allow time for event to delete
        time.sleep(5)
        CMYP.delete_periodic_event(
            EVENT_ID_3, backup_manager=CONFIGURATION_SCOPE)
    except Exception:
        get_all_pod_logs(test_case)
        raise

    finally:
        get_all_pod_logs(test_case)


def test_collect_logs():
    """
    Collects logs for archiving
    """
    log("Test Case: test_collect_logs")
    get_all_pod_logs("post_all_tests")


def test_remove_k8s_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    log("Test Case: test_remove_k8s_resources")
    # Close the CMYP connection
    CMYP.close()

    # Close kafka consumer
    KAFKA_CONSUMER.close()
    REDIS_CLIENT.close()

    # stops sftp server
    SFTP_SERVER.stop_sftp_server()
    bro_utils.remove_namespace_resources(NAMESPACE)
    log("Finished removing Kubernetes resources")


# Helper functions
def get_agent_ids():
    """
    Fetches the Agent Id of all agents and
    returns this as a list.

    :return: a list of the agent Ids.
    """

    agents = []
    for agent in AGENTS:
        agents.append(agent["AgentId"])
    return agents


def get_all_pod_logs(log_file_suffix):
    """
    Gets the pod logs for all agents and the Orchestrator

    :param log_file_suffix: A string to append to the name of the log files
    """
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, log_file_suffix)
    get_agent_pod_logs(log_file_suffix)


def get_agent_pod_logs(log_file_suffix):
    """
    Gets the pod logs for all agents

    :param log_file_suffix : A string to append to the name of the log files
    """
    agent_pod_prefixes = get_agent_pod_prefixes()

    for prefix in agent_pod_prefixes:
        bro_utils.get_pod_logs(NAMESPACE, prefix, log_file_suffix)


def get_agent_pod_prefixes():
    """
    Fetches the pod_prefixes of all agents and
    returns this as a list.
    :return: a list of the agent pod_prefixes.
    """

    prefixes = []
    for agent in AGENTS:
        prefixes.append(agent["pod_prefix"])
    return prefixes


def wait_until_bro_cmyp_ready_after_action(progress_report_id,
                                           action_name,
                                           scope="DEFAULT",
                                           expected_result="SUCCESS",
                                           backup_name=None,
                                           failure_info=None,
                                           agents=None):
    """
    wait until both BRO and CMYP is ready
    :param progress_report_id : the id of progress report
    :param action_name : the name of action
    :param scope : the BRM
    :param expected_result : the expected result
    :param backup_name : the name of backup
    :param failure_info: failure reason for the action
    """
    BRO_CTRL.wait_for_action_complete(
        action_id=progress_report_id,
        action_type=action_name,
        scope=scope,
        expected_result=expected_result,
        backup_name=backup_name,
        failure_info=failure_info

    )
    time.sleep(10)
    CMYP.wait_until_progress_report_is_complete(
        progress_report_id=progress_report_id,
        action_name=action_name,
        scope=scope,
        expected_result=expected_result,
        backup_name=backup_name,
        agents=agents
    )


def prepare_cmyp():
    """
    Prepare the cmyp user and connect to the cmyp server
    """
    # Create the cmyp user in LDAP via IAM
    token_query_url = ('https://eric-sec-access-mgmt-http:8443/'
                       'auth/realms/master/protocol/openid-connect/token')
    token_request_data = {'grant_type': 'password', 'username': 'adminplanb',
                          'password': 'kcpwb@Planb1', 'client_id': 'admin-cli'}
    token_request_headers = {'content-type':
                             'application/x-www-form-urlencoded'}
    # Query the endpoint for the token
    token_request_response = rest_utils.rest_request(
        rest_utils.REQUESTS.post, "Retrieve IAM token",
        token_query_url, token_request_data, headers=token_request_headers,
        content_type_json=False, ca_cert=False)
    token = json.loads(token_request_response)['access_token']
    user_creation_query_url = ('https://eric-sec-access-mgmt-http:8443/'
                               'auth/admin/realms/local-ldap3/users')
    user_creation_request_data = {
        "username": CMYP_USER,
        "enabled": "true",
        "credentials": [
            {
                "type": "password",
                "value": CMYP_PSW
            }
        ],
        "attributes": {
            "uidNumber": ["710"],
            "roles": ["system-admin"],
            "pwdReset": ["FALSE"]
        }
    }
    user_creation_headers = {'content-type': 'application/json',
                             'Authorization': ''}
    user_creation_headers['Authorization'] = 'bearer ' + token
    # Send the create user request
    rest_utils.rest_request(
        rest_utils.REQUESTS.post, "Create cmyp user",
        user_creation_query_url, user_creation_request_data,
        headers=user_creation_headers, ca_cert=False)
    CMYP.connect()


def check_log_is_present(retrieved_logs_dict, search_terms):
    """
    Checks if given message is present in the given logs dictionary argument.
    :param retrieved_logs_dict: retrieved logs dictionary from Search Engine
    :param search_terms: list of terms which is unique to the required log
    :return: a boolean whether the logs were found or not
    """
    # for each "hit" in the list of hits check the log message contents
    for hit in retrieved_logs_dict["hits"]["hits"]:
        if all(term in hit["_source"]["message"] for term in search_terms):
            return True

    return False
