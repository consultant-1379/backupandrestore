#!/usr/bin/env python3
"""
This module provides test cases to verify the Backup and Restore Orchestrator.
In this test phase global security is set to false.
See confluence page for architecture and use cases:
 https://confluence.lmera.ericsson.se/display/AA/BR+Orchestrator+Component
 +Description
"""
import os
import random
import time
from datetime import datetime

import bro_ctrl
import bro_data
import bro_sftp
import bro_utils
from bro_utils import assert_equal
import helm3procs
import k8sclient
from utilprocs import log
import rest_utils
from globals import (V2_FRAG_ONLY, V2_FRAG_CM,
                     V3_FRAG_CM, V3_FRAG_ONLY,
                     V4_FRAG_CM,
                     V4TLS_FRAG_ONLY,
                     V4_MULTI_FRAG_CM,
                     V4_NO_FRAG_OR_META,
                     V4_NO_FRAG_CM_ONLY,
                     V4_MULTI_FRAG_CM_CONFIGURATION,
                     V4_MULTI_FRAG_CM_SUBSCRIBER)

# Get variables passed in from ruleset
from bro_dualport import DualPortValidator

NAMESPACE = os.environ.get("kubernetes_namespace")
# snapshot repo
BRO_SNAP_REPO = os.environ.get("helm_repo")
# chart_version
BRO_SNAP_VERSION = os.environ.get('baseline_chart_version')
# chart_archive
BRO_SNAP_ARCHIVE = os.environ.get("chart_archive")
# bro longjump version
BRO_LONGJUMP_VERSION = "3.2.0+51"

# Set BRO variables
BRO_NAME = "eric-ctrl-bro"

BRO_REL_REPO = "https://arm.sero.gic.ericsson.se/artifactory/" \
               "proj-adp-eric-ctrl-bro-drop-helm"
BRO_REL_REPO_NAME = "bro_rel"
BRO_SNAP_REPO_NAME = "bro_snap"
BRO_POD_NAME = "eric-ctrl-bro-0"
BRO_PVC = "backup-data-" + BRO_POD_NAME

BRO_HELM_RELEASE_NAME = bro_utils.get_service_helm_release_name(
    BRO_NAME, NAMESPACE)

# Set Test Service variables
TEST_CHART_NAME = "eric-test-service-basic"
TEST_REPO = os.environ.get("test_helm_repo")
TEST_REPO_NAME = "test_svc"
# Remove after merge to master
# Handling the fact that test repo in tests is picking
# up v3 agents but BRO on branch is still v2 only
TEST_CHART_VERSION = ""
BRO_PRA_REPO = "https://arm.sero.gic.ericsson.se/artifactory/" \
               "proj-adp-gs-released-helm"

BRO_PRA_REPO_NAME = "bro_pra_rel"

BRO_PRODUCT_CONFIGMAP = "productInfoConf"

# Instantiate kube client
KUBE = k8sclient.KubernetesClient()

BRO_CTRL = bro_ctrl.BroCtrlClient()

# ORCH Backup Data Location
BACKUP_LOCATION = "/bro/backups"

# Base REST URL
BASE_URL = "http://eric-ctrl-bro:7001/v1/backup-manager"

# SFTP Server USER and password
SFTP_USER = "brsftp"
SFTP_PASSWORD = "planbsftp"
SFTP_PATH = "bro_test/1/3/"
SFTP_SERVER = bro_sftp.SFTPServer(SFTP_USER, SFTP_PASSWORD)
HTTP_PATH = ""

# Define Backup Names
BACKUP_LIST = ["bu1", "bu2"]
SUBSCRIBER_BACKUP_LIST = ["sub_bu1", "sub_bu2"]
CONFIGURATION_BACKUP_LIST = ["con_bu1", "con_bu2"]
MAX_STORED_BACKUPS = 5


# VBRM Suffixes
V4_FRAG_CM_VBRM_SUFFIX = "-" + V4_FRAG_CM["AgentId"]
V3_FRAG_CM_VBRM_SUFFIX = "-" + V3_FRAG_CM["AgentId"]

# Backup Manager Scopes
DEFAULT_SCOPE = "DEFAULT"
SUBSCRIBER_SCOPE = "subscriber"
CONFIGURATION_SCOPE = "configuration-data"
V4_FRAG_CM_VBRM_SCOPE = "DEFAULT" + V4_FRAG_CM_VBRM_SUFFIX
V3_FRAG_CM_VBRM_SCOPE = "DEFAULT" + V3_FRAG_CM_VBRM_SUFFIX

V2_AGENTS = [V2_FRAG_ONLY, V2_FRAG_CM]
V3_AGENTS = [V3_FRAG_CM, V3_FRAG_ONLY]
V4_AGENTS = [V4_FRAG_CM,
             V4TLS_FRAG_ONLY,
             V4_MULTI_FRAG_CM,
             V4_NO_FRAG_OR_META,
             V4_NO_FRAG_CM_ONLY]

TEST_AGENTS = V2_AGENTS + V3_AGENTS + V4_AGENTS
CONFIGURATION_AGENTS = [V4_FRAG_CM, V4_MULTI_FRAG_CM_CONFIGURATION,
                        V3_FRAG_CM, V3_FRAG_ONLY]
SUBSCRIBER_AGENTS = [V4TLS_FRAG_ONLY,
                     V2_FRAG_ONLY,
                     V4_MULTI_FRAG_CM_SUBSCRIBER,
                     V4_NO_FRAG_OR_META,
                     V4_NO_FRAG_CM_ONLY]

V4_FRAG_CM_VBRM_AGENT = [V4_FRAG_CM]
V3_FRAG_CM_VBRM_AGENT = [V3_FRAG_CM]
BACKUP_TYPE = [
    V4_MULTI_FRAG_CM_SUBSCRIBER,
    V4_MULTI_FRAG_CM_CONFIGURATION,
]

BACKUP_TARBALL_NAME = ""

# Periodic event id. This value is set in
# test_schedule_backup_and_auto_export
EVENT_ID = ""

DATE_FORMAT = "%Y-%m-%dT%H:%M:%S"
TIME_FORMAT = "%H:%M:%S"


# Test Cases


def test_clear_any_failed_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    log("Test Case: test_clear_any_failed_resources")

    bro_utils.remove_namespace_resources(NAMESPACE)
    log("Finished removing Kubernetes resources")


def test_deploy_bro_service():
    """
    Deploys the Orchestrator Service.
    """
    log("Test Case: test_deploy_bro_service")

    log("Add the BRO Snapshot helm repo")
    helm3procs.add_helm_repo(BRO_SNAP_REPO, BRO_SNAP_REPO_NAME)

    # Install Orchestrator Chart
    start_time = time.perf_counter()
    bro_utils.install_service_chart(BRO_SNAP_REPO_NAME, BRO_NAME,
                                    BRO_SNAP_VERSION, NAMESPACE,
                                    agent_discovery=True,
                                    enable_global_tls=False)
    install_time = bro_utils.calc_time(start_time, BRO_POD_NAME, NAMESPACE)
    log("TEST REPORT: Install Time: {}s".format(install_time))

    # Verify that there are no agents registered
    assert BRO_CTRL.get_registered_agents() == [], "No agents were expected"

    # Check that the DEFAULT backup-manager is created
    # Expect 2 since the reset vBRM will be created
    BRO_CTRL.check_backup_manager_is_created(expected_number_of_managers=2,
                                             rest_version="v4")

    # Get all actions in the default backup-manager
    out = BRO_CTRL.get_actions()
    # Verify there are no actions
    assert out == [], "No actions were expected"

    # Get all backups in the default backup-manager
    out = BRO_CTRL.get_backups()
    assert out == [], "No backups were expected"
    # Starts the sftp server, doesn't wait for the service to be up and running
    SFTP_SERVER.start_sftp_server()


def test_update_backup_manager():
    """
    Updates the default backup manager.
    """
    log("Test Case: test_update_backup_manager")

    BRO_CTRL.update_backup_manager({"backupType": "Data Services",
                                    "backupDomain": "OSS"})

    # Verify Backup Manager endpoint
    # Only Import and Create backup should be available
    backup_manager = BRO_CTRL.get_backup_manager(rest_version="v4")

    create_task = bro_utils.generate_backup_manager_task(
        task='create', scope=DEFAULT_SCOPE, method='POST',
        task_state="Available")
    import_task = bro_utils.generate_backup_manager_task(
        task='import', scope=DEFAULT_SCOPE, method='POST',
        task_state="Available")

    assert create_task and import_task in backup_manager['availableTasks'], \
        "backup-manager does not have the expected availableTasks"

    assert [] == backup_manager['ongoingTasks'], \
        "backup-manager does not have the expected ongoingTasks"

    assert 'Data Services' and 'OSS' and DEFAULT_SCOPE \
        in list(backup_manager.values()), \
        "backup-manager not updated as expected"


def test_deploy_bro_agents():
    """
    Deploys the Test Services/Agents.
    """
    log("Test Case: test_deploy_bro_agents")

    log("Add the test service helm repo")
    global TEST_CHART_VERSION, TEST_REPO

    # Get latest test service chart
    # This colon exists if a version is passed in the ruleset.
    # This implies a branch is being used and that the relevant
    # test-service ARM repo should be used
    if ";" in TEST_REPO:
        [TEST_REPO, TEST_CHART_VERSION] = TEST_REPO.split(";")
        helm3procs.add_helm_repo(TEST_REPO, TEST_REPO_NAME)
    else:
        helm3procs.add_helm_repo(TEST_REPO, TEST_REPO_NAME)
        TEST_CHART_VERSION = \
            helm3procs.get_latest_chart_version(TEST_CHART_NAME,
                                                helm_repo_name=TEST_REPO_NAME,
                                                development_version=True)

    # Deploy the v2 test agents
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 '4.0.0-8',
                                 [agent["AgentId"] for agent in V2_AGENTS],
                                 NAMESPACE, enable_global_tls=False)

    # Deploy the v3 test agents
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION,
                                 [agent["AgentId"] for agent in V3_AGENTS],
                                 NAMESPACE, enable_global_tls=False)

    # Deploy the v4 test agents
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION,
                                 [agent["AgentId"] for agent in V4_AGENTS],
                                 NAMESPACE, enable_global_tls=False)

    # Verify that Orchestrator has all expected agents registered
    BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())

    # DEFAULT vBRM for each agent, plus DEFAULT, SUBSCRIBER and CONFIGURATION
    # plus the DEFAULT, SUBSCRIBER and CONFIGURATION reset vBRMs
    brm_count = len(get_agent_ids()) + (3 * 2)

    # confirm that the expected scopes are present
    log("Checking the backup managers for the subscriber scope")
    BRO_CTRL.check_backup_manager_is_created(SUBSCRIBER_SCOPE, brm_count)
    log("Checking the agents registered to the subscriber scope")
    subscriber_agents = BRO_CTRL.get_registered_agents_of_brm(SUBSCRIBER_SCOPE)
    bro_utils.assert_equal(len(subscriber_agents), len(SUBSCRIBER_AGENTS))

    log("Checking the backup managers for the configuration-data scope")
    BRO_CTRL.check_backup_manager_is_created(CONFIGURATION_SCOPE, brm_count)
    log("Checking the agents registered to the configuration-data scope")
    config_agents = BRO_CTRL.get_registered_agents_of_brm(CONFIGURATION_SCOPE)
    assert len(config_agents) == len(CONFIGURATION_AGENTS), \
        "Number of agents do not match, Expected: {} Actual: {}".format(
            len(CONFIGURATION_AGENTS), len(config_agents))


