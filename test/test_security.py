"""
This module deploys the Backup and Restore Orchestrator for security scans
"""
import os
import utilprocs
import helm3procs
import k8sclient
import bro_utils
import bro_ctrl
from globals import V4_FRAG_CM

# Get variables passed in from ruleset
NAMESPACE = os.environ.get("kubernetes_namespace")

# Set BRO variables
BRO_NAME = "eric-ctrl-bro"

BRO_CHART_REPO = "https://arm.sero.gic.ericsson.se/artifactory/" \
                 "proj-adp-eric-ctrl-bro-internal-helm"
BRO_CHART_REPO_NAME = "bro_repo"

BRO_CHART_VERSION = ""

# Set Test Agent
TEST_REPO = "https://arm.rnd.ki.sw.ericsson.se/artifactory/" \
            "proj-adp-eric-ctrl-bro-test-internal-helm"

TEST_CHART_NAME = "eric-test-service-basic"
TEST_REPO_NAME = "test_svc"
TEST_CHART_VERSION = ""

# Instantiate kube client
KUBE = k8sclient.KubernetesClient()
BRO_CTRL = bro_ctrl.BroCtrlClient()


def test_clear_any_failed_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    utilprocs.log("Test Case: test_clear_any_failed_resources")

    bro_utils.remove_namespace_resources(NAMESPACE, remove_redis_cluster=False)
    utilprocs.log("Finished removing Kubernetes resources")


def test_deploy_bro_service():
    """
    Deploys the Orchestrator Service.
    """
    test_case = "test_deploy_bro_service"
    utilprocs.log("Test Case: {}".format(test_case))

    utilprocs.log("Add the BRO helm repo")
    global BRO_CHART_VERSION

    helm3procs.add_helm_repo(BRO_CHART_REPO, BRO_CHART_REPO_NAME)
    # Get latest BRO chart
    BRO_CHART_VERSION = \
        helm3procs.get_latest_chart_version(BRO_NAME,
                                            helm_repo_name=BRO_CHART_REPO_NAME,
                                            development_version=True)

    utilprocs.log("Latest BRO version found: {}. Installing..."
                  .format(BRO_CHART_VERSION))

    # Install Orchestrator Chart
    bro_utils.install_service_chart(BRO_CHART_REPO_NAME, BRO_NAME,
                                    BRO_CHART_VERSION, NAMESPACE,
                                    agent_discovery=True,
                                    enable_global_tls=False)

    # Collect console logs
    KUBE.get_pod_logs(NAMESPACE, "{}-0".format(BRO_NAME), test_case)


def test_deploy_test_agent():
    """
    Deploys Test Agent Helm Chart.
    """
    utilprocs.log("Test Case: test_deploy_test_agent")

    utilprocs.log("Add the test service helm repo")
    helm3procs.add_helm_repo(TEST_REPO, TEST_REPO_NAME)

    global TEST_CHART_VERSION
    TEST_CHART_VERSION = \
        helm3procs.get_latest_chart_version(TEST_CHART_NAME,
                                            helm_repo_name=TEST_REPO_NAME,
                                            development_version=True)

    # Deploy latest test agent
    bro_utils.install_bro_agents(TEST_REPO_NAME, TEST_CHART_NAME,
                                 TEST_CHART_VERSION, [V4_FRAG_CM["AgentId"]],
                                 NAMESPACE, enable_global_tls=False)

    # Verify that Orchestrator has all expected agents registered
    BRO_CTRL.wait_for_bro_agents_to_reconnect([V4_FRAG_CM["AgentId"]])


def test_create_backup():
    """
    Creates backup
    """
    utilprocs.log("Test Case: test_create_backup")
    BRO_CTRL.create_backup("bu1", [V4_FRAG_CM], NAMESPACE, test_agent=True)


def test_remove_k8s_resources():
    """
    Removes all kubernetes resources in the namespace.
    """
    utilprocs.log("Test Case: test_remove_k8s_resources")

    bro_utils.remove_namespace_resources(NAMESPACE)
    utilprocs.log("Finished removing Kubernetes resources")
