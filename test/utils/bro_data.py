#!/usr/bin/env python3
"""
This module provides methods to verify the integrity of the files
transferred between the Orchestrator and the Agents during a backup
or restore operation.
"""

import json
import utilprocs
import k8sclient
from bro_s3 import BroS3


# Instantiate kube client
KUBE = k8sclient.KubernetesClient()

# container and pod data
BRO_CONTAINER = "eric-ctrl-bro"
BRO_POD_NAME = "eric-ctrl-bro-0"
DEFAULT_SCOPE = "DEFAULT"

# backup data path on ORCH pod
BACKUP_LOCATION = "bro/backups"

# data location on agent pods
AGENT_BACKUP_PATH = "/backupdata"
AGENT_RESTORE_PATH = "/restoredata"


def verify_file_transfer(backup_name, agent_list, action,
                         namespace, scope=DEFAULT_SCOPE,
                         validate_checksum=True):
    """
    Verifies that the expected files have been transferred from
    the TestAgent to the Orchestrator or viceversa.
    Also verifies the file content.

    :param backup_name: Name of the backup
    :param agent_list: List of agents in the backup
    :param action: string indicating if action is backup or restore
    :param namespace: namespace where orchestrator pod is deployed
    :param scope: scope of backup
    :param validate_checksum: Validates checksum files
    """

    # Initialize agent path
    if action == "restore":
        agent_path = AGENT_RESTORE_PATH
    else:
        agent_path = AGENT_BACKUP_PATH

    # Check the file transfer for each test agent
    for agent in agent_list:
        agent_id = agent["AgentId"]
        if agent_id == "v4-metadata-only":
            utilprocs.log("Skip Check for Agent {}".format(agent_id))
            continue

        utilprocs.log("Checking Agent {}".format(agent_id))
        agent_pod = "{}-agent-0".format(agent_id)

        if agent_path == AGENT_RESTORE_PATH:
            # Check that the expected files exist on the agent
            expected_files = get_container_files(AGENT_BACKUP_PATH,
                                                 agent_pod, namespace)
            utilprocs.log("Expected files: {}".format(expected_files))

            actual_files = get_container_files(AGENT_RESTORE_PATH,
                                               agent_pod, namespace)
            # lost+found directory created after storage provider change
            actual_files.append('lost+found')
            utilprocs.log("Actual files: {}".format(actual_files))

            files_exist = all(item in actual_files for item in expected_files)
            assert files_exist, "Not all expected files present"

        if "fragments" in agent:
            for fragment in agent["fragments"]:
                fragment_id = fragment["fragmentId"]
                utilprocs.log("Checking fragment {}".format(fragment_id))

                # Derive the backup path on orchestrator
                orch_path = "{}/{}/{}/{}/{}".format(BACKUP_LOCATION, scope,
                                                    backup_name, agent_id,
                                                    fragment_id)

                # Check files on data path
                verify_data_file("{}/data".format(orch_path),
                                 '{}_data.txt'.format(fragment_id),
                                 agent_pod, agent_path, namespace,
                                 validate_checksum)
                # Check files on data path

                if fragment["customData"]:
                    utilprocs.log("Checking Custom Metadata for fragment {}"
                                  .format(fragment_id))
                    # Check files on CustomMetadata path
                    cm_id = "{}_CustomMetadata.txt".format(fragment_id)
                    verify_data_file("{}/customMetadata".format(orch_path),
                                     cm_id,
                                     agent_pod, agent_path, namespace,
                                     validate_checksum)
                if agent_path == AGENT_BACKUP_PATH:
                    # Verify contents of Fragment.json
                    verify_fragment_file(orch_path, fragment_id, namespace)


def verify_data_file(orch_path, file_name, agent_pod, agent_path,
                     namespace, validate_checksum):
    """
    Verifies that the expected file has been transferred.
    Calculates checksum and compares with the file on the agent pod.

    :param orch_path: path to the backup directory on the orchestrator
    :param file_name: expected backup file name
    :param agent_pod: pod name of the agent
    :param agent_path: path to backup or restore directory on agent
    :param namespace: namespace where orchestrator pod is deployed
    :param validate_checksum: Validates checksum files

    """

    if validate_checksum:
        # Check that the expected file exists on the Orchestrator
        file = get_container_files(orch_path, BRO_POD_NAME, namespace,
                                   container=BRO_CONTAINER)
    else:
        file = get_osmn_files(orch_path)

    # Remove the stored checksum from the list if present
    if len(file) > 1:
        file.remove("{}.md5".format(file_name))

    assert file == [file_name], "unexpected files"

    if validate_checksum:
        # Verify the checksum of the file
        # Check file on Orchestrator
        md5_orch = get_fragment_checksum(orch_path, BRO_POD_NAME,
                                         file, namespace,
                                         container=BRO_CONTAINER)
        # Check file on agent
        md5_agent = get_fragment_checksum(
            agent_path, agent_pod, file, namespace)
        assert md5_orch == md5_agent, "checksum mismatch"


def verify_fragment_file(orch_path, fragment_id, namespace):
    """
    Verifies that the Fragment.json file has
    the expected fragment id.

    :param orch_path: path to the backup fragment on Orchestrator
    :param fragment_id: id of the fragment
    :param namespace: namespace where orchestrator pod is deployed
    """

    # Verify contents of Fragment.json
    cmd = ['cat', '{}/Fragment.json'.format(orch_path)]
    out = KUBE.exec_cmd_on_pod(BRO_POD_NAME, namespace,
                               cmd, container=BRO_CONTAINER)
    # Adjust string to expected json format
    out = out.replace("\'", "\"")
    assert json.loads(out)['fragmentId'] == fragment_id, \
        "incorrect fragment id"


def get_fragment_checksum(path, pod, files, namespace, container=""):
    """
    Fetches the checksum for each file from the container
    and returns this as a list.

    :param path: path on pod
    :param pod: pod holding the files
    :param files: list of files on pod to be checked
    :param namespace: namespace where pods deployed
    :param container: container on the pod

    :return: dict of file : checksums for all files
    """

    checksum = []
    for backupfile in files:
        cmd = ['md5sum', '{}/{}'.format(path, backupfile)]
        out = KUBE.exec_cmd_on_pod(pod, namespace, cmd, container=container)
        value = list(out.split(" "))[0]
        checksum.append(value)
    return dict(zip(files, checksum))


def get_container_files(path, pod, namespace, container=""):
    """
    Fetches file names in a pod container at the specified path

    :param path: path in container
    :param pod: pod holding the files
    :param namespace: namespace where pods deployed
    :param container: container on the pod

    :return: list of file names at the specified path
    """

    cmd = ['ls', "{}".format(path)]
    out = KUBE.exec_cmd_on_pod(pod, namespace,
                               cmd, container=container)
    files = list(out.split("\n"))
    files.remove('')
    return files


def get_osmn_files(path, bucket_name="bro"):
    """
    Fetches file names in a pod container at the specified path

    :param path: path in container
    :param namespace: namespace where pods deployed
    :param container: container on the pod
    :param osmn_svc: pod holding the files

    :return: list of file names at the specified path
    """
    osmn_svc = BroS3()
    return osmn_svc.get_bucket_file_list(path=path, bucket_name=bucket_name)


def read_container_content(file_path, pod, namespace, container=""):
    """
    Retrieves the content of a file in a pod container.

    :param file_path: path to the file in the container
    :param pod: pod holding the file
    :param namespace: namespace where the pod is deployed
    :param container: container on the pod

    :return: content of the file
    """
    cmd = ['cat', file_path]
    return KUBE.exec_cmd_on_pod(pod, namespace, cmd, container=container)
