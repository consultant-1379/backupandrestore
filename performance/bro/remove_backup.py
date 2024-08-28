#!/usr/bin/env python3

"""
Module for deleting a backup
"""


import requests

from bro import bro_utils
from bro_logging import logger
from utils import k8s_util


def remove(backup_name):
    """
    Deletes a backup from the Orchestrator

    :param backup_name: Name of the backup to delete
    """
    body = {
        "action": "DELETE_BACKUP",
        "payload": {
            "backupName": backup_name
        }
    }
    action_id = None
    k8s_util.start_port_forward()
    try:
        resp = requests.post(
            "http://localhost:7001/v1/backup-manager/DEFAULT/action",
            None, body)
        resp.raise_for_status()
        action_id = resp.json()["id"]
        logger.log(
            'Action created to delete backup <{}>. Action Id: {}'.format(
                backup_name,
                action_id
            ))
    except Exception as error:
        logger.log('Failed to remove backup')
        logger.log(str(error))
        raise
    finally:
        k8s_util.stop_port_forward()
    bro_utils.wait_for_action_to_complete(action_id)


if __name__ == '__main__':
    print('You cannot use remove_backup.py on the command line!')
