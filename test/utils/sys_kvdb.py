#!/usr/bin/env python3
"""
This module provides methods to verify the integration of
Key Value Database AG with the Orchestrator.
"""

import json
import time

import k8sclient
import rest_utils

DATA = {"1": "one", "2": "two", "3": "three", "4": "four", "5": "five",
        "6": "six", "7": "seven", "8": "eight", "9": "nine", "10": "ten"}

BASE_URL = "http://{}:8080/kvdb-ag/management/v1/gfsh-commands/"


def setup_kvdb(namespace):
    """
    Setup kvdb

    :param namespace: namespace
    """
    cluster_ip = k8sclient.KubernetesClient().get_service_cluster_ip(
        "eric-data-kvdb-ag-admin-mgr", namespace)

    global BASE_URL
    BASE_URL = BASE_URL.format(cluster_ip)

    add_region()
    add_data()


def add_region():
    """
    Add region to kvdb
    """
    body = {"command":
            "create region --name=example-region --type=PARTITION_PERSISTENT"}

    out = rest_utils.rest_request(rest_utils.REQUESTS.post,
                                  "Adding region to kvdb",
                                  BASE_URL, body=body)

    check_request_status(json.loads(out))


def add_data():
    """
    Add data into the created region
    """
    for key, value in DATA.items():
        body = {"command": "put --region=example-region --key={} --value={}"
                           .format(key, value)}

        out = rest_utils.rest_request(rest_utils.REQUESTS.post,
                                      "Adding key {} to kvdb".format(key),
                                      BASE_URL, body=body)

        check_request_status(json.loads(out))


def delete_data():
    """
    Delete data from the region
    """
    for key in range(5, 8):
        body = {
            "command": "remove --region=example-region --key={}".format(key)}
        out = rest_utils.rest_request(rest_utils.REQUESTS.post,
                                      "Delete key {} from kvdb".format(key),
                                      BASE_URL, body=body)

        check_request_status(json.loads(out))


def verify_data():
    """
    Get data from the region
    """
    for key in range(5, 8):
        body = {
            "command": "get --region=example-region --key={}".format(key)}

        out = rest_utils.rest_request(rest_utils.REQUESTS.post,
                                      "Get key {} from kvdb".format(key),
                                      BASE_URL, body=body)

        check_request_status(json.loads(out))


def check_request_status(response):
    """
    Check the status of the rest response

    :param response: json response containing commandId
    """
    url = BASE_URL + response["commandId"]

    count = 0
    result = None
    while count < 10:
        result = rest_utils.rest_request(
            rest_utils.REQUESTS.get,
            "Check status of command {}".format(response["commandId"]), url)

        if result["executionStatus"] == "EXECUTING":
            count = count + 1
            time.sleep(1)
        else:
            break

    if result["executionStatus"] != "EXECUTED" or result["statusCode"] != 0:
        raise RuntimeError(
            'Request to execute command {} is unsuccessful'
            .format(result["command"]))

    if "false" in result["output"]:
        raise RuntimeError(
            'Request to execute command {} is unsuccessful'
            .format(result["command"]))