def test_bro_config_restored():
    """
    This test verifies that the BRO configuration is restored.
    * Create a periodic event
    * Then, create a backup and the backup is exported to the sftp server
    * Then BRO along with its PVC is deleted
    * Then BRO is reinstalled
    * The backup from the sftp is imported back and restored
      under the  -bro BRM
      Then verify the existence of periodic events
    """
    log("Test Case: test_bro_config_restored")

    # Create an event 2.5 hours in the future
    now = time.time()
    timezone = "Z"
    date_format = "%Y-%m-%dT%H:%M:%S"
    scheduled_time_in_utc = now + 60*(60+60+30)
    start_time = bro_utils.calculate_time_in_timezone(timezone,
                                                      scheduled_time_in_utc)
    event_config = {
        "hours": 1,
        "startTime": time.strftime(date_format, start_time)
    }
    # Create a periodic event
    event_id = BRO_CTRL.create_scheduled_periodic_event(event_config)

    # Creates a backup with the name backup_test
    # and waits for action to complete
    BRO_CTRL.create_backup("backup_test", TEST_AGENTS, NAMESPACE)
    global BACKUP_TARBALL_NAME
    BACKUP_TARBALL_NAME = BRO_CTRL.get_backup_tarball_name("backup_test")

    # Verify Backup Manager endpoint
    # All tasks should be available
    backup_manager = BRO_CTRL.get_backup_manager(rest_version="v4")

    create_task = bro_utils.generate_backup_manager_task(
        task='create', scope=DEFAULT_SCOPE, method='POST',
        task_state="Available")
    import_task = bro_utils.generate_backup_manager_task(
        task='import', scope=DEFAULT_SCOPE, method='POST',
        task_state="Available")
    export_task = bro_utils.generate_backup_manager_task(
        task='export', scope=DEFAULT_SCOPE, backup_name='backup_test',
        method='POST', task_state="Available")
    restore_task = bro_utils.generate_backup_manager_task(
        task='restore', scope=DEFAULT_SCOPE, backup_name='backup_test',
        method='POST', task_state="Available")
    delete_task = bro_utils.generate_backup_manager_task(
        task='delete', scope=DEFAULT_SCOPE, backup_name='backup_test',
        method='DELETE', task_state="Available")

    assert create_task and import_task and export_task and restore_task \
        and delete_task in backup_manager['availableTasks'], \
        "backup-manager does not have the expected availableTasks"

    assert [] == backup_manager['ongoingTasks'], \
        "backup-manager does not have the expected ongoingTasks"

    assert 'Data Services' and 'OSS' and DEFAULT_SCOPE \
        in list(backup_manager.values()), \
        "backup-manager not updated as expected"

    # Verify Backup Manager endpoint
    # Only Restore should be available
    backup_manager = BRO_CTRL.get_backup_manager(
        backup_manager_name="DEFAULT-bro", rest_version="v4")

    restore_task = bro_utils.generate_backup_manager_task(
        task='restore', scope='DEFAULT-bro', backup_name='backup_test',
        method='POST', task_state="Available")

    expected_output = [[], [restore_task], "DEFAULT-bro"]

    assert list(backup_manager.values()) == expected_output, \
        "backup-manager not updated as expected"

    # Creates the payload for export
    sftp_payload = SFTP_SERVER.get_sftp_payload(SFTP_PATH)
    log("Exporting {}".format("backup_test"))

    # The backup is exported to the Sftp server
    BRO_CTRL.v4_export_backup(
        backup_name="backup_test",
        payload=sftp_payload)

    # bro helm chart is uninstalled
    helm3procs.helm_delete_release(BRO_HELM_RELEASE_NAME, NAMESPACE)

    # pvc for bro is deleted
    KUBE.delete_selective_pvc_namespace(BRO_PVC, NAMESPACE)

    # reinstall bro
    bro_utils.install_service_chart(BRO_SNAP_REPO_NAME, BRO_NAME,
                                    BRO_SNAP_VERSION, NAMESPACE,
                                    agent_discovery=True,
                                    enable_global_tls=False)

    # waiting for the agents to register
    BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())

    # Creates the payload for imports.
    path_remote = SFTP_PATH + DEFAULT_SCOPE + "/" + BACKUP_TARBALL_NAME
    sftp_payload = SFTP_SERVER.get_sftp_payload(path_remote)
    BRO_CTRL.v4_import_backup(
        backup_name="backup-test",
        payload=sftp_payload)

    # Checks if the periodic event does not exist before restore
    response = BRO_CTRL.get_scheduled_periodic_event(event_id)
    assert response["statusCode"] == 404, \
        "Periodic event " + event_id + " still exists"

    # restore BRO configuration
    scope = DEFAULT_SCOPE + "-bro"

    BRO_CTRL.restore_backup("backup_test", TEST_AGENTS,
                            NAMESPACE, scope, False)

    # Checks if the periodic scheduling configuration is restored
    response = BRO_CTRL.get_scheduled_periodic_event(event_id)
    assert response["id"] == event_id, \
        "Periodic event " + event_id + " was not restored"

    config = BRO_CTRL.get_scheduler_config(
        scope=DEFAULT_SCOPE, rest_version='v4')

    # Get the next scheduled time
    actual_next_scheduled_time =  \
        ".".join(config["nextScheduledTime"].split(".")[:-1])

    # verifies the time
    assert_equal(actual_next_scheduled_time, event_config["startTime"])

    # delete the periodic event
    BRO_CTRL.delete_periodic_events([{"id": event_id}], "DEFAULT")


def test_schedule_backup_and_auto_export():
    """
    Configures the default backup manager's scheduler
    with auto export enabled and set up to export to
    the sftp server, then sets up an event to run every
    minute, sleeps long enough to let it run a couple
    of times, then checks both backup and export actions
    have been run and completed successfully.
    """
    log("test_schedule_backup_and_auto_export")
    global EVENT_ID
    EVENT_ID = \
        BRO_CTRL.create_scheduled_periodic_event_autoexport(
            SFTP_SERVER,
            SFTP_PATH,
            SFTP_PASSWORD,
            DEFAULT_SCOPE,
            "v4",
            cleanup_events_after_test=False,
            timezone="+01:00")


def test_update_scheduling_configuration():
    """
    Update scheduling configuration to disable auto export using V4 Rest
    endpoint and verify the export action did not run
    """
    test_case = "test_update_scheduling_configuration"
    log("Test Case: {}".format(test_case))

    # Actions before
    actions = BRO_CTRL.get_actions(DEFAULT_SCOPE)
    backup_actions_count_before = \
        len([x for x in actions if x["name"]
             in ["CREATE_BACKUP"]])
    export_action_count_before = \
        len([x for x in actions if x["name"]
             in ["EXPORT"]])

    # Disable auto-export config in scheduling
    config = {"autoExport": "DISABLED"}
    BRO_CTRL.v4_patch_scheduler_config(config, DEFAULT_SCOPE)
    updated_config = BRO_CTRL.get_scheduler_config(DEFAULT_SCOPE)

    # Check the returned auto export config
    assert updated_config["autoExport"] == "DISABLED"

    time.sleep(70)  # Wait for sometime to let the event run

    # Actions after
    actions_after = BRO_CTRL.get_actions(DEFAULT_SCOPE)
    backup_actions_count_after = \
        len([x for x in actions_after if x["name"]
             in ["CREATE_BACKUP"]])
    export_action_count_after = \
        len([x for x in actions_after if x["name"]
             in ["EXPORT"]])

    # Assert only create backup action ran
    assert backup_actions_count_after > backup_actions_count_before, \
        "Backup action was supposed to run"
    assert export_action_count_after - export_action_count_before == 0, \
        "Auto-export action was not supposed to run"

    # Enable auto-exort config in scheduling
    config = {"autoExport": "ENABLED"}
    BRO_CTRL.v4_patch_scheduler_config(config, DEFAULT_SCOPE)
    updated_config = BRO_CTRL.get_scheduler_config(DEFAULT_SCOPE)

    # Check the returned auto export config
    assert updated_config["autoExport"] == "ENABLED"


def test_schedule_backup_failure():
    """
    This test verifies that the create-backup action fails and
    auto-export does not execute if one of the agents returns
    a failure message.
    """
    test_case = "test_schedule_backup_failure"
    log("Test Case: {}".format(test_case))

    # Install a Test Agent which will fail a backup
    agent_name = "fails-backup"
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION, [agent_name],
                                 NAMESPACE, enable_global_tls=False)
    expected_agents = get_agent_ids()
    expected_agents.append('fails-backup')
    BRO_CTRL.wait_for_bro_agents_to_reconnect(expected_agents)

    # Actions before
    actions = BRO_CTRL.get_actions(DEFAULT_SCOPE)
    action_count_before = \
        len([x for x in actions if x["name"]
             in ["EXPORT"]])

    BRO_CTRL.wait_for_orchestrator_to_be_available()
    time.sleep(70)  # Wait for sometime to let the event run
    BRO_CTRL.wait_for_orchestrator_to_be_available()

    # Actions after
    actions_after = BRO_CTRL.get_actions(DEFAULT_SCOPE)
    action_count_after = \
        len([x for x in actions_after if x["name"]
             in ["EXPORT"]])

    # Assert only create backup action ran
    assert action_count_after - action_count_before == 0, \
        "Auto-export action was not supposed to run"

    # Assert CREATE BACKUP action failed
    backup_actions = []
    for action in actions_after:
        if action["name"] in ["CREATE_BACKUP"]:
            backup_actions.append(action)

    backup_action = backup_actions[len(backup_actions) - 1]

    assert backup_action["result"] == "FAILURE", \
        "CREATE_BACKUP action did not fail as expected"

    # Assert all EXPORT actions were successful
    for action in actions_after:
        if action["name"] in ["EXPORT"]:
            assert action["result"] == "SUCCESS", \
                "Action " + str(action) + " failed"

    # Collect console logs before removing agent
    KUBE.get_pod_logs(NAMESPACE, "{}-agent-0".format(agent_name), test_case)

    # Remove the Test Agent
    helm_release_name = bro_utils.get_service_helm_release_name(agent_name,
                                                                NAMESPACE)
    helm3procs.helm_delete_release(helm_release_name, NAMESPACE)

    bro_utils.wait_for_agent_to_be_removed(agent_name, NAMESPACE)


def test_v4_replace_periodic_event():
    """
    Test the v4 Endpoint which replaces the given periodic schedule.
    """
    test_case = "test_v4_replace_periodic_event"
    log("Test Case: {}".format(test_case))
    BRO_CTRL.wait_for_orchestrator_to_be_available()
    # this will be delete in next test.
    update = {
        "days": 3,
        "hours": 10,
        "minutes": 5,
        "weeks": 1,
        "startTime": "2008-09-15T15:54:00Z",
        "stopTime": "2008-09-18T15:54:00Z"
    }
    BRO_CTRL.v4_replace_scheduled_periodic_event(
        EVENT_ID, update, DEFAULT_SCOPE)
    event = BRO_CTRL.get_scheduled_periodic_event(
        EVENT_ID, DEFAULT_SCOPE, 'v4')
    assert_equal(event["startTime"], "2008-09-15T15:54:00Z")


def test_delete_periodic_event():
    """
    Delete periodic event and verify scheduled event did not run
    """
    test_case = "test_delete_periodic_event"
    log("Test Case: {}".format(test_case))

    BRO_CTRL.wait_for_orchestrator_to_be_available()
    actions = BRO_CTRL.get_actions(DEFAULT_SCOPE)

    # Delete the event and wait for all events to finish
    BRO_CTRL.delete_scheduled_periodic_event(EVENT_ID, DEFAULT_SCOPE, 'v4')

    # Wait another 70 seconds, then check the deleted event didn't run
    time.sleep(70)

    after_delete_actions = BRO_CTRL.get_actions(DEFAULT_SCOPE)

    log("Actions before delete: " + str(actions))
    log("Actions after delete: " + str(after_delete_actions))

    assert actions == after_delete_actions, \
        "Event ran after being deleted"


