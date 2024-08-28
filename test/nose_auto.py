#!/usr/bin/env python3
"""
This module provides set of basic test cases to verify the
Backup and Restore Orchestrator functionality.
The full set of test cases are available in the module nose_auto_nightly.
In this test phase global security is set to false.
See confluence page for architecture and use cases:
 https://confluence.lmera.ericsson.se/display/AA/BR+Orchestrator+Component
 +Description
"""
import os
import time

import bro_sftp
import bro_ctrl
import bro_data
import bro_utils
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

# Base REST URL and Scopes
BASE_URL = "http://eric-ctrl-bro:7001/v1/backup-manager"
DEFAULT_SCOPE = "DEFAULT"
SUBSCRIBER_SCOPE = "subscriber"
CONFIGURATION_SCOPE = "configuration-data"

# SFTP Server USER and password
SFTP_USER = "brsftp"
SFTP_PASSWORD = "planbsftp"
SFTP_PATH = "bro_test/1/3/"
SFTP_SERVER = bro_sftp.SFTPServer(SFTP_USER, SFTP_PASSWORD)
HTTP_PATH = ""

# SFTP Server
SFTP_POD_NAME = "bro-test-sftp-0"
SFTP_CONTAINER = "bro-test-sftp"

# Define Backup Names
BACKUP_LIST = ["bu1", "bu2"]
SUBSCRIBER_BACKUP_LIST = ["sub_bu1", "sub_bu2"]
CONFIGURATION_BACKUP_LIST = ["con_bu1", "con_bu2"]
MAX_STORED_BACKUPS = 5

BACKUP_TARBALL_NAME = ""

# Periodic event id. This value is set in
# test_schedule_backup_and_auto_export
EVENT_ID = ""

V2_AGENTS = [V2_FRAG_ONLY, V2_FRAG_CM]
V3_AGENTS = [V3_FRAG_CM, V3_FRAG_ONLY]
V4_AGENTS = [V4_FRAG_CM,
             V4TLS_FRAG_ONLY,
             V4_MULTI_FRAG_CM,
             V4_NO_FRAG_OR_META,
             V4_NO_FRAG_CM_ONLY]
TEST_AGENTS = V2_AGENTS + V3_AGENTS + V4_AGENTS
CONFIGURATION_AGENTS = [V4_FRAG_CM, V4_MULTI_FRAG_CM_CONFIGURATION,
                        V3_FRAG_ONLY, V3_FRAG_CM]
SUBSCRIBER_AGENTS = [V4TLS_FRAG_ONLY,
                     V2_FRAG_ONLY,
                     V4_MULTI_FRAG_CM_SUBSCRIBER,
                     V4_NO_FRAG_OR_META,
                     V4_NO_FRAG_CM_ONLY]
BACKUP_TYPE = [
    V4_MULTI_FRAG_CM_SUBSCRIBER,
    V4_MULTI_FRAG_CM_CONFIGURATION,
]


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
    settings = {"bro.enableLimitedParallelActions": "true",
                "global.pullSecret": "armdocker",
                "global.security.tls.enabled": "false",
                "bro.enableAgentDiscovery": "true",
                "service.endpoints.restActions.tls.enforced": "optional",
                "bro.vBRMAutoCreate": "NONE",
                "eric-ctrl-bro.bro.enableNotifications": "false",
                }
    helm_release_name = \
        bro_utils.get_service_helm_release_name(BRO_NAME, NAMESPACE)
    helm3procs.helm_install_chart_from_repo_with_dict(
        BRO_NAME,
        helm_release_name,
        NAMESPACE,
        helm_repo_name=BRO_SNAP_REPO_NAME,
        chart_version=BRO_SNAP_VERSION,
        settings_dict=settings,
        timeout=120)

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
                                 get_agent_ids(V2_AGENTS),
                                 NAMESPACE, enable_global_tls=False)

    # Deploy the v3 test agents
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION,
                                 get_agent_ids(V3_AGENTS),
                                 NAMESPACE, enable_global_tls=False)

    # Deploy the v4 test agents
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION,
                                 get_agent_ids(V4_AGENTS),
                                 NAMESPACE, enable_global_tls=False)

    # Verify that Orchestrator has all expected agents registered
    BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids(TEST_AGENTS))

    # DEFAULT, SUBSCRIBER and CONFIGURATION reset vBRMs
    brm_count = 3 * 2

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
    bro_utils.assert_equal(len(config_agents), len(CONFIGURATION_AGENTS))


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
    date_format = "%Y-%m-%dT%H:%M:%S"
    event_config = {
        "hours": 1,
        "startTime": time.strftime(date_format, start_time)
    }
    event_id = BRO_CTRL.create_scheduled_periodic_event(event_config)
    BRO_CTRL.wait_for_brm_to_be_available("CREATE_BACKUP", "DEFAULT")
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


