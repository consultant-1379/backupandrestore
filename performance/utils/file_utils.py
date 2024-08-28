#!/usr/bin/env python3

"""
Module for common file operations
"""

import os


def create_directories(path):
    """
    Creates a directory recursively

    :param path: path
    """
    if not os.path.exists(path):
        os.makedirs(path)


if __name__ == '__main__':
    print('You cannot use file_utils.py on the command line!')