def test_delete_calendar_based_event_after_first_run():
    """
    Creates a calendar based event to run next minute.
    Deletes the event after first run.
    """
    test_case = "test_delete_calendar_based_event_after_first_run"
    log("Test Case: {}".format(test_case))

    # Actions before
    actions = BRO_CTRL.get_actions(DEFAULT_SCOPE)
    action_count_before = \
        len([x for x in actions if x["name"]
             in ["CREATE_BACKUP"]])

    now = time.time()
    run_time = time.localtime(now + 60*1)
    next_minute = time.strftime(TIME_FORMAT, run_time)
    expected_next_time = time.localtime(now + (60*24*60)+60)

    event = {
        "time": next_minute
    }

    global EVENT_ID
    EVENT_ID = BRO_CTRL.create_calendar_based_event(event)

    calendar_event = BRO_CTRL.get_calendar_based_event(EVENT_ID)
    log("Got event id: " + str(EVENT_ID))
    log("Scheduled event: " + str(calendar_event))
    assert_equal(calendar_event["time"], next_minute)

    time.sleep(80)

    # Actions after
    actions_after = BRO_CTRL.get_actions(DEFAULT_SCOPE)
    action_count_after = len([x for x in actions_after if x["name"]
                              in ["CREATE_BACKUP"]])

    # Assert only one create backup action ran
    assert action_count_after - action_count_before == 1, \
        "CREATE_BACKUP action did not run"

    config = BRO_CTRL.get_scheduler_config(
        scope=DEFAULT_SCOPE, rest_version='v4')
    log("Got scheduler config: " + str(config))
    log("Expected next scheduled time: " +
        time.strftime(DATE_FORMAT.split(".")[0], expected_next_time))

    # Get the next scheduled time with milliseconds trimmed off
    next_scheduled_time = config["nextScheduledTime"][:-1]
    log("Next scheduled time: " + next_scheduled_time)

    # Assert the next scheduled time is one day later
    assert_equal(next_scheduled_time,
                 time.strftime(DATE_FORMAT, expected_next_time))

    BRO_CTRL.delete_calendar_based_event(EVENT_ID)

    calendar_events = BRO_CTRL.get_calendar_based_events()
    assert_equal(calendar_events, [])

    config = BRO_CTRL.get_scheduler_config(
        scope=DEFAULT_SCOPE, rest_version='v4')
    log("Got scheduler config: " + str(config))

    # Get the next scheduled time
    assert config["nextScheduledTime"] is None, \
        "Incorrect next scheduled time"


def test_delete_calendar_based_event_before_first_run():
    """
    Creates a calendar based event to run next minute.
    Deletes the event before first run.
    """
    test_case = "test_delete_calendar_based_event_before_first_run"
    log("Test Case: {}".format(test_case))

    # Actions before
    actions = BRO_CTRL.get_actions(DEFAULT_SCOPE)
    action_count_before = \
        len([x for x in actions if x["name"]
             in ["CREATE_BACKUP"]])

    now = time.time()
    run_time = time.localtime(now + 60*1)
    next_minute = time.strftime(TIME_FORMAT, run_time)
    expected_next_time = time.strftime(DATE_FORMAT, run_time)

    event = {
        "time": next_minute
    }

    global EVENT_ID
    EVENT_ID = BRO_CTRL.create_calendar_based_event(event)

    calendar_event = BRO_CTRL.get_calendar_based_event(EVENT_ID)
    log("Got event id: " + str(EVENT_ID))
    log("Scheduled event: " + str(calendar_event))
    assert_equal(calendar_event["time"], next_minute)

    config = BRO_CTRL.get_scheduler_config(
        scope=DEFAULT_SCOPE, rest_version='v4')
    log("Got scheduler config: " + str(config))
    log("Expected next scheduled time: " + expected_next_time)

    # Get the next scheduled time with milliseconds trimmed off
    next_scheduled_time = config["nextScheduledTime"][:-1]
    log("Next scheduled time: " + next_scheduled_time)

    # Assert the next scheduled time is the next minute
    assert_equal(next_scheduled_time, expected_next_time)

    BRO_CTRL.delete_calendar_based_event(EVENT_ID)

    calendar_events = BRO_CTRL.get_calendar_based_events()
    assert_equal(calendar_events, [])

    time.sleep(80)

    # Actions after
    actions_after = BRO_CTRL.get_actions(DEFAULT_SCOPE)
    action_count_after = \
        len([x for x in actions_after if x["name"]
             in ["CREATE_BACKUP"]])

    # Assert only create backup action ran
    assert action_count_after - action_count_before == 0, \
        "CREATE_BACKUP action was not supposed to run"

    config = BRO_CTRL.get_scheduler_config(
        scope=DEFAULT_SCOPE, rest_version='v4')
    log("Got scheduler config: " + str(config))

    # Assert the nextScheduledTime is null
    assert config["nextScheduledTime"] is None, \
        "Incorrect next scheduled time"


def test_update_calendar_based_event():
    """
    Creates a calendar based event to run next two minutes.
    Updates a calendar to run the next minute.
    All elements are going to change
    Deletes the event before first run.
    """
    test_case = "test_update_calendar_based_event"
    log("Test Case: {}".format(test_case))

    # Actions before
    actions = BRO_CTRL.get_actions(DEFAULT_SCOPE)
    action_count_before = \
        len([x for x in actions if x["name"]
             in ["CREATE_BACKUP"]])
    # Calendar created by default is setting month 1 and day 31
    # time 2 minutes from now
    # the expected update time is 30 seconds
    update_minute, update_expected_next_time, event_id = \
        BRO_CTRL.create_calendar_update(seconds_to_change=30)
    calendar_event_initial = BRO_CTRL.get_calendar_based_event(event_id)
    event_update = {
        "time": update_minute
    }
    # Patch element keeping the month and day and date updated to 1 minute
    log("PATCH event with time: " + update_minute)
    BRO_CTRL.patch_calendar_based_event(event_id, event_update)
    calendar_event = BRO_CTRL.get_calendar_based_event(event_id)
    log("Scheduled event after PATCH: " + str(calendar_event))

    changes = compare_calendars(calendar_event_initial, calendar_event)
    # just 1 calendar element is patched
    assert_equal(len(changes), 1)
    config = BRO_CTRL.get_scheduler_config(
        scope=DEFAULT_SCOPE, rest_version='v4')
    log("Got scheduler config: " + str(config))
    log("Expected updated next scheduled time: " + update_expected_next_time)
    # Get the next scheduled time with milliseconds trimmed off
    next_scheduled_time = config["nextScheduledTime"][:-1]
    log("Next scheduled time: " + next_scheduled_time)
    assert_equal(next_scheduled_time, update_expected_next_time)

    # Put element replace all fields to default
    log("PUT event with time: " + update_minute)
    BRO_CTRL.put_calendar_based_event(event_id, event_update)
    calendar_event = BRO_CTRL.get_calendar_based_event(event_id)
    log("Scheduled event after PUT: " + str(calendar_event))
    changes = compare_calendars(calendar_event_initial, calendar_event)
    # all calendar elements are set to default
    assert len(changes) > 3
    assert_equal(calendar_event["time"], update_minute)

    update_next_time = datetime.now().strftime("%Y-%m-%dT") + update_minute

    config = BRO_CTRL.get_scheduler_config(
        scope=DEFAULT_SCOPE, rest_version='v4')
    log("Got scheduler config: " + str(config))
    log("Expected updated next scheduled time: " + update_next_time)

    # Get the next scheduled time with milliseconds trimmed off
    next_scheduled_time = config["nextScheduledTime"][:-1]
    log("Next scheduled time: " + next_scheduled_time)
    assert_equal(next_scheduled_time, update_next_time)
    BRO_CTRL.delete_calendar_based_event(event_id)
    calendar_events = BRO_CTRL.get_calendar_based_events()
    assert_equal(calendar_events, [])

    time.sleep(80)

    # Actions after
    actions_after = BRO_CTRL.get_actions(DEFAULT_SCOPE)
    action_count_after = \
        len([x for x in actions_after if x["name"]
             in ["CREATE_BACKUP"]])

    # Assert only create backup action ran
    assert action_count_after - action_count_before == 0, \
        "CREATE_BACKUP action was not supposed to run"

    config = BRO_CTRL.get_scheduler_config(
        scope=DEFAULT_SCOPE, rest_version='v4')
    log("Got scheduler config: " + str(config))

    # Assert the nextScheduledTime is null
    assert config["nextScheduledTime"] is None, \
        "Incorrect next scheduled time"


def test_calendar_event_with_start_and_stop_time():
    """
    This test creates a calendar based event with start time in the past
    and stop time in the next two minute.
    """
    test_case = "test_calendar_event_with_start_and_stop_time"
    log("Test Case: {}".format(test_case))

    # Actions before
    actions = BRO_CTRL.get_actions(DEFAULT_SCOPE)
    action_count_before = \
        len([x for x in actions if x["name"]
             in ["CREATE_BACKUP"]])

    now = time.time()
    # Stop time in next two minutes
    stop_time = time.localtime(now + 60*2)
    formatted_stop_time = time.strftime(DATE_FORMAT, stop_time)

    # start time is one day before the current time with one additional minute
    start_time = time.localtime(now - (60*24*60)+60)
    formatted_start_time = time.strftime(DATE_FORMAT, start_time)

    # Run time is the next minute
    run_time = time.localtime(now + 60*1)
    next_minute = time.strftime(TIME_FORMAT, run_time)

    event = {
        "startTime": formatted_start_time,
        "time": next_minute,
        "stopTime": formatted_stop_time
    }

    global EVENT_ID
    EVENT_ID = BRO_CTRL.create_calendar_based_event(event)

    calendar_event = BRO_CTRL.get_calendar_based_event(EVENT_ID)
    log("Got event id: " + str(EVENT_ID))
    log("Scheduled event: " + str(calendar_event))

    assert_equal(calendar_event["time"], next_minute)
    assert_equal(
        calendar_event["startTime"][:-1], formatted_start_time)
    assert_equal(
        calendar_event["stopTime"][:-1], formatted_stop_time)

    time.sleep(80)

    # Actions after
    actions_after = BRO_CTRL.get_actions(DEFAULT_SCOPE)
    action_count_after = len([x for x in actions_after if x["name"]
                              in ["CREATE_BACKUP"]])

    # Assert only one create backup action ran
    assert action_count_after - action_count_before == 1, \
        "CREATE_BACKUP action did not run"

    config = BRO_CTRL.get_scheduler_config(
        scope=DEFAULT_SCOPE, rest_version='v4')

    log("Got scheduler config: " + str(config))
    # Assert the nextScheduledTime is null
    assert config["nextScheduledTime"] is None, \
        "Incorrect next scheduled time"


def test_config_reset():
    """
    This test:
    - locks the scheduler
    - creates a periodic event
    - takes a backup
    - deletes the periodic event
    - unlocks the scheduler
    - modifies the housekeeping configuration
    - runs a configuration reset with the backup
    After the reset operation, the scheduler should be locked, with the
    periodic event present and the housekeeping configuration should be
    the same as it was before modification
    """
    test_case = "test_config_reset"
    log("Test Case: {}".format(test_case))
    # Lock the DEFAULT scheduler
    BRO_CTRL.update_scheduler_config({"adminState": "LOCKED"})

    # Create an event 2.5 hours in the future
    now = time.time()
    start_time = time.localtime(now + 60*(60+60+30))
    event_config = {
        "hours": 1,
        "startTime": time.strftime(DATE_FORMAT, start_time)
    }
    event_id = BRO_CTRL.create_scheduled_periodic_event(event_config)

    # Create the backup to be used for reset
    action_id = BRO_CTRL.create_backup("for-reset", TEST_AGENTS, NAMESPACE)
    BRO_CTRL.wait_for_action_complete(action_id, "CREATE_BACKUP", "for-reset")

    # Create another periodic event
    event_id_2 = BRO_CTRL.create_scheduled_periodic_event(event_config)

    # Delete the periodic event
    BRO_CTRL.delete_periodic_events([{"id": event_id}], "DEFAULT")

    # Assert it was deleted
    response = BRO_CTRL.get_scheduled_periodic_event(event_id)
    assert response["statusCode"] == 404, \
        "Event " + event_id + " was not deleted"

    # Unlock the scheduler
    BRO_CTRL.update_scheduler_config({"adminState": "UNLOCKED"})

    # Modify housekeeping
    housekeeping_before = BRO_CTRL.get_housekeeping_config()
    BRO_CTRL.update_housekeeping_config(max_stored_backups=123)

    # Run reset
    scope = DEFAULT_SCOPE + "-bro"
    BRO_CTRL.restore_backup("for-reset", TEST_AGENTS, NAMESPACE, scope, False)

    # Check scheduler is locked and unlock it
    assert BRO_CTRL.get_scheduler_config()["adminState"] == "LOCKED", \
        "Scheduler was not reset to LOCKED"
    # And unlock it again
    BRO_CTRL.update_scheduler_config({"adminState": "UNLOCKED"})

    # Check the periodic event exists, and delete it again
    response = BRO_CTRL.get_scheduled_periodic_event(event_id)
    assert response["id"] == event_id, \
        "Periodic event " + event_id + " was not restored"

    BRO_CTRL.delete_periodic_events([{"id": event_id}], "DEFAULT")

    # Check the periodic event 2 does not exist after restore
    response = BRO_CTRL.get_scheduled_periodic_event(event_id_2)
    assert response["statusCode"] == 404, \
        "Periodic event " + event_id + " was not removed after restore"

    # Assert housekeeping was reset
    assert BRO_CTRL.get_housekeeping_config() == housekeeping_before, \
        "Housekeeping was not reset"

    # GET housekeeping configuration
    get_housekeeping = BRO_CTRL.get_housekeeping_config(rest_version="v4")
    assert get_housekeeping == {
        "maxStoredBackups": 1, "autoDelete": "enabled"}, \
        "Unexpected housekeeping configuration received"

    # Delete the backup
    BRO_CTRL.delete_backup("for-reset", NAMESPACE)


