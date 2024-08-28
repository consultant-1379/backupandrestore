#!/usr/bin/env python3

"""
Module for conducting a performance test of a backup
"""

import argparse
import datetime

import config
import install_agents
import get_pod_logs
from bro import execute_backup
from bro import bro_utils
from bro_logging import logger


def run_test(backup_name, number_of_agents, backup_sizes):
    """
    Runs a performance test of a backup

    :param backup_name: name of the backup to be created
    :param number_of_agents: the number of agents to be installed
    :param backup_sizes: a list of the sizes of the backups for each agent
    """
    logger.log('Starting backup performance test run')
    logger.log('Backup name: {}'.format(backup_name))
    logger.log('Number of Agents: {}'.format(number_of_agents))
    logger.log('Agent Sizes:')
    for backup_size in backup_sizes:
        logger.log('    {}'.format(backup_size))

    install_agents.install(number_of_agents, backup_sizes)
    bro_utils.wait_for_agents_to_register(number_of_agents)

    logger.backup_log('Backup name: {}'.format(backup_name))
    logger.backup_log('Number of Agents: {}'.format(number_of_agents))
    logger.backup_log('Agent Sizes:')
    for backup_size in backup_sizes:
        logger.backup_log('    {}'.format(backup_size))

    start_time = _log_current_time('Start time:')

    action_id = execute_backup.backup(backup_name)
    try:
        bro_utils.wait_for_action_to_complete(action_id)
    except Exception as error:
        logger.log('Error while waiting for backup to complete')
        logger.log(str(error))
        get_pod_logs.get_logs(number_of_agents, config.BACKUP_NAME)
        raise

    finish_time = _log_current_time('Finish time:')

    duration = finish_time - start_time
    logger.backup_log('Duration:')
    logger.backup_log(str(duration))


def _log_current_time(label):
    """
    Logs the current time in the backup log file

    :param label: A label to put in front of the time
    """
    current_time = datetime.datetime.now()
    logger.backup_log(label)
    logger.backup_log(str(current_time))
    return current_time


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
    run_test(args.backup_name, number_of_agents, backup_sizes)


def _get_cli_args():
    """
    Retrieves the command line arguments. Only used if this file is the
    __main__ file.
    """
    parser = argparse.ArgumentParser(
        description='Installs the necessary agents and performs a backup. It '
                    'does not delete the agents afterwards. It also does not '
                    'delete the backup afterwards.\n'
                    'Pre-requisites:\n'
                    ' - The Orchestrator is installed',
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
