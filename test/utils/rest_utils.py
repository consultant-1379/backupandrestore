"""
This module provides methods for making http requests to REST endpoints
"""

from enum import Enum
import json
import requests
import utilprocs

# enum for request type
REQUESTS = Enum('RestCall', 'get post delete put patch')


def rest_request(request, log, url, body=None, ca_cert=None,
                 raise_for_status=True, raise_for_connection=True,
                 headers=None, content_type_json=True):
    """
    Method to exercise the REST endpoints.

    :param request: Enum indicating request type
    :param url: REST endpoint
    :param log: string to be logged
    :param body: request body for POST calls
    :param ca_cert: path to the trusted root CA certificate that HTTPS requests
                    should be verified against
    :param raise_for_status: Specifies if an exception should be raised if the
                             response is a HTTP error response
    :param raise_for_connection: Specifies if an exception should be raised if
                                 a connection error occurs
    :param headers: Supplied headers for the request or the default
                    content-type application/json will be used
    :param content_type_json: Set to false if the content type passed
                              is not json

    :return: dict of the orchestrator response for GET calls
             string for POST and DELETE calls
    """

    response = ""
    if headers is None:
        header = {'content-type': 'application/json'}
    else:
        header = headers

    # log request
    utilprocs.log("{} : {}".format(log, url))

    try:
        if request == REQUESTS.get:
            response = requests.get(url, verify=ca_cert)
            utilprocs.log("Got response {0}".format(response.text))
            raise_status(raise_for_status, response)
            # return the response as a dictionary
            return response.json()

        if request == REQUESTS.post:
            if content_type_json:
                response = requests.post(url, data=json.dumps(body),
                                         headers=header,
                                         verify=ca_cert)
            else:
                response = requests.post(url, data=body,
                                         headers=header,
                                         verify=ca_cert)
        elif request == REQUESTS.delete:
            response = requests.delete(url, verify=ca_cert)
        elif request == REQUESTS.put:
            response = requests.put(url, data=json.dumps(body),
                                    headers=header, verify=ca_cert)
        elif request == REQUESTS.patch:
            response = requests.patch(url, data=json.dumps(body),
                                      headers=header, verify=ca_cert)

    except requests.ConnectionError as err:
        utilprocs.log("Received ConnectionError: {}"
                      .format(err))
        if raise_for_connection:
            raise

    utilprocs.log("Got response {0}".format(response.text))
    raise_status(raise_for_status, response)

    return response.text


def raise_status(raise_for_status, response):
    """
    Raise status if there is error in response

    :param raise_for_status: boolean
    :param response: response from request
    """
    if raise_for_status:
        response.raise_for_status()


def get_resource(resource_url, ca_cert=None):
    """
    Performs a REST GET call on the resource URL.

    :param resource_url: url of the resource to get
    :param ca_cert: path to the trusted root CA certificate that HTTPS requests
                    should be verified against

    :return: returns the GET output
    """

    out = rest_request(REQUESTS.get,
                       "Get resource {}".format(resource_url),
                       resource_url,
                       ca_cert=ca_cert)
    return out


def update_resource(resource_url, data, ca_cert=None):
    """
    Updates the resource with the json data supplied.

    :param resource_url: url of the resource to update
    :param data: json data to update on the resource
    :param ca_cert: path to the trusted root CA certificate that HTTPS requests
                    should be verified against
    """

    # Update Resource
    rest_request(REQUESTS.post,
                 "Update resource {} with {}"
                 .format(resource_url, data),
                 resource_url,
                 body=data,
                 ca_cert=ca_cert)
