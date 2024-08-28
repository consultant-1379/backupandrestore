"""
This module provides methods to verify the integration of
CM Mediator with the Orchestrator.
"""
import json
import time
from requests.exceptions import HTTPError

import rest_utils
import utilprocs

CM_URL = 'http://eric-cm-mediator:5003/cm/api/v1/'
CM_SCHEMA_URL = CM_URL + 'schemas'
CM_CONFIG_URL = CM_URL + 'configurations'
CM_SUBSCRIPTION_URL = CM_URL + 'subscriptions'

BRO_SCHEMA_NAME = 'ericsson-brm'
BRO_CONFIGURATION_NAME = 'ericsson-brm'
BRO_SCHEMA_URL = CM_SCHEMA_URL + '/' + BRO_SCHEMA_NAME
BRO_CONFIGURATION_URL = CM_CONFIG_URL + '/' + BRO_CONFIGURATION_NAME

TEST_SCHEMA_NAME = 'ericsson-bro-test'
TEST_CONFIGURATION_NAME = 'ericsson-bro-test'
TEST_SUBSCRIPTION_NAME = 'example-sub'
TEST_SCHEMA_URL = CM_SCHEMA_URL + '/' + TEST_SCHEMA_NAME
TEST_CONFIGURATION_URL = CM_CONFIG_URL + '/' + TEST_CONFIGURATION_NAME
TEST_SUBSCRIPTION_URL = CM_SUBSCRIPTION_URL + '/' + TEST_SUBSCRIPTION_NAME

TEST_SCHEMA_FILE = "test_cm_schema.json"
TEST_CONFIGURATION_FILE = "test_cm_configuration.json"
TEST_SUBSCRIPTION_FILE = "test_cm_subscription.json"


def verify_bro_schema():
    """
    Verifies that the BRO json schema has been pushed to CM Mediator
    """
    retries = 0
    while True:
        try:
            schema = get_schema(BRO_SCHEMA_NAME)
            break
        except HTTPError:
            if retries >= 20:
                raise
            time.sleep(2)
            retries += 1

    if schema['name'] != BRO_SCHEMA_NAME:
        utilprocs.log('Schema retrieved from CM mediator: {}'.format(schema))
        error_message = 'Invalid schema: Expected schema name: "{}", ' \
                        'Actual schema name: "{}"'
        raise ValueError(error_message.format(schema['name'], BRO_SCHEMA_NAME))


def delete_bro_configuration():
    """
    Deletes BRM configuration from CM Mediator
    """
    delete_cm_resource(BRO_CONFIGURATION_URL,
                       'Deleting BRM configuration from CM Mediator')


def create_test_data():
    """
    Creates a test schema, configuration and subscription in CM Mediator
    """
    create_test_schema()
    create_test_configuration()
    create_test_subscription()


def remove_test_data():
    """
    Deletes the test schema, configuration and subscription from CM Mediator
    """
    delete_cm_resource(TEST_SUBSCRIPTION_URL,
                       "Deleting test subscription from CM Mediator")
    delete_cm_resource(TEST_CONFIGURATION_URL,
                       "Deleting test configuration from CM Mediator")
    delete_cm_resource(TEST_SCHEMA_URL,
                       "Deleting test schema from CM Mediator")


def verify_test_data():
    """
    Verifies the test data in CM Mediator
    """
    verify_test_schema()
    verify_test_configuration()
    verify_cm_resource_does_not_exist(TEST_SUBSCRIPTION_URL)


def get_schema(schema_name):
    """
    Retrieves a json schema from CM Mediator

    :param schema_name : The name of the schema to retrieve
    :returns: a dict representing the schema
    """
    log_message = 'Attempting to retrieve schema  with name of {} from CM' \
                  'Mediator'.format(schema_name)
    url = "{}/{}".format(CM_SCHEMA_URL, schema_name)
    return rest_utils.rest_request(rest_utils.REQUESTS.get, log_message, url)


