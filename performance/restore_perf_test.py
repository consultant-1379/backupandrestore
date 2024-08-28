#!/usr/bin/env python3

"""
Module for conducting a performance test of a restore
"""

import argparse
import datetime

import config
import get_pod_logs
from bro import execute_restore
from bro import bro_utils
from bro_logging import logger


def run_test(backup_name, number_of_agents):
    """
    Runs a performance test of a restore

    :param backup_name: name of the backup to be created
    :param number_of_agents: the number of agents to be installed
    """
    logger.log('Starting restore performance test run')
    logger.log('Backup name: {}'.format(backup_name))

    logger.log('Ensuring that the correct number of agents are registered.')
    logger.log('If this step runs forever, check that the correct agents are '
               'installed.')
    bro_utils.wait_for_agents_to_register(number_of_agents)

    logger.restore_log('Backup name: {}'.format(backup_name))
    logger.restore_log('Number of Agents: {}'.format(number_of_agents))

    start_time = _log_current_time('Start time:')

    action_id = execute_restore.restore(backup_name)
    try:
        bro_utils.wait_for_action_to_complete(action_id)
    except Exception as error:
        logger.log('Error while waiting for restore to complete')
        logger.log(str(error))
        get_pod_logs.get_logs(number_of_agents, config.BACKUP_NAME)
        raise

    finish_time = _log_current_time('Finish time:')

    duration = finish_time - start_time
    logger.restore_log('Duration:')
    logger.restore_log(str(duration))


def _log_current_time(label):
    """
    Logs the current time in the backup log file

    :param label: A label to put in front of the time
    """
    current_time = datetime.datetime.now()
    logger.restore_log(label)
    logger.restore_log(str(current_time))
    return current_time


def _main():
    """
    Main method
    """
    args = _get_cli_args()
    config.BACKUP_NAME = args.backup_name
    config.OUTPUT_TO_LOG_FILE = True
    number_of_agents = int(args.num_agents)
    run_test(args.backup_name, number_of_agents)


def _get_cli_args():
    """
    Retrieves the command line arguments. Only used if this file is the
    __main__ file.
    """
    parser = argparse.ArgumentParser(
        description='Performs a restore. It does not delete the agents '
                    'afterwards. It also does not delete the backup '
                    'afterwards.\n'
                    'Pre-requisites: \n'
                    ' - The orchestrator is installed\n'
                    ' - The agents are installed\n'
                    ' - A backup has already been taken',
        formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument(
        'backup_name', type=str,
        help='The name of the backup you want to restore')
    parser.add_argument(
        'num_agents', type=str,
        help='The number of agents you want to delete')
    return parser.parse_args()


if __name__ == '__main__':
    _main()
