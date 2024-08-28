#!/usr/bin/env python3

"""
Module for performing a backup
"""

import requests

from bro_logging import logger
from utils import k8s_util


def backup(backup_name):
    """
    Tells the Orchestrator to do a backup

    :param backup_name: backup name
    """

    logger.log('Performing backup with backup name of: {}'.format(backup_name))

    body = {
        "action": "CREATE_BACKUP",
        "payload": {
            "backupName": backup_name
        }
    }
    k8s_util.start_port_forward()
    try:
        resp = requests.post(
            "http://localhost:7001/v1/backup-manager/DEFAULT/action",
            None,
            body)
        resp.raise_for_status()
        action_id = resp.json()["id"]
        logger.log('Backup action created with action Id of: {}'.format(
            action_id))
        return action_id
    except Exception as error:
        logger.log('Failed to execute backup')
        logger.log(str(error))
        raise
    finally:
        k8s_util.stop_port_forward()


if __name__ == '__main__':
    print('You cannot use execute_backup.py on the command line!')
