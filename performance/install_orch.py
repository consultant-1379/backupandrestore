#!/usr/bin/env python3

"""
Module for installing the Orchestrator
"""

import argparse

import config
from utils import cmd_util


USAGE = './install_orch.py <pvc_size>'


def install_orchestrator(pvc_size):
    """
    Installs the orchestrator

    :param pvc_size: The size of the PVC for the orchestrator
    """
    install_cmd = config.ORCH_INSTALL_CMD.format(config.ORCH_RELEASE_NAME,
                                                 pvc_size)
    cmd_util.execute_command(install_cmd)


def _main():
    """
    Main method
    """
    args = _get_cli_args()
    install_orchestrator(args.pvc_size)


def _get_cli_args():
    """
    Retrieves the command line arguments. Only used if this file is the
    __main__ file.
    """
    parser = argparse.ArgumentParser(
        description='Installs the Orchestrator based on the values that you '
                    'have set in config.py.')
    parser.add_argument('pvc_size', type=str,
                        help='The size in GB for how big the Orchestrator\'s '
                        'PVC will be')
    return parser.parse_args()


if __name__ == '__main__':
    _main()