def test_deploy_bro_agent_with_same_agentid():
    """
    Deploy a second Test Service/Agent which has the
    same AgentId as the deployed agent
    Orchestrator will reject this registration attempt.
    """
    test_case = "test_deploy_bro_agent_with_same_agentid"
    log("Test Case: {}".format(test_case))

    # Install an Agent with ID of an already registered agent
    duplicate_agent = "same-id"
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION, [duplicate_agent],
                                 NAMESPACE, enable_global_tls=False)

    # Verify that Orchestrator has all expected agents registered
    BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())

    # Collect console logs before removing agent
    KUBE.get_pod_logs(NAMESPACE, "{}-agent-0".format(duplicate_agent),
                      test_case)
    # Remove the faulty agent
    helm_release_name = bro_utils.get_service_helm_release_name(
        duplicate_agent, NAMESPACE)
    helm3procs.helm_delete_release(helm_release_name, NAMESPACE)

    # Wait for agent to undeploy
    bro_utils.wait_for_agent_to_be_removed(duplicate_agent, NAMESPACE)


def test_agent_discovery_detects_failed_pod():
    """
    Installs a test agent which fails to deploy the agent pod.
    Verifies that the Orchestrator will detect this failed pod
    and correctly fail a backup
    """
    test_case = "test_agent_discovery_detects_failed_pod"
    log("Test Case: {}".format(test_case))

    # Install an Agent which will fail to deploy
    failing_agent = "agent-pod-fails"
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION, [failing_agent],
                                 NAMESPACE, expect_failure=True,
                                 enable_global_tls=False)

    # Create a backup
    failure_message = "do not match with expected agents"
    BRO_CTRL.create_backup("expect-fail", TEST_AGENTS, NAMESPACE,
                           expected_result="FAILURE",
                           failure_info=failure_message)

    # Remove the faulty agent
    helm_release_name = bro_utils.get_service_helm_release_name(
        failing_agent, NAMESPACE)
    helm3procs.helm_delete_release(helm_release_name, NAMESPACE)

    # Wait for agent to undeploy
    bro_utils.wait_for_agent_to_be_removed(failing_agent, NAMESPACE)


def test_update_housekeeping_limits():
    """
    Updates and checks housekeeping limit of backup-managers
    and a virtual backup manager.
    Initially sets 'auto-delete' to disabled to ensure this does
    not prevent normal backup creation.
    """
    log("Test Case: test_update_housekeeping_limits")

    auto_delete = "disabled"

    # Updates housekeeping values for DEFAULT, SUBSCRIPTION and CONFIGURATION
    # backup managers
    BRO_CTRL.update_housekeeping_config(auto_delete, MAX_STORED_BACKUPS)
    BRO_CTRL.update_housekeeping_config(auto_delete, MAX_STORED_BACKUPS,
                                        SUBSCRIBER_SCOPE)
    BRO_CTRL.update_housekeeping_config(auto_delete, MAX_STORED_BACKUPS,
                                        CONFIGURATION_SCOPE)
    BRO_CTRL.update_housekeeping_config(auto_delete, MAX_STORED_BACKUPS,
                                        V4_FRAG_CM_VBRM_SCOPE)

    for scopes in [DEFAULT_SCOPE, SUBSCRIBER_SCOPE,
                   CONFIGURATION_SCOPE, V4_FRAG_CM_VBRM_SCOPE]:
        backup_manager = BRO_CTRL.get_housekeeping_config(scopes)
        assert {backup_manager["auto-delete"],
                backup_manager["max-stored-manual-backups"]} \
            == {auto_delete, MAX_STORED_BACKUPS}, \
            "Housekeeping post request failed for {} brm".format(scopes)


def test_create_backups():
    """
    This test creates multiple backups and verifies the
    backups content in the Orchestrator.
    This test does this for the scopes "DEFAULT",
    "configuration-data" and "subscriber"
    """

    log("Test Case: test_create_backups")

    # Create backup and check against all agents
    BRO_CTRL.v4_create_backup(BACKUP_LIST[0], TEST_AGENTS, NAMESPACE)

    # run backups for scope and check against the
    # filtered lists of agents
    BRO_CTRL.v4_create_backup(SUBSCRIBER_BACKUP_LIST[0],
                              SUBSCRIBER_AGENTS,
                              NAMESPACE,
                              SUBSCRIBER_SCOPE)

    BRO_CTRL.v4_create_backup(CONFIGURATION_BACKUP_LIST[0],
                              CONFIGURATION_AGENTS,
                              NAMESPACE,
                              CONFIGURATION_SCOPE)

    # Enable auto deletion again in housekeeping using REST v4
    auto_delete = "enabled"
    single_backup = 1
    BRO_CTRL.update_housekeeping_config(max_stored_backups=MAX_STORED_BACKUPS,
                                        rest_version="v4",
                                        rest_method=rest_utils.REQUESTS.patch)
    BRO_CTRL.update_housekeeping_config(auto_delete, rest_version="v4",
                                        rest_method=rest_utils.REQUESTS.patch)
    BRO_CTRL.update_housekeeping_config(auto_delete, single_backup,
                                        SUBSCRIBER_SCOPE, rest_version="v4",
                                        rest_method=rest_utils.REQUESTS.patch)
    BRO_CTRL.update_housekeeping_config(auto_delete, single_backup,
                                        CONFIGURATION_SCOPE, rest_version="v4",
                                        rest_method=rest_utils.REQUESTS.put)


def test_restore_parent_backup_using_vbrm():
    """
    This test calls restore on a parent backup using vBRM and verifies the
    restore content in the agent.
    """

    log("Test Case: test_restore_parent_backup_using_vbrm")

    BRO_CTRL.restore_backup(BACKUP_LIST[0], [V4_FRAG_CM],
                            NAMESPACE, V4_FRAG_CM_VBRM_SCOPE,
                            owns_backup=False)


def test_vbrm_create_backup():
    """
    This test creates a backup and verifies the
    backups success.
    This test does this for the vBRM scope
    "DEFAULT-v4-frag-and-metadata"
    """

    log("Test Case: test_vbrm_create_backup")

    # Create vBRM backup
    BRO_CTRL.create_backup(BACKUP_LIST[0] + V4_FRAG_CM_VBRM_SUFFIX,
                           V4_FRAG_CM_VBRM_AGENT, NAMESPACE,
                           V4_FRAG_CM_VBRM_SCOPE)

    # Assert CREATE BACKUP action success
    vbrm_backup_actions = []
    vbrm_actions = BRO_CTRL.get_actions(V4_FRAG_CM_VBRM_SCOPE)
    for vbrm_action in vbrm_actions:
        if vbrm_action["name"] in ["CREATE_BACKUP"]:
            vbrm_backup_actions.append(vbrm_action)

    vbrm_backup_action = vbrm_backup_actions[len(vbrm_backup_actions) - 1]

    assert vbrm_backup_action["result"] == "SUCCESS", \
        "CREATE_BACKUP action failed"


def test_vbrm_create_backup_with_parent_backup_name_fails():
    """
    This test verifies failure to create a vBRM backup with the
    same name as an existing parent BRM backup and vice versa.
    This test relates to BRM scope "DEFAULT"
    and vBRM scope "DEFAULT-v4-frag-and-metadata"
    """

    log("Test Case: \
        test_vbrm_create_backup_with_parent_backup_name_fails")

    # Create an action for CREATE_BACKUP with the same name as parent
    create_payload = {"backupName": BACKUP_LIST[0]}
    out = BRO_CTRL.create_action("CREATE_BACKUP", create_payload,
                                 V4_FRAG_CM_VBRM_SCOPE)
    action_id = out["id"]

    # Follow the create backup progress
    BRO_CTRL.wait_for_action_complete(action_id, "CREATE_BACKUP",
                                      BACKUP_LIST[0], V4_FRAG_CM_VBRM_SCOPE,
                                      expected_result="FAILURE")

    # Create an action for CREATE_BACKUP with the same name as child
    create_payload = {"backupName": BACKUP_LIST[0] + V4_FRAG_CM_VBRM_SUFFIX}
    out = BRO_CTRL.create_action("CREATE_BACKUP", create_payload)
    action_id = out["id"]

    # Follow the create backup progress
    BRO_CTRL.wait_for_action_complete(action_id, "CREATE_BACKUP",
                                      BACKUP_LIST[0] + V4_FRAG_CM_VBRM_SUFFIX,
                                      expected_result="FAILURE")


def test_export_parent_backup_from__vbrm_scope_fails():
    """
    This test verifies that exporting a parent's backup
    from the vBRM scope fails
    """
    log("Test Case: test_export_parent_backup_from__vbrm_scope_fails")

    backup = BACKUP_LIST[0]

    # Creates the payload for export.
    sftp_payload = SFTP_SERVER.get_sftp_payload(SFTP_PATH)
    sftp_payload.update({"backupName": backup})

    out = BRO_CTRL.create_action("EXPORT",
                                 payload=sftp_payload,
                                 scope=V4_FRAG_CM_VBRM_SCOPE,
                                 raise_for_status=False,
                                 action_created=False)

    assert "Failed to create action" in out["message"]


def test_export_vbrm_backup():
    """
    This test verifies that a vBRM's backup can be exported
    from the vBRM scope
    """
    log("Test Case: \
        test_export_vbrm_backup")

    backup = BACKUP_LIST[0] + V4_FRAG_CM_VBRM_SUFFIX

    # Creates the payload for export.
    sftp_payload = SFTP_SERVER.get_sftp_payload(SFTP_PATH)
    sftp_payload.update({"backupName": backup})
    action_id = BRO_CTRL.export_backup(
        backup_name=backup,
        payload=sftp_payload,
        scope=V4_FRAG_CM_VBRM_SCOPE)
    assert BRO_CTRL.get_action_result(action_id, "EXPORT",
                                      scope=V4_FRAG_CM_VBRM_SCOPE) == "SUCCESS"
    assert (backup in BRO_CTRL.get_action_additionalInfo(
        action_id, "EXPORT", scope=V4_FRAG_CM_VBRM_SCOPE))


def test_import_vbrm_backup():
    """
    This test verifies that a vBRM backup can be imported from the SFTP server
    using the vBRM scope.
    """
    log("Test Case: test_import_vbrm_backup")

    global BACKUP_TARBALL_NAME
    BACKUP_TARBALL_NAME = BRO_CTRL.generate_backup_tarball_name(
        BACKUP_LIST[0] + V4_FRAG_CM_VBRM_SUFFIX, scope=V4_FRAG_CM_VBRM_SCOPE)

    BRO_CTRL.import_backup_with_existing_backup_deletion(
        backup_name=BACKUP_LIST[0] + V4_FRAG_CM_VBRM_SUFFIX,
        remote_backup_name=BACKUP_TARBALL_NAME,
        namespace=NAMESPACE,
        sftp_path=SFTP_PATH,
        sftp_server=SFTP_SERVER,
        scope=V4_FRAG_CM_VBRM_SCOPE)


def test_restore_vbrm_backup():
    """
    This test calls restore on a vBRM and verifies the
    restore content in the agent.
    """

    log("Test Case: test_restore_vbrm_backup")

    BRO_CTRL.restore_backup(BACKUP_LIST[0] + V4_FRAG_CM_VBRM_SUFFIX,
                            V4_FRAG_CM_VBRM_AGENT, NAMESPACE,
                            V4_FRAG_CM_VBRM_SCOPE)


