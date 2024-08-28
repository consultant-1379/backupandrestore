#!/usr/bin/env python3

"""
Module for managing the configuration of the performance tests
"""

# Mandatory config values for performance tests
NAMESPACE = ''
AGENT_INSTALL_CMD = ''
AGENT_RELEASE_NAME = ''

# Optional config values for performance tests
_LOG_LOCATION = ''

# Optional config values for additional scripts
ORCH_INSTALL_CMD = ''
ORCH_RELEASE_NAME = ''

##############################################################################
# All values below this point should ONLY be set at runtime by the scripts
##############################################################################

BACKUP_NAME = ''
OUTPUT_TO_LOG_FILE = False


def verify_mandatory_config_values():
    """
    Checks the mandatory config values to make sure they are valid
    """
    config_values = [
        NAMESPACE,
        AGENT_RELEASE_NAME,
        AGENT_INSTALL_CMD
    ]
    for value in config_values:
        if not value:
            raise ValueError('A config value was not set')


def verify_orch_install_values():
    """
    Checks the optional config values relating to installation of the
    orchestrator to make sure they are valid
    """
    config_values = [
        NAMESPACE,
        ORCH_RELEASE_NAME,
        ORCH_INSTALL_CMD
    ]
    for value in config_values:
        if not value:
            raise ValueError('A config value was not set')


def log_location():
    """
    Retrieves the file system location where the logs will be written
    :return: path to the log location
    """
    if not _LOG_LOCATION:
        return './performance_logs'
    return _LOG_LOCATION


if __name__ == '__main__':
    print('You cannot use config.py on the command line!')
