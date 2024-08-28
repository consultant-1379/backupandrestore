#!/usr/bin/env python3

"""
Module for interacting with the Orchestrator
"""

import time
import requests

from bro_logging import logger
from utils import k8s_util


def wait_for_agents_to_register(expected_number_of_agents):
    """
    Waits until all agents are registered

    :param expected_number_of_agents: The number of agents we are expecting to
    register
    """

    logger.log("Waiting until all agents have registered")

    health_url = "http://localhost:7001/v1/health"

    k8s_util.start_port_forward()
    while True:
        try:
            response = requests.get(health_url)

            # Raise if exception other than networking
            response.raise_for_status()
            agents = response.json()['registeredAgents']
            if len(agents) == expected_number_of_agents:
                logger.log("All agents have registered!")
                break
            time.sleep(5)

        except requests.ConnectionError:
            # log the networking exception
            logger.log("Received ConnectionError running health check")
            time.sleep(5)
    k8s_util.stop_port_forward()

    # return the response as a dictionary
    return response.json()


def wait_for_action_to_complete(action_id):
    """
    Waits unit an action has completed

    :param action_id: The Id of the action
    """
    url = "http://localhost:7001/v1/backup-manager/DEFAULT/action/" + action_id

    k8s_util.start_port_forward()
    logger.log("Waiting until the action <{}> is complete".format(
                action_id))
    while True:
        try:
            response = requests.get(url)

            # Raise if exception other than networking
            if response.status_code == 404:
                logger.log("The action has not been created yet")
                time.sleep(1)
                continue
            response.raise_for_status()
            resp = response.json()
            status = resp['state']
            if status == "FINISHED":
                logger.log("*********************************************")
                logger.log("Action Complete!")
                logger.log("Action result: " + resp["result"])
                logger.log("*********************************************")
                if resp["result"] == "FAILURE":
                    logger.log(str(resp))
                    raise ValueError('The action{} FAILED!'.format(action_id))
                break
            time.sleep(5)

        except requests.ConnectionError:
            # log the networking exception
            logger.log("Received ConnectionError checking action")
            time.sleep(5)
    k8s_util.stop_port_forward()

    # return the response as a dictionary
    return response.json()


if __name__ == '__main__':
    print('You cannot use perf_test_utils.py on the command line!')