def test_delete_parent_backup_from__vbrm_scope_fails():
    """
    This test verifies a parents backup cannot be deleted
    from the vBRM scope.
    """
    log("Test Case: test_delete_parent_backup_from__vbrm_scope_fails")

    backup = BACKUP_LIST[0]

    # Create an action for DELETE_BACKUP
    delete_payload = {"backupName": backup}
    out = BRO_CTRL.create_action("DELETE_BACKUP", delete_payload,
                                 V4_FRAG_CM_VBRM_SCOPE)
    action_id = out["id"]

    # Follow the delete backup progress
    BRO_CTRL.wait_for_action_complete(action_id, "DELETE_BACKUP",
                                      BACKUP_LIST[0], V4_FRAG_CM_VBRM_SCOPE,
                                      expected_result="FAILURE")


def test_export_backup_to_http_fail():
    """
    This test uses a single backup and export the
    content into an external HTTP server.
    It must fail due this functionality was deprecated
    """
    log("Test Case: test_export_backup_to_http_fail")
    # Creates the payload for export.
    # export requires the "backupName" parameter
    http_payload = {"uri": "http://127.0.0.1:1001/bro/default/"}
    http_payload.update({"backupName": BACKUP_LIST[0]})
    log("Exporting {}".format(BACKUP_LIST[0]))
    action_id = BRO_CTRL.export_backup(
        backup_name=BACKUP_LIST[0],
        payload=http_payload,
        expected_result="FAILURE")
    assert BRO_CTRL.get_action_result(action_id, "EXPORT") == "FAILURE"
    assert ("http import/export is not supported"
            in BRO_CTRL.get_action_additionalInfo(action_id, "EXPORT"))


def test_export_backup():
    """
    This test uses a single backup and export the
    content into an external SFTP server.
    """
    log("Test Case: test_export_backup")

    # Creates the payload for export.
    sftp_payload = SFTP_SERVER.get_sftp_payload(SFTP_PATH)
    log("Exporting {}".format(BACKUP_LIST[0]))
    action_id = BRO_CTRL.v4_export_backup(
        backup_name=BACKUP_LIST[0],
        payload=sftp_payload)

    log("Export task ID: {}".format(action_id))

    # GET all exports
    all_exports = BRO_CTRL.v4_get_exports(backup_name=BACKUP_LIST[0])
    values_in_all_exports = [elem.get("id") for elem in all_exports]
    assert action_id in values_in_all_exports, \
        "Export id {} is not in the export list" \
        .format(action_id)

    # Get Export by ID
    export_by_id = BRO_CTRL.v4_get_exports_by_id(
        backup_name=BACKUP_LIST[0],
        export_id=action_id)
    assert export_by_id["task"]["result"] == "SUCCESS", \
        "Export found does not have the expected result"

    assert BRO_CTRL.get_action_result(action_id, "EXPORT") == "SUCCESS"

    # GET last-task executed
    last_task = BRO_CTRL.v4_get_last_task()
    assert last_task["resource"] == \
        "/backup-restore/v4/backup-managers/{}/backups/{}/exports" \
        .format(DEFAULT_SCOPE, BACKUP_LIST[0]), \
        "Last executed task is not the expected task"

    assert (BACKUP_LIST[0] in BRO_CTRL.get_action_additionalInfo(
        action_id, "EXPORT"))


def test_overwrite_export():
    """
    This test will try to overwrite a previously exported backup.

    The test must throw an exception showing the error.
    No action_id is generated due the nature of the failure.
    """
    log("Test Case: test_overwrite_export")

    sftp_payload = SFTP_SERVER.get_sftp_payload(SFTP_PATH)
    sftp_payload.update({"backupName": BACKUP_LIST[0]})
    log("Exporting {} in a previous exported backup".
        format(BACKUP_LIST[0]))
    action_id = BRO_CTRL.export_backup(
        backup_name=BACKUP_LIST[0],
        payload=sftp_payload,
        expected_result="FAILURE")
    assert BRO_CTRL.get_action_result(action_id, "EXPORT") == "FAILURE", \
        "Invalid export, cant override a previous backup {}"\
        .format(BACKUP_LIST[0])


def test_import_backup():
    """
     Imports a backup from a sftp server.
    """
    log("Test Case: test_import_backup")
    log("Deletes backup {} if exist".format(BACKUP_LIST[0]))

    global BACKUP_TARBALL_NAME
    BACKUP_TARBALL_NAME = BRO_CTRL.get_backup_tarball_name(BACKUP_LIST[0])

    if BRO_CTRL.backup_exists(BACKUP_LIST[0]):
        BRO_CTRL.delete_backup(BACKUP_LIST[0], NAMESPACE)
        BRO_CTRL.wait_for_backup_to_be_removed(BACKUP_LIST[0])

    log("Import {}".format(BACKUP_TARBALL_NAME))

    # Creates the payload for import.
    path_remote = SFTP_PATH + DEFAULT_SCOPE + "/" + BACKUP_TARBALL_NAME
    sftp_payload = SFTP_SERVER.get_sftp_payload(path_remote)

    action_id = BRO_CTRL.v4_import_backup(
        backup_name=BACKUP_LIST[0],
        payload=sftp_payload)

    log("Import task ID: {}".format(action_id))

    # GET all imports
    all_imports = BRO_CTRL.v4_get_imports()
    values_in_all_imports = [elem.get("id") for elem in all_imports]
    assert action_id in values_in_all_imports, \
        "Import ID {} is not in the import list" \
        .format(action_id)

    # Get Import by ID
    import_by_id = BRO_CTRL.v4_get_imports_by_id(import_id=action_id)
    assert import_by_id["task"]["result"] == "SUCCESS", \
        "Import found does not have the expected result"

    # GET last-task
    last_task = BRO_CTRL.v4_get_last_task()
    assert last_task["resource"] == \
        "/backup-restore/v4/backup-managers/{}/imports" \
        .format(DEFAULT_SCOPE), \
        "Last executed task is not the expected task"
    log("Import task ID: {}".format(action_id))

    # Get all tasks
    all_tasks = BRO_CTRL.v4_get_tasks()
    values_in_all_tasks = [elem.get("id") for elem in all_tasks]
    assert action_id in values_in_all_tasks, \
        "Import ID {} is not in the tasks list" \
        .format(action_id)

    # Get task by ID
    task_by_id = BRO_CTRL.v4_get_tasks_by_id(task_id=action_id)
    assert task_by_id["resource"] == \
        "/backup-restore/v4/backup-managers/{}/imports" \
        .format(DEFAULT_SCOPE), \
        "Last executed task is not the expected task"

    assert BRO_CTRL.backup_exists(BACKUP_LIST[0]), \
        "Backup {} has not been imported ".format(BACKUP_LIST[0])


def test_create_backups_using_v2_endpoint():
    """
    This test creates multiple backups using v2 endpoint and verifies the
    backups content in the Orchestrator.
    This test does this for the scopes "DEFAULT",
    "configuration-data" and "subscriber"
    """

    log("Test Case: test_create_backups_using_v2_endpoint")

    # Create backup and check against all agents
    BRO_CTRL.create_backup_yang_action(BACKUP_LIST[1], TEST_AGENTS, NAMESPACE)

    # run backups for scope and check against the
    # filtered lists of agents
    BRO_CTRL.create_backup_yang_action(SUBSCRIBER_BACKUP_LIST[1],
                                       SUBSCRIBER_AGENTS,
                                       NAMESPACE,
                                       SUBSCRIBER_SCOPE)

    BRO_CTRL.create_backup_yang_action(CONFIGURATION_BACKUP_LIST[1],
                                       CONFIGURATION_AGENTS,
                                       NAMESPACE,
                                       CONFIGURATION_SCOPE)


def test_update_backup():
    """
    This test verifies that backup description can be updated.
    In this case a label is added.
    """

    log("Test Case: test_update_backup")

    # Select a backup from the backup list
    backup_name = random.choice(BACKUP_LIST)
    BRO_CTRL.update_backup(backup_name, "OSS")

    backup = BRO_CTRL.get_backup(backup_name)
    assert backup["userLabel"] == "OSS", "Backup not updated with label"


def test_restore_backups():
    """
    This test calls restore multiple times and verifies the
    restore content in the agent.
    """

    log("Test Case: test_restore_backups")

    action_id = BRO_CTRL.v4_restore_backup(BACKUP_LIST[0], TEST_AGENTS,
                                           NAMESPACE)

    BRO_CTRL.v4_restore_backup(SUBSCRIBER_BACKUP_LIST[1], SUBSCRIBER_AGENTS,
                               NAMESPACE, SUBSCRIBER_SCOPE)

    BRO_CTRL.v4_restore_backup(CONFIGURATION_BACKUP_LIST[1],
                               CONFIGURATION_AGENTS, NAMESPACE,
                               CONFIGURATION_SCOPE)

    log("Restore task ID: {}".format(action_id))

    # GET all restores
    all_restores = BRO_CTRL.v4_get_restores(backup_name=BACKUP_LIST[0])
    values_in_all_restores = [elem.get("id") for elem in all_restores]
    assert action_id in values_in_all_restores, \
        "Restore ID {} is not in the restore list" \
        .format(action_id)

    # Get restore by ID
    restore_by_id = BRO_CTRL.v4_get_restores_by_id(
        backup_name=BACKUP_LIST[0], restore_id=action_id)
    assert restore_by_id["task"]["result"] == "SUCCESS", \
        "Restore found does not have the expected result"

    # GET last-task
    last_task = BRO_CTRL.v4_get_last_task()
    assert last_task["resource"] == \
        "/backup-restore/v4/backup-managers/{}/backups/{}/restores" \
        .format(DEFAULT_SCOPE, BACKUP_LIST[0]), \
        "Last executed task is not the expected task"


def test_restore_backups_using_v2_endpoints():
    """
    This test calls restore multiple times and verifies the
    restore content in the agent.
    """

    log("Test Case: test_restore_backups_using_v2_endpoints")

    BRO_CTRL.restore_backup_yang_action(BACKUP_LIST[1], TEST_AGENTS, NAMESPACE)

    BRO_CTRL.restore_backup_yang_action(SUBSCRIBER_BACKUP_LIST[1],
                                        SUBSCRIBER_AGENTS,
                                        NAMESPACE, SUBSCRIBER_SCOPE)

    BRO_CTRL.restore_backup_yang_action(CONFIGURATION_BACKUP_LIST[1],
                                        CONFIGURATION_AGENTS,
                                        NAMESPACE, CONFIGURATION_SCOPE)


def test_create_backup_agent_failure():
    """
    This test verifies that the create-backup action will fail if one
    of the agents returns a failure message.
    """

    test_case = "test_create_backup_agent_failure"
    log("Test Case: {}".format(test_case))

    # Install a Test Agent which will fail a backup
    agent_name = "fails-backup"
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION, [agent_name],
                                 NAMESPACE, enable_global_tls=False)
    expected_agents = get_agent_ids()
    expected_agents.append('fails-backup')
    BRO_CTRL.wait_for_bro_agents_to_reconnect(expected_agents)

    # Create an action for CREATE_BACKUP
    backup = "expect-failure"
    create_payload = {"backupName": backup}
    out = BRO_CTRL.create_action("CREATE_BACKUP", create_payload)
    action_id = out["id"]

    # Follow the create backup progress
    BRO_CTRL.wait_for_action_complete(action_id, "CREATE_BACKUP", backup,
                                      expected_result="FAILURE")
    # Get the failed backup
    out = BRO_CTRL.get_backup(backup)

    assert {out["name"], out["creationType"],
            out["status"], out["id"]} == {backup, "MANUAL",
                                          "CORRUPTED", backup}

    log("Attempt to restore the CORRUPTED backup")
    failure_message = "Cannot restore backup <{}>, status - CORRUPTED". \
        format(backup)
    restore_payload = {"backupName": backup}
    out = BRO_CTRL.create_action("RESTORE", restore_payload)

    # Follow the restore progress
    BRO_CTRL.wait_for_action_complete(out["id"], "RESTORE", backup,
                                      expected_result="FAILURE",
                                      failure_info=failure_message)

    # Collect console logs before removing agent
    KUBE.get_pod_logs(NAMESPACE, "{}-agent-0".format(agent_name), test_case)

    # Remove the Test Agent
    helm_release_name = bro_utils.get_service_helm_release_name(agent_name,
                                                                NAMESPACE)
    helm3procs.helm_delete_release(helm_release_name, NAMESPACE)
    bro_utils.wait_for_agent_to_be_removed(agent_name, NAMESPACE)


