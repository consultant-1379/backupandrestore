#!/usr/bin/env python3

"""
Module for executing command line commands
"""

import subprocess

from bro_logging import logger


def execute_command(command):
    """
    Executes commands.

    :param command: Command to be executed
    :returns: Output
    """
    logger.log('Executing: ' + '"' + command + '"\n')
    try:
        output = subprocess.check_output(
            command.split(' '),
            stderr=subprocess.STDOUT, encoding='utf-8',
            universal_newlines=True)
        logger.log(output)
        return output
    except subprocess.CalledProcessError as error:
        logger.log(error.output)
        raise ValueError('There was an error while trying to execute the '
                         'command "{}"'.format(command)) from error


def execute_command_send_to_file(command, output_file_path):
    """
    Executes a command and directs the output to a file.

    :param command: Command to be executed
    :param output_file_path: File to direct output to
    """
    logger.log('Executing: ' + '"' + command + '"\n')
    try:
        with open(output_file_path, 'a+') as out_file:
            subprocess.run(
                command.split(' '),
                encoding='UTF-8',
                check=True,
                stdout=out_file,
                stderr=out_file)
    except subprocess.CalledProcessError as error:
        logger.log(error.output.decode('UTF-8'))
        raise ValueError('There was an error while trying to execute the '
                         'command "{}"'.format(command)) from error


def execute_command_async(command):
    """
    Executes a command asynchronously.

    :param command: Command to be executed asynchronously
    """
    logger.log('Executing: ' + '"' + command + '" asynchronously\n')
    return subprocess.Popen(command.split(' '))


if __name__ == '__main__':
    print('You cannot use cmd_util.py on the command line!')