def test_delete_backup():
    """
    This test verifies that a backup can be deleted.
    Uses v4 delete backup endpoint.
    """
    log("Test Case: test_delete_backup")
    BRO_CTRL.wait_for_brm_to_be_available("DELETE_BACKUP",
                                          DEFAULT_SCOPE, "for-reset")
    BRO_CTRL.v4_delete_backup("for-reset", NAMESPACE)


def test_create_and_export_parallel():
    """
    This test creates multiple backups and verifies the
    backups content in the Orchestrator.
    This test does this for the scopes "DEFAULT",
    "configuration-data" and "subscriber"
    """

    log("Test Case: test_create_backups")

    auto_delete = "enabled"
    BRO_CTRL.update_housekeeping_config(max_stored_backups=MAX_STORED_BACKUPS,
                                        rest_version="v4",
                                        rest_method=rest_utils.REQUESTS.patch)
    BRO_CTRL.update_housekeeping_config(auto_delete, rest_version="v4",
                                        rest_method=rest_utils.REQUESTS.patch)

    # Create backup and check against all agents
    BRO_CTRL.v4_create_backup(BACKUP_LIST[0], TEST_AGENTS, NAMESPACE)

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
        task='export', scope=DEFAULT_SCOPE, backup_name=BACKUP_LIST[0],
        method='POST', task_state="Available")
    restore_task = bro_utils.generate_backup_manager_task(
        task='restore', scope=DEFAULT_SCOPE, backup_name=BACKUP_LIST[0],
        method='POST', task_state="Available")
    delete_task = bro_utils.generate_backup_manager_task(
        task='delete', scope=DEFAULT_SCOPE, backup_name=BACKUP_LIST[0],
        method='DELETE', task_state="Available")

    assert create_task and import_task and export_task and restore_task \
        and delete_task in backup_manager['availableTasks'], \
        "backup-manager does not have the expected availableTasks"

    assert [] == backup_manager['ongoingTasks'], \
        "backup-manager does not have the expected ongoingTasks"

    assert DEFAULT_SCOPE in list(backup_manager.values()), \
        "backup-manager does not have the expected scope"

    # Creates the payload for export.
    sftp_payload = SFTP_SERVER.get_sftp_payload(SFTP_PATH)
    log("Exporting {}".format(BACKUP_LIST[0]))
    action_id = BRO_CTRL.v4_export_backup(
        backup_name=BACKUP_LIST[0],
        payload=sftp_payload, wait_for_action_complete=False)

    # Verify Backup Manager endpoint
    # Export should be ongoing and create backup should be available
    BRO_CTRL.wait_for_action_start(action_id, "EXPORT", rest_version="v3")
    backup_manager = BRO_CTRL.get_backup_manager(rest_version="v4")

    export_task = bro_utils.generate_backup_manager_task(
        task='export', scope=DEFAULT_SCOPE, backup_name=BACKUP_LIST[0],
        action_id=action_id, method='GET', task_state="Ongoing")

    expected_output = [[export_task], [create_task], "DEFAULT"]

    assert list(backup_manager.values()) == expected_output, \
        "backup-manager not updated as expected"

    # Create backup and check against all agents
    # Whilst exporting backup and create backup. This method will wait
    # till create backup is finished. Export should be finished at this
    # point and we can check that both is successful
    create_id = BRO_CTRL.v4_create_backup(BACKUP_LIST[1],
                                          TEST_AGENTS,
                                          NAMESPACE,
                                          wait_for_action_complete=False)

    # Verify Backup Manager endpoint
    # Export and create backup should be ongoing
    BRO_CTRL.wait_for_action_start(create_id, "CREATE_BACKUP",
                                   rest_version="v3")
    backup_manager = BRO_CTRL.get_backup_manager(rest_version="v4")

    create_task = bro_utils.generate_backup_manager_task(
        task='create', scope=DEFAULT_SCOPE,
        backup_name=BACKUP_LIST[1], method='GET', task_state="Ongoing")

    assert [] == backup_manager['availableTasks'], \
        "backup-manager does not have the expected availableTasks"

    assert create_task and export_task in backup_manager['ongoingTasks'], \
        "backup-manager does not have the expected ongoingTasks"

    assert DEFAULT_SCOPE in list(backup_manager.values()), \
        "backup-manager does not have the expected scope"

    # Added wait for action to complete and verify as checks were skipped
    # to check that both actions were ongoing
    BRO_CTRL.wait_for_action_complete(create_id, "CREATE_BACKUP",
                                      BACKUP_LIST[1],
                                      DEFAULT_SCOPE,
                                      'SUCCESS',
                                      rest_version="v3")

    BRO_CTRL.verify_backup_is_taken(BACKUP_LIST[1],
                                    TEST_AGENTS,
                                    NAMESPACE,
                                    DEFAULT_SCOPE,
                                    test_agent=True,
                                    rest_version="v4")

    assert BRO_CTRL.get_action_result(action_id, "EXPORT") == "SUCCESS"
    assert (BACKUP_LIST[0] in BRO_CTRL.get_action_additionalInfo(
        action_id, "EXPORT"))

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

    # Get backup tarball name and get the exported backup path
    backup_tarball_name = BRO_CTRL.get_backup_tarball_name(BACKUP_LIST[0])
    exported_backup_path = "bro_test/1/3/DEFAULT/{}.xxh64".format(
        backup_tarball_name)

    # Verify Checksum is 16 bits in length
    bro_utils.validate_checksum_size(path=exported_backup_path,
                                     pod_name=SFTP_POD_NAME,
                                     namespace=NAMESPACE,
                                     container=SFTP_CONTAINER)

    # run backups for scope and check against the
    # filtered lists of agents
    BRO_CTRL.v4_create_backup(SUBSCRIBER_BACKUP_LIST[1],
                              SUBSCRIBER_AGENTS,
                              NAMESPACE,
                              SUBSCRIBER_SCOPE)

    BRO_CTRL.v4_create_backup(CONFIGURATION_BACKUP_LIST[1],
                              CONFIGURATION_AGENTS,
                              NAMESPACE,
                              CONFIGURATION_SCOPE)

    # Enable auto deletion again in housekeeping using REST v4
    single_backup = 1
    BRO_CTRL.update_housekeeping_config(auto_delete, single_backup,
                                        SUBSCRIBER_SCOPE, rest_version="v4",
                                        rest_method=rest_utils.REQUESTS.patch)
    BRO_CTRL.update_housekeeping_config(auto_delete, single_backup,
                                        CONFIGURATION_SCOPE, rest_version="v4",
                                        rest_method=rest_utils.REQUESTS.put)