def test_housekeeping_config_update():
    """
    Updates housekeeping configuration and ensures that backups are removed
    in the correct order of failed first and then oldest.
    """
    log("Test Case: test_housekeeping_config_update")

    # At this stage DEFAULT has 2 successful backups and 2 newer failed backups
    # Update housekeeping limit to 2
    auto_delete = "disabled"
    max_backups = 2
    BRO_CTRL.update_housekeeping_config(auto_delete, max_backups)

    # Verify that the failed backups were removed
    for failed_backup in ["expect-failure", "channel-failure-backup"]:
        BRO_CTRL.verify_backup_is_deleted(failed_backup, NAMESPACE)

    # Update housekeeping limit to 1
    auto_delete = "enabled"
    max_backups = 1
    BRO_CTRL.update_housekeeping_config(auto_delete, max_backups)

    # Verify that the oldest backup has been removed.
    BRO_CTRL.verify_backup_is_deleted(BACKUP_LIST[0], NAMESPACE)
    assert BRO_CTRL.backup_exists(BACKUP_LIST[1]) is True, \
        "Oldest Backup was not removed by housekeeping"


def test_import_backup_after_housekeeping():
    """
    Imports a backup from the sftp server.
    Verifies housekeeping handling at import backup.
    """
    log("Test Case: test_import_backup_after_housekeeping")

    log("Import {}".format(BACKUP_LIST[0]))

    # Creates the payload for import.
    # Import does not require the "backupName" parameter
    path_remote = SFTP_PATH + DEFAULT_SCOPE + "/" + BACKUP_TARBALL_NAME
    sftp_payload = SFTP_SERVER.get_sftp_payload(path_remote)

    BRO_CTRL.import_backup(
        backup_name=BACKUP_LIST[0],
        payload=sftp_payload)

    assert BRO_CTRL.backup_exists(BACKUP_LIST[0]), \
        "Backup {} has not been imported ".format(BACKUP_LIST[0])

    # Verify housekeeping at import
    # Verify the existing backup was deleted
    BRO_CTRL.verify_backup_is_deleted(BACKUP_LIST[1], NAMESPACE)
    # And verify that the imported backup is the only backup in the BRM
    backups = BRO_CTRL.get_backups()
    assert len(backups) == 1, \
        "The existing backups are not deleted and the " \
        "number of backups exceeded the limit"

    SFTP_SERVER.stop_sftp_server()


def test_delete_bro_pod():
    """
    Deletes the BRO pod and ensures it comes back up running.

    The BRO Test Agent detects the channel is down and will
    continually try to re-establish the GRPC connection until successful.
    """
    test_case = "test_delete_bro_pod"
    log("Test Case: {}".format(test_case))

    # List status of the helm release
    # (commented for helm3 issue, to be fixed in ADPPRG-40833)
    # bro_utils.get_helm_release_status(BRO_HELM_RELEASE_NAME)

    # Fetch metadata stored in Orchestrator
    metadata_pre = BRO_CTRL.get_orchestrator_metadata()
    # Fetch the backup data
    path = "{}/{}".format(BACKUP_LOCATION, DEFAULT_SCOPE)
    files_pre = bro_data.get_container_files(path, BRO_POD_NAME, NAMESPACE,
                                             container=BRO_NAME)

    # Collect console logs before restarting orchestrator
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, "pre_{}".format(test_case))

    start_time = time.perf_counter()
    bro_utils.restart_pod(BRO_POD_NAME, NAMESPACE)
    restart_time = bro_utils.calc_time(start_time, BRO_POD_NAME, NAMESPACE)
    log("TEST REPORT: Restart Time: {}s".format(restart_time))

    # List status of helm release
    # (commented for helm3 issue, to be fixed in ADPPRG-40833)
    # bro_utils.get_helm_release_status(BRO_HELM_RELEASE_NAME)

    # Verify Orchestrator status and check registered agents
    BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())

    # Fetch metadata stored in Orchestrator
    metadata_post = BRO_CTRL.get_orchestrator_metadata()

    # Verify that the Orchestrator metadata has been persisted
    assert sorted([sorted(i) for i in metadata_pre]) \
        == sorted([sorted(i) for i in metadata_post]), \
        "Metadata has not been persisted"

    # Fetch the backup data
    files_post = bro_data.get_container_files(path, BRO_POD_NAME, NAMESPACE,
                                              container=BRO_NAME)
    assert files_pre == files_post, "Backup data has not been persisted"


def test_housekeeping_disabled():
    """
    This test performs a create-backup action and checks if it is rejected when
    auto-delete is false.

    It sets auto-delete to true afterwards and performs a create-backup action
    and verifies backup is created and housekeeping activities are performed.
    """
    log("Test Case: test_housekeeping_disabled")

    # Get the backups in the default backup-manager
    backups = BRO_CTRL.get_backups()

    # Update housekeeping limit to current number of backups and set
    # auto-delete to false
    BRO_CTRL.update_housekeeping_config("disabled", len(backups))

    # Create an action for CREATE_BACKUP
    backup = "TooManyBackups"
    create_payload = {"backupName": backup}
    out = BRO_CTRL.create_action("CREATE_BACKUP", create_payload)
    action_id = out["id"]

    # Follow the create backup progress
    BRO_CTRL.wait_for_action_complete(action_id, "CREATE_BACKUP", backup,
                                      expected_result="FAILURE")

    assert BRO_CTRL.backup_exists(backup) is False, "create-backup action is" \
                                                    "not rejected"

    # Set auto-delete to true
    BRO_CTRL.update_housekeeping_config("enabled", len(backups))

    # Create a backup
    BRO_CTRL.create_backup(backup, TEST_AGENTS, NAMESPACE)

    # Get the backups in the default backup-manager
    backups = BRO_CTRL.get_backups()

    # Get housekeeping values for DEFAULT scope
    backup_manager = BRO_CTRL.get_housekeeping_config()

    assert {BRO_CTRL.backup_exists(backup), len(backups)} == \
           {True, backup_manager["max-stored-manual-backups"]}, \
        "Backup has not been created"


def test_restore_backup_agent_failure():
    """
    This test verifies that the restore action will fail if one
    of the agents returns a failure message.
    """
    test_case = "test_restore_backup_agent_failure"
    log("Test Case: {}".format(test_case))

    failsrestore = {"AgentId": "fails-restore",
                    "fragments": [{"fragmentId": "fails-restore_1",
                                   "customData": True}]}

    # Install a Test Agent which can create a backup but will fail a restore
    agent_name = failsrestore["AgentId"]
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION, [agent_name],
                                 NAMESPACE, enable_global_tls=False)

    expected_agents = get_agent_ids()
    expected_agents.append('fails-restore')
    BRO_CTRL.wait_for_bro_agents_to_reconnect(expected_agents)

    # Create a backup
    backup = "restore-failure"

    TEST_AGENTS.append(failsrestore)
    BRO_CTRL.create_backup(backup, TEST_AGENTS, NAMESPACE)

    # Create an action for RESTORE
    restore_payload = {"backupName": backup}
    out = BRO_CTRL.create_action("RESTORE", restore_payload)
    action_id = out["id"]

    # Follow the restore backup progress
    BRO_CTRL.wait_for_action_complete(action_id, "RESTORE", backup,
                                      expected_result="FAILURE")

    # Collect console logs before removing agent
    KUBE.get_pod_logs(NAMESPACE, "{}-agent-0".format(agent_name), test_case)

    # Remove the Test Agent
    helm_release_name = bro_utils.get_service_helm_release_name(
        agent_name, NAMESPACE)
    helm3procs.helm_delete_release(helm_release_name, NAMESPACE)
    bro_utils.wait_for_agent_to_be_removed(agent_name, NAMESPACE)
    TEST_AGENTS.remove(failsrestore)


def test_delete_backup_using_v2_endpoint():
    """
    This test verifies that a backup can be deleted.
    Uses v2 endpoint.
    """
    log("Test Case: test_delete_backup_using_v2_endpoint")

    backup = "restore-failure"
    BRO_CTRL.delete_backup_yang_action(backup, NAMESPACE)


def test_agent_connection_failure_restore():
    """
    This test verifies that the restore action will fail if one
    of the agents participating in restore closes the control
    channel during restore.
    """

    test_case = "test_agent_connection_failure_restore"
    log("Test Case: {}".format(test_case))

    agent_dies_restore = {"AgentId": "agent-dies-restore",
                          "fragments": [{"fragmentId": "agent-dies-restore_1",
                                         "customData": True}]}

    # Install a Test Agent which will close connection during restore
    agent_name = agent_dies_restore["AgentId"]
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION, [agent_name],
                                 NAMESPACE, enable_global_tls=False)

    expected_agents = get_agent_ids()
    expected_agents.append('agent-dies-restore')
    BRO_CTRL.wait_for_bro_agents_to_reconnect(expected_agents)

    # Create a backup
    backup = "channel-failure"

    TEST_AGENTS.append(agent_dies_restore)
    BRO_CTRL.create_backup(backup, TEST_AGENTS, NAMESPACE)

    # Create an action for RESTORE
    restore_payload = {"backupName": backup}
    out = BRO_CTRL.create_action("RESTORE", restore_payload)
    action_id = out["id"]

    # Follow the restore backup progress
    BRO_CTRL.wait_for_action_complete(action_id, "RESTORE", backup,
                                      expected_result="FAILURE")

    # Collect console logs before removing agent
    KUBE.get_pod_logs(NAMESPACE, "{}-agent-0".format(agent_name),
                      test_case)

    # Remove the Test Agent
    helm_release_name = bro_utils.get_service_helm_release_name(
        agent_name, NAMESPACE)
    helm3procs.helm_delete_release(helm_release_name, NAMESPACE)
    bro_utils.wait_for_agent_to_be_removed(agent_name, NAMESPACE)
    TEST_AGENTS.remove(agent_dies_restore)


def test_delete_backup():
    """
    This test verifies that a backup can be deleted.
    Uses v4 delete backup endpoint.
    """
    log("Test Case: test_delete_backup")

    backup = "channel-failure"
    BRO_CTRL.v4_delete_backup(backup, NAMESPACE)


def test_product_exact_match_missing_configmap_restore_failure():
    """
    Upgrades the Orchestrator Service with productMatchType as
    EXACT_MATCH and appProductInfoConfigMap with configmap name and
    verify if restore fails due to missing configmap
    """
    test_case = "test_product_exact_match_type_restore_failure"
    log("Test Case: {}".format(test_case))

    log("Fetch BRO snapshot chart for upgrade")
    helm3procs.helm_get_chart_from_repo(
        BRO_NAME, BRO_SNAP_VERSION, helm_repo_name=BRO_SNAP_REPO_NAME)

    log(
        "Initiate upgrade with productMatchType set to EXACT_MATCH" +
        "and appProductInfoConfigMap set")
    helm3procs.helm_upgrade_with_chart_repo_with_dict(
        BRO_HELM_RELEASE_NAME,
        BRO_NAME,
        NAMESPACE,
        BRO_SNAP_REPO_NAME,
        BRO_SNAP_VERSION,
        settings_dict={"bro.enableAgentDiscovery": "true",
                       "global.pullSecret": "armdocker",
                       "global.security.tls.enabled": "false",
                       "bro.productMatchType": "EXACT_MATCH",
                       "bro.appProductInfoConfigMap": BRO_PRODUCT_CONFIGMAP},
        timeout=120)

    bro_utils.wait_for_resources_to_deploy(BRO_HELM_RELEASE_NAME, NAMESPACE)

    # Verify Orchestrator status and check registered agents
    BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())

    # Create an action for RESTORE
    backup = CONFIGURATION_BACKUP_LIST[1]
    restore_payload = {"backupName": backup}
    out = BRO_CTRL.create_action(
        "RESTORE", restore_payload, CONFIGURATION_SCOPE)
    action_id = out["id"]

    # Follow the restore backup progress
    BRO_CTRL.wait_for_action_complete(action_id, "RESTORE", backup,
                                      CONFIGURATION_SCOPE,
                                      expected_result="FAILURE")


