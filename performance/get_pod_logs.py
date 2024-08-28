#!/usr/bin/env python3

"""
Module for getting pod logs
"""

import argparse

from bro_logging import logger
import config
from utils import cmd_util, file_utils


def get_logs(number_of_agents, sub_folder=None):
    """
    Retrieves the logs of the orchestrator and the agents

    :param number_of_agents: The number of agents installed
    :param sub_folder: The name of a sub-folder to put the logs into. This
    sub-folder will be created under the default log_location for the
    performance tests
    """
    logger.log('Getting logs from agents and Orchestrator')
    pod_log_location = config.log_location()
    if sub_folder:
        pod_log_location = '{}/{}'.format(pod_log_location, sub_folder)
    file_utils.create_directories(pod_log_location)
    agent_log_cmd_template = 'kubectl logs {}'
    orch_log_cmd = 'kubectl logs eric-ctrl-bro-0 -c eric-ctrl-bro'
    cmd_util.execute_command_send_to_file(
        orch_log_cmd, '{}/orch_logs.txt'.format(pod_log_location))
    for agent_number in range(number_of_agents):
        agent_name = config.AGENT_RELEASE_NAME.format(agent_number)
        pod_name = '{}-agent-0'.format(agent_name)
        agent_log_cmd = agent_log_cmd_template.format(pod_name)
        cmd_util.execute_command_send_to_file(
            agent_log_cmd, '{}/agent_{}_logs.txt'.format(
                pod_log_location, agent_number))


def _main():
    """
    Main method
    """
    args = _get_cli_args()
    sub_folder = None
    if hasattr(args, 'sub_folder'):
        sub_folder = args.sub_folder
    get_logs(int(args.num_agents), sub_folder)


def _get_cli_args():
    """
    Retrieves the command line arguments. Only used if this file is the
    __main__ file.
    """
    parser = argparse.ArgumentParser(
        description='Retrieves the logs of all the agents and the Orchestrator'
                    '. The logs will be stored in the _LOG_LOCATION as defined'
                    ' in config.py.')
    parser.add_argument(
        'num_agents', type=str, help='The number of agents installed.')
    parser.add_argument(
        '-f', '--sub-folder', dest='sub_folder', type=str, required=False,
        help='By default, the logs will be placed directly under _LOG_LOCATION'
             ' as defined in config.py. This option allows you to specify a '
             'folder inside of _LOG_LOCATION to place the logs into.')
    return parser.parse_args()


if __name__ == '__main__':
    _main()