def test_import_and_restore_parallel():
    """
     Imports a backup from a sftp server and
     calls a restore to run in parallel to import.
     Calls a second and third restore from a different scope
     and verifies the restore content in the agent.
    """
    log("Test Case: test_import_and_restore_parallel")
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

    import_id = BRO_CTRL.v4_import_backup(
        backup_name=BACKUP_LIST[0],
        payload=sftp_payload, wait_for_action_complete=False)

    # Restore backup in parallel to import
    first_restore_id = BRO_CTRL.v4_restore_backup(BACKUP_LIST[1],
                                                  TEST_AGENTS,
                                                  NAMESPACE)

    # Second and Third restore runs after import and the first restore
    second_restore_id = BRO_CTRL.v4_restore_backup(SUBSCRIBER_BACKUP_LIST[1],
                                                   SUBSCRIBER_AGENTS,
                                                   NAMESPACE,
                                                   SUBSCRIBER_SCOPE)

    third_restore_id = BRO_CTRL.v4_restore_backup(CONFIGURATION_BACKUP_LIST[1],
                                                  CONFIGURATION_AGENTS,
                                                  NAMESPACE,
                                                  CONFIGURATION_SCOPE)

    # GET all imports
    all_imports = BRO_CTRL.v4_get_imports()
    values_in_all_imports = [elem.get("id") for elem in all_imports]
    assert import_id in values_in_all_imports, \
        "Import ID {} is not in the import list" \
        .format(import_id)

    # Get Import by ID
    import_by_id = BRO_CTRL.v4_get_imports_by_id(import_id=import_id)
    assert import_by_id["task"]["result"] == "SUCCESS", \
        "Import found does not have the expected result"

    # GET all restores
    all_restores = BRO_CTRL.v4_get_restores(backup_name=BACKUP_LIST[1])
    values_in_all_restores = [elem.get("id") for elem in all_restores]
    assert first_restore_id in values_in_all_restores, \
        "Restore ID {} is not in the restore list" \
        .format(first_restore_id)

    # Get restore by ID
    restore_by_id = BRO_CTRL.v4_get_restores_by_id(
        backup_name=BACKUP_LIST[1], restore_id=first_restore_id)
    assert restore_by_id["task"]["result"] == "SUCCESS", \
        "Restore found does not have the expected result"

    # Get restore by ID
    restore_by_id = BRO_CTRL.v4_get_restores_by_id(
        backup_name=SUBSCRIBER_BACKUP_LIST[1],
        restore_id=second_restore_id,
        scope=SUBSCRIBER_SCOPE)
    assert restore_by_id["task"]["result"] == "SUCCESS", \
        "Restore found does not have the expected result"

    # Get restore by ID
    restore_by_id = BRO_CTRL.v4_get_restores_by_id(
        backup_name=CONFIGURATION_BACKUP_LIST[1],
        restore_id=third_restore_id,
        scope=CONFIGURATION_SCOPE)
    assert restore_by_id["task"]["result"] == "SUCCESS", \
        "Restore found does not have the expected result"

    # Get all tasks
    all_tasks = BRO_CTRL.v4_get_tasks()
    values_in_all_tasks = [elem.get("id") for elem in all_tasks]
    assert import_id in values_in_all_tasks, \
        "Import ID {} is not in the tasks list" \
        .format(import_id)

    # Get task by ID
    task_by_id = BRO_CTRL.v4_get_tasks_by_id(task_id=first_restore_id)
    assert task_by_id["resource"] == \
        "/backup-restore/v4/backup-managers/{}/backups/{}/restores" \
        .format(DEFAULT_SCOPE, BACKUP_LIST[1]), \
        "Restore task is not the expected task"

    assert BRO_CTRL.backup_exists(BACKUP_LIST[0]), \
        "Backup {} has not been imported ".format(BACKUP_LIST[0])