def test_product_list_match_type_restore_failure():
    """
    Upgrades the Orchestrator Service with productMatchType as
    LIST and verify if restore fails due to empty match list
    """
    test_case = "test_product_exact_match_type_restore_failure"
    log("Test Case: {}".format(test_case))

    log("Fetch BRO snapshot chart for upgrade")
    helm3procs.helm_get_chart_from_repo(
        BRO_NAME, BRO_SNAP_VERSION, helm_repo_name=BRO_SNAP_REPO_NAME)

    log("Initiate upgrade with productMatchType as LIST")
    helm3procs.helm_upgrade_with_chart_repo_with_dict(
        BRO_HELM_RELEASE_NAME,
        BRO_NAME,
        NAMESPACE,
        BRO_SNAP_REPO_NAME,
        BRO_SNAP_VERSION,
        settings_dict={"bro.enableAgentDiscovery": "true",
                       "global.pullSecret": "armdocker",
                       "global.security.tls.enabled": "false",
                       "bro.productMatchType": "LIST",
                       "bro.immediateFailedBackupDeletion": "true"},
        timeout=120)

    bro_utils.wait_for_resources_to_deploy(BRO_HELM_RELEASE_NAME, NAMESPACE)

    # Verify Orchestrator status and check registered agents
    BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())

    # Create an action for RESTORE
    backup = SUBSCRIBER_BACKUP_LIST[1]
    restore_payload = {"backupName": backup}
    out = BRO_CTRL.create_action("RESTORE", restore_payload, SUBSCRIBER_SCOPE)
    action_id = out["id"]

    # Follow the restore backup progress
    BRO_CTRL.wait_for_action_complete(action_id, "RESTORE", backup,
                                      SUBSCRIBER_SCOPE,
                                      expected_result="FAILURE")


def test_agent_connection_failure_backup_and_auto_delete():
    """
    This test verifies that the create-backup action will fail if one
    of the agents participating in backup closes the control
    channel during backup. It also verifies the backup is deleted
    as the service is deployed with failed backup deletion enabled
    """

    test_case = "test_agent_connection_failure_backup"
    log("Test Case: {}".format(test_case))

    # Install a Test Agent which will close connection during backup
    agent_name = "agent-dies-backup"
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION, [agent_name],
                                 NAMESPACE, enable_global_tls=False)

    expected_agents = get_agent_ids()
    expected_agents.append('agent-dies-backup')
    BRO_CTRL.wait_for_bro_agents_to_reconnect(expected_agents)

    # Create an action for CREATE_BACKUP
    backup = "channel-failure-backup"
    create_payload = {"backupName": backup}
    out = BRO_CTRL.create_action("CREATE_BACKUP", create_payload)
    action_id = out["id"]

    # Follow the create backup progress
    BRO_CTRL.wait_for_action_complete(action_id, "CREATE_BACKUP", backup,
                                      expected_result="FAILURE")

    # Collect console logs before removing agent
    KUBE.get_pod_logs(NAMESPACE, "{}-agent-0".format(agent_name),
                      test_case)

    # Remove the Test Agent
    helm_release_name = bro_utils.get_service_helm_release_name(agent_name,
                                                                NAMESPACE)
    helm3procs.helm_delete_release(helm_release_name, NAMESPACE)
    bro_utils.wait_for_agent_to_be_removed(agent_name, NAMESPACE)

    # Verify backup is deleted as auto deletion of failed backups is enabled
    BRO_CTRL.verify_backup_is_deleted(backup, NAMESPACE)

    # Create backup again check it is successful
    out = BRO_CTRL.create_action("CREATE_BACKUP", create_payload)
    action_id = out["id"]

    # Follow the create backup progress
    BRO_CTRL.wait_for_action_complete(action_id, "CREATE_BACKUP", backup,
                                      expected_result="SUCCESS")


def test_delete_test_agent():
    """
    Deletes a test agent pod.
    Verifies that the agent successfully registers again.
    """
    test_case = "test_delete_test_agent"
    log("Test Case: {}".format(test_case))

    # Select an agent from the agent list
    agent_name = random.choice(get_agent_ids())
    log("Restart agent {}".format(agent_name))

    # List status of the helm release
    # helm_release_name = bro_utils.get_service_helm_release_name(agent_name,
    #                                                            NAMESPACE)
    # (commented for helm3 issue, to be fixed in ADPPRG-40833)
    # bro_utils.get_helm_release_status(helm_release_name)

    log("Delete the test agent pod for agent {}".format(agent_name))
    pod = "{}-agent-0".format(agent_name)

    # Collect console logs before restarting agent
    KUBE.get_pod_logs(NAMESPACE, pod, "pre_{}".format(test_case))

    bro_utils.restart_pod(pod, NAMESPACE)

    # List status of helm release
    # bro_utils.get_helm_release_status(helm_release_name)

    # Verify that the test agent re-registers on start up
    BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())


def test_agent_deployed_before_orch():
    """
    Deploys a V3 and V4 test agent before the orchestrator.
    Verifies the test agent registers successfully after
    the orchestrator is deployed.

    The BRO_LONGJUMP_VERSION of the BRO chart is deployed here
    so it can be used as the source state for the subsequent upgrade test.
    """
    test_case = "test_agent_deployed_before_orch"
    log("Test Case: {}".format(test_case))

    # Collect console logs before clean down
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME,
                      "pre_{}".format(test_case))
    get_agent_pod_logs("pre_{}".format(test_case))

    log("Remove installed services and k8s resources")
    bro_utils.remove_namespace_resources(NAMESPACE)

    log("Wait for existing pods to terminate")
    pod_name = 'test-nose-auto-nightly'
    KUBE.wait_for_all_pods_to_terminate(NAMESPACE,
                                        exclude_pods_list=[pod_name],
                                        counter=10)

    global TEST_AGENTS
    TEST_AGENTS = [V3_FRAG_CM, V4_FRAG_CM]

    # Deploy the test agents
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 "6.4.0-18",
                                 [V3_AGENTS[0]["AgentId"]],
                                 NAMESPACE, enable_global_tls=False)

    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION, [V4_AGENTS[0]["AgentId"]],
                                 NAMESPACE, enable_global_tls=False)

    # Deploy Orchestrator
    log("Add the BRO PRA Repo")

    helm3procs.add_helm_repo(BRO_PRA_REPO, BRO_PRA_REPO_NAME)
    log("Get latest pra chart version")
    helm3procs.helm_get_chart_from_repo(BRO_NAME, BRO_LONGJUMP_VERSION,
                                        helm_repo_name=BRO_PRA_REPO_NAME)

    bro_utils.install_service_chart(BRO_PRA_REPO_NAME, BRO_NAME,
                                    BRO_LONGJUMP_VERSION, NAMESPACE,
                                    enable_global_tls=False)

    # Verify Orchestrator status and check registered agents
    BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())
    BRO_CTRL.check_backup_manager_is_created(expected_number_of_managers=2)

    SFTP_SERVER.start_sftp_server()


def test_export_backup_to_sftp_as_legacy():
    """"
    Tests will export a backup to SFTP server
    as a legacy backup
    """
    test_case = "test_export_backup_to_sftp_as_legacy"
    log("Test Case: {}".format(test_case))

    BRO_CTRL.create_backup(BACKUP_LIST[0], TEST_AGENTS, NAMESPACE)

    SFTP_SERVER.wait_to_start()
    sftp_payload = SFTP_SERVER.get_sftp_payload(SFTP_PATH)
    sftp_payload.update({"backupName": BACKUP_LIST[0]})

    log("Exporting {}".format(BACKUP_LIST[0]))
    action_id = BRO_CTRL.export_backup(
        backup_name=BACKUP_LIST[0],
        payload=sftp_payload)

    assert BRO_CTRL.get_action_result(action_id, "EXPORT",
                                      rest_version="v1") == "SUCCESS"

    # Need to be added again after fix is available in PRA version.
    # assert (BACKUP_LIST[0] in BRO_CTRL.get_action_additionalInfo(
    #    action_id, "EXPORT"))


def test_upgrade_bro_lj_pra_to_dev_snapshot():
    """
    Upgrades BRO longjump PRA to the dev snapshot version
    """
    test_case = "test_upgrade_bro_lj_pra_to_dev_snapshot"
    log("Test Case: {}".format(test_case))

    # Collect console logs before upgrade
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME,
                      "pre_{}".format(test_case))

    log("Fetch BRO snapshot chart for upgrade")
    helm3procs.helm_get_chart_from_repo(
        BRO_NAME, BRO_SNAP_VERSION, helm_repo_name=BRO_SNAP_REPO_NAME)

    log("Initiate upgrade")
    start_time = time.perf_counter()
    helm3procs.helm_upgrade_with_chart_archive_with_dict(
        BRO_HELM_RELEASE_NAME,
        BRO_SNAP_ARCHIVE,
        NAMESPACE,
        settings_dict={
            "global.security.tls.enabled": "false",
            "global.pullSecret": "armdocker",
            "bro.enableAgentDiscovery": "true",
            "bro.vBRMAutoCreate": "DEFAULT"},
        timeout=120)
    upgrade_time = bro_utils.calc_time(start_time, BRO_POD_NAME, NAMESPACE)
    log("TEST REPORT: Upgrade Time: {}s".format(upgrade_time))

    bro_utils.wait_for_resources_to_deploy(BRO_HELM_RELEASE_NAME, NAMESPACE)

    # Verify Orchestrator status and check registered agents
    BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())


def test_export_vbrm_backup_post_upgrade_to_dev():
    """
    This test verifies that a vBRM's backup can be exported
    from the vBRM scope
    """
    log("Test Case: \
        test_export_vbrm_backup_post_upgrade_to_dev")
    backup = BACKUP_LIST[0] + V3_FRAG_CM_VBRM_SUFFIX

    # Create vBRM Backup
    BRO_CTRL.create_backup(backup, V3_FRAG_CM_VBRM_AGENT,
                           NAMESPACE, V3_FRAG_CM_VBRM_SCOPE)

    # Creates the payload for export.
    sftp_payload = SFTP_SERVER.get_sftp_payload(SFTP_PATH)
    sftp_payload.update({"backupName": backup})
    action_id = BRO_CTRL.export_backup(
        backup_name=backup,
        payload=sftp_payload,
        scope=V3_FRAG_CM_VBRM_SCOPE)
    assert BRO_CTRL.get_action_result(action_id, "EXPORT",
                                      scope=V3_FRAG_CM_VBRM_SCOPE) == "SUCCESS"
    assert (backup in BRO_CTRL.get_action_additionalInfo(
        action_id, "EXPORT", scope=V3_FRAG_CM_VBRM_SCOPE))


def test_import_vbrm_backup_post_upgrade_to_dev():
    """
    This test verifies that a vBRM backup can be imported from the SFTP server
    using the vBRM scope.
    """
    log("Test Case: test_import_vbrm_backup_post_upgrade_to_dev")

    global BACKUP_TARBALL_NAME
    BACKUP_TARBALL_NAME = BRO_CTRL.generate_backup_tarball_name(
        BACKUP_LIST[0] + V3_FRAG_CM_VBRM_SUFFIX, scope=V3_FRAG_CM_VBRM_SCOPE)

    BRO_CTRL.import_backup_with_existing_backup_deletion(
        backup_name=BACKUP_LIST[0] + V3_FRAG_CM_VBRM_SUFFIX,
        remote_backup_name=BACKUP_TARBALL_NAME,
        namespace=NAMESPACE,
        sftp_path=SFTP_PATH,
        sftp_server=SFTP_SERVER,
        scope=V3_FRAG_CM_VBRM_SCOPE)


def test_restore_vbrm_backup_post_upgrade_to_dev():
    """
    This test calls restore on a vBRM and verifies the
    restore content in the agent.
    """

    log("Test Case: test_restore_vbrm_backup_post_upgrade_to_dev")

    BRO_CTRL.restore_backup(BACKUP_LIST[0] + V3_FRAG_CM_VBRM_SUFFIX,
                            V3_FRAG_CM_VBRM_AGENT, NAMESPACE,
                            V3_FRAG_CM_VBRM_SCOPE)


def test_create_backup_after_bro_upgrade_lj_to_dev_snapshot():
    """"
    Tests creating a backup after upgrading from
    BRO_LONGJUMP_VERSION to dev snapshot version
    """
    test_case = "test_create_backup_after_bro_upgrade_lj_to_dev_snapshot"
    log("Test Case: {}".format(test_case))
    BRO_CTRL.create_backup(BACKUP_LIST[0], TEST_AGENTS, NAMESPACE)


