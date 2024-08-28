#!/usr/bin/env python3

"""
Module for installing the agents
"""

import argparse

import config
from utils import cmd_util
from bro_logging import logger


def install(number_of_agents, backup_sizes):
    """
    Installs the agents

    :param number_of_agents: the number of agents to be installed
    :param backup_sizes: a list of the sizes of the backups for each agent
    """
    logger.log('Installing {} agents'.format(number_of_agents))
    for agent_number in range(number_of_agents):
        install_cmd = config.AGENT_INSTALL_CMD.format(
            config.AGENT_RELEASE_NAME.format(agent_number),
            backup_sizes[agent_number])
        cmd_util.execute_command(install_cmd)


def _main():
    """
    Main method
    """
    args = _get_cli_args()
    number_of_agents = int(args.num_agents)
    backup_sizes = []
    for backup_size in args.backup_sizes:
        backup_sizes.append(int(backup_size))
    assert number_of_agents == len(backup_sizes)
    install(number_of_agents, backup_sizes)


def _get_cli_args():
    """
    Retrieves the command line arguments. Only used if this file is the
    __main__ file.
    """
    parser = argparse.ArgumentParser(
        description='Installs agents. Eg:\n'
                    '  install_agents.py 3 1024 1024 5120',
        formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument(
        'num_agents', type=str, help='The number of agents you want to delete')
    parser.add_argument(
        'backup_sizes', nargs='+',
        help='The sizes of the backups that you want each agent to generate')
    return parser.parse_args()


if __name__ == '__main__':
    _main()
