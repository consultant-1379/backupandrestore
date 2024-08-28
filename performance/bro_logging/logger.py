"""
Module responsible for performance test logging
"""

import config
from utils import file_utils


def log(message):
    """
    Logs a message

    :param message: message
    """
    config.verify_mandatory_config_values()
    _log('output.txt', message)


def backup_log(message):
    """
    Logs a message to the backup log file

    :param message: message
    """
    config.verify_mandatory_config_values()
    _log('backup.txt', message)


def restore_log(message):
    """
    Logs a message to the restore log file

    :param message: message
    """
    config.verify_mandatory_config_values()
    _log('restore.txt', message)


def _log(file_name, message):
    """
    Logs a message to a file

    :param file_name: Name of the log file
    :param message: message
    """
    if config.OUTPUT_TO_LOG_FILE:
        log_location = config.log_location() + '/' + config.BACKUP_NAME
        file_utils.create_directories(log_location)
        with open(log_location + '/' + file_name, 'a+') as log_file:
            log_file.write(message + '\n')
    print(message)


if __name__ == '__main__':
    print('You cannot use logger.py on the command line!')