def get_configuration(config_name):
    """
    Retrieves a configuration from CM Mediator

    :param config_name : The name of the configuration to retrieve
    :returns: a dict representing the configuration
    """
    log_message = 'Attempting to retrieve configuration  with name of {} ' \
                  'from CM Mediator'.format(config_name)
    url = "{}/{}".format(CM_CONFIG_URL, config_name)
    return rest_utils.rest_request(rest_utils.REQUESTS.get, log_message, url)


def create_test_schema():
    """
    Create a test schema
    """
    schema = read_json_file(TEST_SCHEMA_FILE)
    create_cm_resource(CM_SCHEMA_URL,
                       "Creating test schema in CM Mediator",
                       schema)


def create_test_configuration():
    """
    Create a test configuration
    """
    configuration = read_json_file(TEST_CONFIGURATION_FILE)
    create_cm_resource(CM_CONFIG_URL,
                       "Creating test configuration in CM Mediator",
                       configuration)


def create_test_subscription():
    """
    Create a test subscription
    """
    subscription = read_json_file(TEST_SUBSCRIPTION_FILE)
    create_cm_resource(CM_SUBSCRIPTION_URL,
                       "Creating test subscription in CM Mediator",
                       subscription)


def verify_test_schema():
    """
    Verifies that the test schema in CM Mediator matches the expected schema
    """
    expected_schema = read_json_file(TEST_SCHEMA_FILE)
    actual_schema = get_schema(TEST_SCHEMA_NAME)
    del actual_schema["createTime"]
    del actual_schema["modifiedTime"]
    assert expected_schema == actual_schema, \
        "The schema in CM Mediator did not match the expected schema. " \
        "Expected: {}. Actual: {}".format(expected_schema, actual_schema)


def verify_test_configuration():
    """
    Verifies that the test configuration in CM Mediator matches the expected
    configuration
    """
    expected_config = read_json_file(TEST_CONFIGURATION_FILE)
    actual_config = get_configuration(TEST_CONFIGURATION_NAME)
    assert expected_config == actual_config, \
        "The configuration in CM Mediator did not match the expected " \
        "configuration. Expected: {}. Actual: {}".format(expected_config,
                                                         actual_config)


def verify_test_subscription():
    """
    Verifies that the test subscription does not exist in CM Mediator
    """
    verify_cm_resource_does_not_exist(TEST_SUBSCRIPTION_URL)


def create_cm_resource(url, log, body):
    """
    Creates a resource in CM Mediator

    :param url : the url of the resource to create
    :param log : the message to log when creating the resource
    :param body : the body of the request to send to CM Mediator
    """
    rest_utils.rest_request(rest_utils.REQUESTS.post, log, url, body=body)


def delete_cm_resource(url, log):
    """
    Deletes a resource from CM Mediator

    :param url : the url of the resource to delete
    :param log : the message to log when deleting the resource
    """
    rest_utils.rest_request(rest_utils.REQUESTS.delete, log, url)
    verify_cm_resource_does_not_exist(url)


def verify_cm_resource_does_not_exist(resource_url):
    """
    Verifies that a resource does not exist in CM Mediator

    :param resource_url : the url of the resource to verify
    """
    resource_exists = True
    try:
        log_message = 'Attempting to retrieve resource at {} ' \
                      'from CM Mediator'.format(resource_url)
        rest_utils.rest_request(rest_utils.REQUESTS.get, log_message,
                                resource_url)
    except HTTPError as error:
        if error.response.status_code == 404:
            resource_exists = False
        else:
            raise
    assert resource_exists is False, \
        "The resource at {} still exists in CM Mediator.".format(resource_url)


def read_json_file(file_name):
    """
    Reads a json file and returns its contents as a dict

    :param file_name : the name of the file to read
    :returns : dict containing the contents of the file
    """
    with open(file_name) as file:
        return json.load(file)
