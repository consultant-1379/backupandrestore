#!/usr/bin/env python3

"""
Module for deleting agents which were installed using install_agents.py
"""

import argparse
import time

import config
from utils import cmd_util, k8s_util
from bro_logging import logger


def delete(number_of_agents):
    """
    Deletes the agents

    :param number_of_agents: The number of agents to delete
    """
    delete_cmd = 'helm delete --purge ' + config.AGENT_RELEASE_NAME
    for agent_number in range(number_of_agents):
        cmd_util.execute_command(delete_cmd.format(agent_number))
    for agent_number in range(number_of_agents):
        _wait_for_pod_to_delete(config.AGENT_RELEASE_NAME.format(
            agent_number))


def _wait_for_pod_to_delete(pod_name_prefix):
    """
    Waits until a pod has been deleted

    :param pod_name_prefix: A string which is the start of the name of the pod
    to be deleted
    """
    while True:
        pods = k8s_util.list_pods_from_namespace(config.NAMESPACE)
        pod_found = False
        logger.log('Waiting for agents to terminate')
        for pod in pods:
            if pod_name_prefix in pod:
                pod_found = True
                time.sleep(5)
                break
        if not pod_found:
            break


def _main():
    """
    Main method
    """
    args = _get_cli_args()
    delete(int(args.num_agents))


def _get_cli_args():
    """
    Retrieves the command line arguments. Only used if this file is the
    __main__ file.
    """
    parser = argparse.ArgumentParser(
        description='Deletes agents.')
    parser.add_argument(
        'num_agents', type=str, help='The number of agents you want to delete')
    return parser.parse_args()


if __name__ == '__main__':
    _main()