def test_import_backup_after_bro_upgrade_lj_to_dev_snapshot():
    """"
    This will test the import a legacy
    backup from SFTP server after upgrading from
    BRO_LONGJUMP_VERSION to dev snapshot
    """
    test_case = "test_import_backup_after_bro_upgrade_lj_to_dev_snapshot"
    BRO_CTRL.import_backup_to_sftp(test_case, BACKUP_LIST[0], SFTP_PATH,
                                   DEFAULT_SCOPE, SFTP_SERVER, NAMESPACE)


def test_periodic_event_auto_export_after_upgrade_lj_to_dev_snap():
    """"
    Will delete all previous periodic events create a new one.
    Checks backup auto-exports to SFTP
    """
    log("test_periodic_event_auto_export_after_upgrade_lj_to_dev_snap")
    BRO_CTRL.create_scheduled_periodic_event_autoexport(SFTP_SERVER,
                                                        SFTP_PATH,
                                                        SFTP_PASSWORD,
                                                        DEFAULT_SCOPE,
                                                        timezone="-01:00")


def test_rollback_bro_to_lj_version():
    """
    Rollback to BRO_LONGJUMP_VERSION.

    BRO is rolled back to BRO_LONGJUMP_VERSION version here.
    so it can be used as the source state for the subsequent upgrade test.
    """
    test_case = "test_rollback_bro_to_lj_version"
    log("Test Case: {}".format(test_case))

    # Collect console logs before rollback
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME,
                      "pre_{}".format(test_case))

    # List status of the helm release
    # (commented for helm3 issue, to be fixed in ADPPRG-40833)
    # bro_utils.get_helm_release_status(BRO_HELM_RELEASE_NAME)

    # Set helm release revision for rollback target
    release_revision = 1

    log("Initiate rollback")
    start_time = time.perf_counter()
    helm3procs.helm_rollback(BRO_HELM_RELEASE_NAME,
                             NAMESPACE,
                             release_revision,
                             timeout=120)
    rollback_time = bro_utils.calc_time(start_time, BRO_POD_NAME, NAMESPACE)
    log("TEST REPORT: Rollback Time: {}s".format(rollback_time))

    bro_utils.wait_for_resources_to_deploy(BRO_HELM_RELEASE_NAME, NAMESPACE)

    # Verify Orchestrator status and check registered agents
    BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())


def test_create_backup_after_bro_rollback_to_lj_version():
    """"
    Tests create backup after rollback to BRO_LONGJUMP_VERSION
    """
    test_case = "test_create_backup_after_bro_rollback_to_lj_version"
    log("Test Case: {}".format(test_case))
    BRO_CTRL.create_backup(BACKUP_LIST[1], TEST_AGENTS, NAMESPACE)


def test_import_backup_after_bro_rollback_to_lj_version():
    """"
    This will test the import a legacy
    backup from SFTP server after rollback to BRO_LONGJUMP_VERSION
    """
    test_case = "test_import_backup_after_bro_rollback_to_lj_version"
    BRO_CTRL.import_backup_to_sftp(test_case, BACKUP_LIST[0], SFTP_PATH,
                                   DEFAULT_SCOPE, SFTP_SERVER, NAMESPACE)


def test_periodic_event_auto_export_after_rollback_to_lj_version():
    """"
    Test will delete all previous periodic events create a new one.
    Checks backup auto-exports to SFTP
    """
    test_case = \
        "test_periodic_event_auto_export_after_rollback_to_lj_version"
    log("Test Case: {}".format(test_case))

    # Additional Info not included in version BRO_LONGJUMP_VERSION
    # Periodic Events cant be scheduled with nanosecs
    # Timezone block added in BRO 4.1.0-63
    BRO_CTRL.create_scheduled_periodic_event_autoexport(
        SFTP_SERVER, SFTP_PATH, SFTP_PASSWORD, DEFAULT_SCOPE,
        check_timezone_in_rest=False,
        check_additional_info=False, include_nanosec=False,
        include_sec_when_zero=False)


def test_upgrade_bro_lj_pra_to_staging_version():
    """
    Upgrades the BRO_LONGJUMP_VERSION to staging version.

    BRO is upgraded to Staging version.
    so it can be used as the source state for the subsequent upgrade test.
    """
    test_case = "test_upgrade_bro_lj_pra_to_staging_version"
    log("Test Case: {}".format(test_case))

    # Collect console logs before upgrade
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME,
                      "pre_{}".format(test_case))

    log("Add the BRO Release Repo")
    helm3procs.add_helm_repo(BRO_REL_REPO, BRO_REL_REPO_NAME)

    log("Get last released chart version")
    bro_rel_version = \
        helm3procs.get_latest_chart_version(BRO_NAME,
                                            helm_repo_name=BRO_REL_REPO_NAME,
                                            development_version=True)

    helm3procs.helm_upgrade_with_chart_repo_with_dict(
        BRO_HELM_RELEASE_NAME,
        BRO_NAME,
        NAMESPACE,
        helm_repo_name=BRO_REL_REPO_NAME,
        chart_version=bro_rel_version,
        settings_dict={
            "global.security.tls.enabled": "false",
            "global.pullSecret": "armdocker",
            "bro.enableAgentDiscovery": "true",
            "bro.vBRMAutoCreate": "DEFAULT"})

    bro_utils.wait_for_resources_to_deploy(BRO_HELM_RELEASE_NAME, NAMESPACE)

    # Verify Orchestrator status and check registered agents
    BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())


def test_create_backup_after_bro_lj_pra_to_staging_version():
    """"
    Tests create backup after upgrade from BRO_LONGJUMP_VERSION
    to staging
    """
    test_case = "test_create_backup_after_bro_lj_pra_to_staging_version"
    log("Test Case: {}".format(test_case))
    BRO_CTRL.create_backup(BACKUP_LIST[1], TEST_AGENTS, NAMESPACE)


def test_upgrade_bro_staging_version_to_dev_snapshot():
    """
    Upgrades the Orchestrator Service from the
    staging version to snapshot version.
    """
    test_case = "test_upgrade_bro_staging_version_to_dev_snapshot"
    log("Test Case: {}".format(test_case))

    log("Fetch BRO snapshot chart for upgrade")
    helm3procs.helm_get_chart_from_repo(
        BRO_NAME, BRO_SNAP_VERSION, helm_repo_name=BRO_SNAP_REPO_NAME)

    log("Initiate upgrade")
    helm3procs.helm_upgrade_with_chart_archive_with_dict(
        BRO_HELM_RELEASE_NAME,
        BRO_SNAP_ARCHIVE,
        NAMESPACE,
        settings_dict={"bro.enableAgentDiscovery": "true",
                       "global.pullSecret": "armdocker",
                       "global.security.tls.enabled": "false",
                       "bro.vBRMAutoCreate": "DEFAULT",
                       "sftp.archive.compressionLevel": "NO_COMPRESSION"},
        timeout=120)

    bro_utils.wait_for_resources_to_deploy(BRO_HELM_RELEASE_NAME, NAMESPACE)

    # Verify Orchestrator status and check registered agents
    BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids())

    # Collect logs after upgrade
    KUBE.get_pod_logs(NAMESPACE, BRO_POD_NAME, "post_{}".format(test_case))
    get_agent_pod_logs("post_{}".format(test_case))


def test_tls_bro_validate_two_ports():
    """
    Active the TLS port and validates if http / TLS are acive.
    """
    test_case = "test_tls_bro_validate_two_ports"
    log("Test Case: {}".format(test_case))
    validator = DualPortValidator(hostport=7001, tlsport=7002,
                                  ca_cert_path="cacert.pem")
    validator.validate_http_tls_ports()


def test_import_backup_from_sftp_legacy():
    """"
    This will test the import a legacy
    backup from SFTP server
    """
    log("Test Case: test_import_backup_from_sftp_legacy")

    BRO_CTRL.import_backup_with_existing_backup_deletion(
        backup_name=BACKUP_LIST[0],
        remote_backup_name=BACKUP_LIST[0],
        namespace=NAMESPACE,
        sftp_path=SFTP_PATH,
        sftp_server=SFTP_SERVER)


def test_create_backups_post_upgrade():
    """
    This test creates multiple backups and verifies the
    backups content in the Orchestrator.
    This test does this for the "DEFAULT" scope
    """

    log("Test Case: test_create_backups_post_upgrade")

    # Create backup and check against all agents
    BRO_CTRL.create_backup(BACKUP_LIST[0], TEST_AGENTS, NAMESPACE)


def test_restore_backups_post_upgrade():
    """
    This test calls restore multiple times and verifies the
    restore content in the agent.
    """

    log("Test Case: test_restore_backups_post_upgrade")

    BRO_CTRL.restore_backup(BACKUP_LIST[0], TEST_AGENTS, NAMESPACE)


def test_housekeeping_v1():
    """
    This test verifies that BRO returns 501 for the v1 housekeeping endpoints.
    """

    log("Test Case: test_housekeeping_v1")

    # GET v1
    out = BRO_CTRL.get_housekeeping_config(rest_version="v1")

    assert {out["statusCode"], out["message"]} == \
           {501, "Not implemented"}, "Incorrect Response from BRO"

    # POST v1
    out = BRO_CTRL.update_housekeeping_config("enabled", MAX_STORED_BACKUPS,
                                              rest_version="v1")

    assert {out["statusCode"], out["message"]} == \
           {501, "Not implemented"}, "Incorrect Response from BRO"


def test_periodic_event_n_auto_export_upgrade_bro_staging_to_dev():
    """"
    This will create a periodic event and auto export
    backup to SFTP server after upgrading staging to dev snapshot.
    Previous periodic events will be deleted beforehand.
    """

    log("test_periodic_event_n_auto_export_upgrade_bro_staging_to_dev")
    BRO_CTRL.create_scheduled_periodic_event_autoexport(SFTP_SERVER,
                                                        SFTP_PATH,
                                                        SFTP_PASSWORD,
                                                        DEFAULT_SCOPE,
                                                        timezone="+02:00")
    SFTP_SERVER.stop_sftp_server()


# def test_drain_node():
#     """
#     This test will verify that BRO is running after
#     the node on which the BRO pod is running is drained
#     """
#     log("test_drain_node")

#     list_of_pods = KUBE.list_pods_from_namespace(NAMESPACE).items

#     node_name = ""
#     pod_status = ""

#     log("Getting the name of the node the BRO pod is running on"
#         " and the status of the BRO pod")
#     for item in list_of_pods:
#         if BRO_POD_NAME in item.metadata.name:
#             node_name = item.spec.node_name
#             pod_status = item.status.phase

#     log("Node: {} | BRO Pod status: {}"
#         .format(node_name, pod_status))

#     if pod_status == "Running":
#         log("Drain starting")

#         KUBE.drain_node(node_name)

#         log("Node drained, waiting for BRO pod to come back up")

#         KUBE.wait_for_pod_status(BRO_POD_NAME, NAMESPACE, counter=60,
#                                  ready=True, interval=10)

#         log("BRO pod is back up - Running uncordon node")

#         KUBE.cordon_node(node_name, cordon_node=False)
#     else:
#         raise Exception("BRO pod not running - drain not executed")


def test_remove_k8s_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    log("Test Case: test_remove_k8s_resources")
    bro_utils.remove_namespace_resources(NAMESPACE)
    log("Finished removing Kubernetes resources")
    log("Closing kubernetes client")


# Helper function: compare 2 calendars
def compare_calendars(initial_calendar, updated_calendar):
    """
    Compare 2 calendars and return an array with fields changed
    """
    changes = []
    for key in initial_calendar:
        value = initial_calendar[key]
        update_value = updated_calendar[key]
        if value != update_value:
            changes.append(key)
    return changes


# Helper function: get all test agent ids
def get_agent_ids():
    """
    Fetches the Agent Id of all test agents and
    returns this as a list.

    :return: a list of the agent Ids.
    """
    return [agent["AgentId"] for agent in TEST_AGENTS]


def get_agent_pod_logs(name):
    """
    Gets the pod logs for all running agents.
    :param name: name to append  to log
    """
    for agent in get_agent_ids():
        pod = "{}-agent-0".format(agent)
        KUBE.get_pod_logs(NAMESPACE, pod, name)
