#!/usr/bin/env python3
"""
This module provides helper function to validate the Backup and Restore
Orchestrator Prometheus metrics, using the Prometheus API query.
"""
from bro_utils import assert_equal
import rest_utils
from utilprocs import log


class PMClient:
    """
    This class provides methods to handle actions and other rest calls towards
    Prometheus.
    """
    def __init__(self):
        # Note: additional { and } below are used as escape characters
        self.url_template = (
            "http://eric-pm-server:9090/api/v1/query?query="
            "{{kubernetes_namespace=\"\",__name__=\"{}\"}}")

    def validate_specific_metric(self, expected, specific_metric):
        """
        This function will validate any metric that returns a single value
        :param expected: expected value of query as a string
        :param specific_metric: the BRO specific metric you are querying
        """
        log("Validating specific metric: {}".format(specific_metric))
        query_url = self.url_template.format(specific_metric)

        # Retrieve value from metric
        metrics_array = get_api_response(specific_metric, query_url)
        metric_value = metrics_array[0]["value"][1]
        log("Value for {} retrieved was {}".format(
            specific_metric, metric_value))

        # Verify metric_value is the expected value
        assert_equal(metric_value, expected)

    def get_specific_metric_for_specified_label(
            self, metric, label, value):
        """
        This function will validate the value of a metric for a specified
        attribute
        :param metric: the specific metric in the query
        :param label: string/s for the metric attribute in the query
        :param value: the value/s for the label in the query
        :return total_operations
        """
        # create queries extension if type of label/value is list or string
        query_extension = ""
        if isinstance(label, list) and isinstance(value, list):
            zipped = zip(label, value)
            for temp_label, temp_value in zipped:
                query_extension += ",{}=\"{}\"".format(temp_label, temp_value)
        else:
            query_extension += ",{}=\"{}\"".format(label, value)
        # Strip trailing "
        query_extension = query_extension.rstrip(query_extension[-1])
        # create queries url
        specific_metric = "{}\"{}".format(
            metric, query_extension)
        log("Validating specific metric: {} for {}: {}".format(
            metric, label, value))
        query_url = self.url_template.format(specific_metric)

        # Retrieve value from metric
        operations_array = get_api_response(specific_metric, query_url)
        total_operations = 0
        for operation in operations_array:
            # As metric["value"] will return the number (as a string)
            # of actions of each stage add these items
            total_operations += int(operation["value"][1])

        log("Value for {} for {}: {} retrieved was {}".format(
            metric, label, value, total_operations))

        return total_operations

    def validate_specific_metric_for_specified_label(
            self, expected, metric, label, value):
        """
        This function will validate the value of a metric for a specified
        attribute
        :param expected: expected value of the metric as a string
        :param metric: the specific metric in the query
        :param label: string/s for the metric attribute in the query
        :param value: the value/s for the label in the query
        """
        total_operations = self.get_specific_metric_for_specified_label(
            metric, label, value)

        # Verify total_operations is the expected value
        assert_equal(total_operations, int(expected))

    def validate_disk_usage_bytes(self, expected):
        """
        This function will validate the "bro_disk_usage_bytes" for each BRM
        :param expected: The expected list of JSONs as
                         {BRM_id: <brm_id>, Value: <Str(expected_value)>
        """
        specific_metric = "bro_disk_usage_bytes"
        log("Validating specific metric: {}".format(specific_metric))
        query_url = self.url_template.format(specific_metric)

        # Retrieve metric blocks from the array
        brm_array = get_api_response(specific_metric, query_url)
        # Build array for each BRM's disk usage
        brm_metrics = []
        for brm in brm_array:
            brm_bytes_used = {
                "BRM_id": brm["metric"]["backup_type"],
                "Value": brm["value"][1]
            }
            brm_metrics.append(brm_bytes_used)
        log("Value/s for {} retrieved was {}".format(
            specific_metric, brm_metrics))

        expected = sorted(expected, key=lambda agent: agent["BRM_id"])

        # Verify brm_metrics is the expected value
        assert_equal(brm_metrics, expected)

    def validate_bro_registered_agents(self, expected):
        """
        This function will validate the registered agents metric
        :param expected: List of expected registered agents

        """
        specific_metric = "bro_registered_agents"
        log("Validating specific metric: {}".format(specific_metric))
        query_url = self.url_template.format(specific_metric)

        # Retrieve the metric blocks from the array
        registered_agent_array = get_api_response(specific_metric, query_url)

        # Append agent to an array
        registered_agents = []
        for agent in registered_agent_array:
            registered_agents.append(agent["metric"]["agent"])
        registered_agents.sort()  # Sort to be in alphabetical order
        log("Value/s for {} retrieved was {}".format(
            specific_metric, registered_agents))

        # If this function is passed a list of strings in the
        # expected list of agents we can sort as normal
        if all(isinstance(agent, str) for agent in expected):
            expected.sort()
        else:
            # Else we will sort it based on the BRM_id
            expected = sorted(expected, key=lambda agent: agent["BRM_id"])

        assert_equal(registered_agents, expected)

    def validate_bro_granular_stage_info_for_action_type(
            self, expected, action):
        """
        This function will validate the total number of stages in
        "bro_granular_stage_info" of a specified action type
        :param expected: expected number of stages for all
                         actions of given type (as a string)
        :param action: the action type you want for the query
        """
        specific_metric = "{}\",action=\"{}".format(
            "bro_granular_stage_info", action)
        log("Validating specific metric: {} for action: {}".format(
            "bro_granular_stage_info", action))
        query_url = self.url_template.format(specific_metric)

        # Retrieve value from metric
        metrics_array = get_api_response(specific_metric, query_url)
        total = len(metrics_array)

        log("Total stages for {} retrieved was {}".format(
            specific_metric, total))

        # Verify total is the expected value
        assert_equal(total, int(expected))

    def validate_bro_disk_usage_bytes_for_specified_brm(self, brm_id):
        """
        This function will validate the "bro_disk_usage_bytes" of a
        specified brm when it hold a backup
        :param brm_id: the ID of the BRM you are querying
        """
        specific_metric = "{}\",backup_type=\"{}".format(
            "bro_disk_usage_bytes", brm_id)
        log("Validating specific metric: \"{} for brm: \"{}".format(
            "bro_disk_usage_bytes", brm_id))
        query_url = self.url_template.format(specific_metric)

        # Retrieve value from metric
        metrics_array = get_api_response(specific_metric, query_url)
        disk_usage = metrics_array[0]["value"][1]
        log("Total disk usage for {} retrieved was {}".format(
            brm_id, disk_usage))

        # Verify specific_metric is the expected value
        assert int(disk_usage) > 0, \
            "Value for {} metric was {} but expected greater than 0".format(
                specific_metric, disk_usage)


# Helper functions
def get_api_response(specific_metric, query_url):
    """
    Call the Prometheus API
    """
    # Get response from API query
    response = rest_utils.rest_request(
        rest_utils.REQUESTS.get, "Retrieve {} metric PM".format(
            specific_metric), query_url)

    # Parse to only relevant items and return
    return response["data"]["result"]
