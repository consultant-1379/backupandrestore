#!/usr/bin/env python3

"""
Utilities for Kubernetes related operations
"""

import time
import sys

import requests
from kubernetes import client
from kubernetes import config
from kubernetes.client.rest import ApiException

from utils import cmd_util
from bro_logging import logger


_PORT_FORWARD_PROCESS = None
_CORE_V1 = None


def start_port_forward():
    """
    Starts port forwarding to the Orchestrator
    """
    global _PORT_FORWARD_PROCESS
    if _PORT_FORWARD_PROCESS is not None:
        raise ValueError('Error trying to port-forward: '
                         'Already port-forwarding')
    logger.log('Starting port-forwarding')
    _PORT_FORWARD_PROCESS = cmd_util.execute_command_async(
        'kubectl port-forward eric-ctrl-bro-0 7001:7001')
    _wait_for_port_to_open()


def _wait_for_port_to_open():
    """
    Waits until the port has opened
    """
    while True:
        try:
            resp = requests.get(
                "http://localhost:7001/v1/health", None)
            resp.raise_for_status()
            break
        except Exception:
            logger.log('Waiting for port to open')
            time.sleep(1)


def stop_port_forward():
    """
    Stops port forwarding to the Orchestrator
    """
    global _PORT_FORWARD_PROCESS
    logger.log('Ending port-forwarding')
    _PORT_FORWARD_PROCESS.terminate()
    _PORT_FORWARD_PROCESS = None


def list_pods_from_namespace(namespace):
    """
    Retrieves list of pods from the namespace

    :param namespace: namespace name
    :returns List of pods found
    """
    podlist_names = []
    exception_count = 3
    while True:
        try:
            podlist = _CORE_V1.list_namespaced_pod(
                namespace
            )
            for pod in podlist.items:
                podlist_names.append(pod.metadata.name)
            break
        except Exception as e_obj:
            logger.log(
                "Exception when trying to find pods "
                "in namespace:{} Error: {}".format(namespace, e_obj)
            )
            exception_count -= 1
            if exception_count <= 0:
                raise ApiException
            continue
    return podlist_names


def _init():
    """
    Initialises the kubernetes library
    """
    global _CORE_V1
    config.load_kube_config()
    _CORE_V1 = client.CoreV1Api()


if __name__ == '__main__':
    print('You cannot use k8s_util.py on the command line!')
    sys.exit(1)


_init()