def test_import_and_restore_different_brm_parallel():
    """
     Imports a backup from a sftp server and
     calls a restore on a different BRM to run
     in parallel to import.
    """
    log("Test Case: test_import_and_restore_different_brm_parallel")
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

    import_id = BRO_CTRL.v4_import_backup(
        backup_name=BACKUP_LIST[0],
        payload=sftp_payload, wait_for_action_complete=False)

    log("Import task ID: {}".format(import_id))

    # Verify Backup Manager endpoint
    # Import should be ongoing and restore should be available
    BRO_CTRL.wait_for_action_start(import_id, "IMPORT", rest_version="v3")
    backup_manager = BRO_CTRL.get_backup_manager(rest_version="v4")

    import_task = bro_utils.generate_backup_manager_task(
        task='import', scope=DEFAULT_SCOPE, method='GET', task_state="Ongoing",
        backup_name=BACKUP_LIST[0], action_id=import_id)
    restore_task = bro_utils.generate_backup_manager_task(
        task='restore', scope=DEFAULT_SCOPE, backup_name=BACKUP_LIST[1],
        method='POST', task_state="Available")

    expected_output = [[import_task], [restore_task], "DEFAULT"]

    assert list(backup_manager.values()) == expected_output, \
        "backup-manager not updated as expected"

    # Restore backup while import is running
    restore_id = BRO_CTRL.v4_restore_backup(CONFIGURATION_BACKUP_LIST[1],
                                            CONFIGURATION_AGENTS,
                                            NAMESPACE,
                                            CONFIGURATION_SCOPE,
                                            wait_for_action_complete=False)

    # Verify Backup Manager endpoint
    # Import and Restore should be ongoing
    BRO_CTRL.wait_for_action_start(restore_id, "RESTORE",
                                   scope=CONFIGURATION_SCOPE,
                                   rest_version="v3")
    backup_manager = BRO_CTRL.get_backup_manager(rest_version="v4")
    backup_manager_config = BRO_CTRL.get_backup_manager(
        backup_manager_name=CONFIGURATION_SCOPE, rest_version="v4")

    restore_task = bro_utils.generate_backup_manager_task(
        task='restore', scope=CONFIGURATION_SCOPE, method='GET',
        task_state="Ongoing", backup_name=CONFIGURATION_BACKUP_LIST[1],
        action_id=restore_id)

    assert import_task in backup_manager['ongoingTasks'], \
        "backup-manager does not have the expected ongoingTasks"

    assert restore_task in backup_manager_config['ongoingTasks'], \
        "Config backup-manager does not have the expected ongoingTasks"

    assert [] == backup_manager['availableTasks'], \
        "backup-manager does not have the expected availableTasks"

    assert [] == backup_manager_config['availableTasks'], \
        "Config backup-manager does not have the expected availableTasks"

    assert DEFAULT_SCOPE in list(backup_manager.values()), \
        "backup-manager does not have the expected scope"

    assert CONFIGURATION_SCOPE in list(backup_manager_config.values()), \
        "Config backup-manager does not have the expected scope"

    # Added wait for action to complete and verify as checks were skipped
    # to check that both actions were ongoing
    BRO_CTRL.wait_for_action_complete(restore_id, "RESTORE",
                                      CONFIGURATION_BACKUP_LIST[1],
                                      CONFIGURATION_SCOPE, "SUCCESS",
                                      rest_version="v3")

    # Verify restore files
    bro_data.verify_file_transfer(CONFIGURATION_BACKUP_LIST[1],
                                  CONFIGURATION_AGENTS,
                                  "restore", NAMESPACE,
                                  CONFIGURATION_SCOPE,
                                  validate_checksum=True)

    # Get restore by ID
    restore_by_id = BRO_CTRL.v4_get_restores_by_id(
        backup_name=CONFIGURATION_BACKUP_LIST[1],
        restore_id=restore_id,
        scope=CONFIGURATION_SCOPE)
    assert restore_by_id["task"]["result"] == "SUCCESS", \
        "Restore found does not have the expected result"

    assert BRO_CTRL.backup_exists(BACKUP_LIST[0]), \
        "Backup {} has not been imported ".format(BACKUP_LIST[0])


def test_housekeeping_config_update():
    """
    Updates housekeeping configuration and ensures that backups are removed
    in the correct order of failed first and then oldest.
    """
    log("Test Case: test_housekeeping_config_update")

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
    BRO_CTRL.wait_for_bro_agents_to_reconnect(get_agent_ids(TEST_AGENTS))

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


def test_remove_k8s_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    log("Test Case: test_remove_k8s_resources")

    bro_utils.remove_namespace_resources(NAMESPACE)

    log("Finished removing Kubernetes resources")

    log("Closing kubernetes client")


# Helper function: get all test agent ids
def get_agent_ids(test_agents):
    """
    Fetches the Agent Id of all test agents and
    returns this as a list.

    :return: a list of the agent Ids.
    """
    return [agent["AgentId"] for agent in test_agents]


def get_agent_pod_logs(name):
    """
    Gets the pod logs for all running agents.
    :param name: name to append  to log
    """
    for agent in TEST_AGENTS:
        pod = "{}-agent-0".format(agent)
        KUBE.get_pod_logs(NAMESPACE, pod, name)
