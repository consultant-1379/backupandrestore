#!/usr/bin/env python3

"""
Module for running a full performance test
"""

import argparse

import config
from bro_logging import logger
import backup_perf_test
import restore_perf_test
from bro import remove_backup
import delete_agents


def full_test(backup_name, number_of_agents, *backup_sizes):
    """
    Runs a full performance test of backup and restore

    :param backup_name: name of the backup to be created
    :param number_of_agents: the number of agents to be installed
    :param backup_sizes: a list of the sizes of the backups for each agent
    """
    logger.log('Running the full performance tests.')
    backup_perf_test.run_test(backup_name, number_of_agents, *backup_sizes)
    restore_perf_test.run_test(backup_name, number_of_agents)
    remove_backup.remove(backup_name)
    delete_agents.delete(number_of_agents)
    logger.log('Full performance tests finished running.')


def _main():
    """
    Main method
    """
    args = _get_cli_args()
    config.BACKUP_NAME = args.backup_name
    config.OUTPUT_TO_LOG_FILE = True
    number_of_agents = int(args.num_agents)
    backup_sizes = []
    for backup_size in args.backup_sizes:
        backup_sizes.append(int(backup_size))
    assert number_of_agents == len(backup_sizes),\
        'The number of agents must match the number of backup sizes that you '\
        'have given'
    full_test(args.backup_name, number_of_agents, backup_sizes)


def _get_cli_args():
    """
    Retrieves the command line arguments.
    """
    parser = argparse.ArgumentParser(
        description='Does a full backup and restore test run. This script:\n'
                    ' - Installs the necessary agents\n'
                    ' - Performs a backup\n'
                    ' - Performs a restore\n'
                    ' - Deletes the backup\n'
                    ' - Deletes the agents\n'
                    'Pre-requisites:'
                    ' - The Orchestrator is already installed\n',
        formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument(
        'backup_name', type=str,
        help='The name of the backup')
    parser.add_argument(
        'num_agents', type=str,
        help='The number of agents you want to delete')
    parser.add_argument(
        'backup_sizes', nargs='+',
        help='The sizes of the backups that you want each agent to generate')
    return parser.parse_args()


if __name__ == '__main__':
    _main()
