#!/usr/bin/env python3
"""
This module integrates methods to validate the BRO export/import feature.
The module creates a temporary kubernetes statefulset object to
enable the sftp server service with the following characteristics:
a) Generic name: bro-test-sftp-0.
b) Specific access users defined on the kubernetes secret
   "object pm-bro-test-sftp-users-secret".
c) Use of port 22 by default.

The kubernetes secret object is defined in the ruleset file on services and
the user file is defined as the credential "sftp-users-yaml" on jenkins server.
"""

import os
from kubernetes import client
from kubernetes.client.rest import ApiException

import bro_utils
from k8sclient import KubernetesClient
import utilprocs


class SFTPServer:
    """
    3pp sftp server application
    bro-test-sftp
    """
    KUBE = KubernetesClient()
    NAMESPACE = os.environ.get('kubernetes_namespace')
    SFTP_SECRETS = 'pm-bro-test-sftp-users-secret'
    SFTP_STATEFULSET = 'bro-test-sftp'
    SFTP_POD_NAME = "bro-test-sftp-0"
    SFTP_PORT = 22
    SFTP_IMG = 'armdocker.rnd.ericsson.se/' + \
               'proj-adp-eric-ctrl-bro-internal-dev/' + \
               'utils/pm/' + \
               'eric-pm-sftp:1.4.0-36'

    def __init__(self, username=None, password=None):
        """
        Starts the Sftp Server and open a connection
        to get into the server
        :param username: SFTP User need it to get access
        :param password: SFTP Password
        """

        self.user = username
        self.password = password

    def create_sftp_statefulset(self, context):
        """
        Creates and init the sftp statefulset service

        The SFTP service requires a couple of volumes to operate:
        - volume data: indicates the temporary sftp path to be used
        - volume config: Where the secret file is set, including the
                         users allowed to use the service
         The default port to be used is 22

        :param context:  Dictionary including basic statefulset settings
                         as: Name.
                             number of replicas.
                             sftp image.
        """

        # Defines the volumes mount settings used on the statefulset
        replicas = int(context["replicas"])
        metadata = client.V1ObjectMeta(name=context["name"])
        envlist = [client.V1EnvVar(name="USERFILE", value="true")]
        volumemount_data = client.V1VolumeMount(mount_path="/bro_test",
                                                name="bro-test-sftp-data")
        volumemount_config = client.V1VolumeMount(
            mount_path="/etc/opt/",
            name="bro-test-sftp-user-config-volume",
            read_only=True)
        volumemounts = [volumemount_data, volumemount_config]

        # Defines the volumes settings required for the statefulset
        # Volume data is an empty dir object

        volume_data = client.V1Volume(
            name="bro-test-sftp-data",
            empty_dir=client.V1EmptyDirVolumeSource())

        # Volume config requires the secret object to obtain
        # the users.yaml file.

        keypath = client.V1KeyToPath(key="users.yaml",
                                     path="pm_sftp_users.yaml")
        items = [keypath]
        secretvolume = client.V1SecretVolumeSource(
            secret_name="pm-bro-test-sftp-users-secret",
            items=items)
        volume_config = client.V1Volume(
            name="bro-test-sftp-user-config-volume",
            secret=secretvolume)

        # Set both volumes created for data and config
        volumes = [volume_data, volume_config]

        # Define all the statefulset specifications
        # replicas, volumemounts are set in the pod template container area
        # The readiness probe is TCP and looks for the port 22
        # volumes is used in the pod template area.
        spec = client.V1StatefulSetSpec(
            service_name=context["name"],
            replicas=replicas,
            selector={'matchLabels': {"app": context["name"],
                                      "release": context["name"]}},
            template=client.V1PodTemplateSpec(
                metadata=client.V1ObjectMeta(
                    labels={"app": context["name"],
                            "release": context["name"]}),
                spec=client.V1PodSpec(
                    containers=[
                        client.V1Container(
                            name=context["name"],
                            image=context["image"],
                            ports=[client.V1ContainerPort(container_port=22)],
                            env=envlist,
                            volume_mounts=volumemounts,
                            readiness_probe=client.V1Probe(
                                tcp_socket=client.V1TCPSocketAction(
                                    port=22
                                ),
                                initial_delay_seconds=10,
                                timeout_seconds=5,
                                period_seconds=30,
                                failure_threshold=5,
                            ),
                        )
                    ],
                    volumes=volumes,
                ),
            ),
        )

        # Creates and start the statefulset object in the namespace
        self.KUBE.create_namespace_statefulset(namespace=self.NAMESPACE,
                                               metadata=metadata,
                                               spec=spec)

    def start_sftp_server(self):
        """
        Start the third party SFTP server for test

        If there is any instance of the sftp service in execution,
        that one will be used instead, otherwise a new instance is started.
        By default the service name is bro-test-sftp

        """
        pods = bro_utils.get_pods_by_prefix(self.NAMESPACE, self.SFTP_POD_NAME)
        # look for any other sftp server running in the same namespace
        if not pods:
            try:
                context = {
                    'name': self.SFTP_STATEFULSET,
                    'replicas': '1',
                    'image': self.SFTP_IMG}
                utilprocs.log("Starts sftp server")
                self.create_sftp_statefulset(context)
            except ApiException as error:
                if error.status == 409:
                    utilprocs.log(
                        "Using the current sftp server instance - {}".format(
                            self.SFTP_POD_NAME)
                    )
                else:
                    utilprocs.log(
                        "Exception trying to start the sftp server - {}"
                        .format(error))
                    raise SystemError("Cannot start SFTP Server.") from error

    def wait_to_start(self):
        """
        Wait until the sftp pod server is up and running
        """
        self.KUBE.wait_for_pod_to_start(self.SFTP_POD_NAME,
                                        self.NAMESPACE,
                                        counter=12)

    def stop_sftp_server(self):
        """
        Stop the SFTP server.

        Delete the generated statefulset and stop the sftp pod.
        """
        utilprocs.log("Stopping sftp server")
        try:
            KubernetesClient. \
                delete_namespace_statefulset(name=SFTPServer.SFTP_STATEFULSET,
                                             namespace=SFTPServer.NAMESPACE)
            self.KUBE.shoot_pod(SFTPServer.SFTP_POD_NAME,
                                SFTPServer.NAMESPACE)
        except ApiException as error:
            utilprocs.log(
                (
                    "Exception trying to stop the ftp server "
                    "IP: {} - {}".format(SFTPServer.SFTP_STATEFULSET,
                                         error)
                )
            )

    def get_sftp_payload(self, sftp_path):
        """
        Returns a dictionary including the payload settings
        used in the sftp actions requests
        :param sftp_path: SFTP remote directory
        """
        sftp_url = self.get_sftp_uri_for_path(sftp_path)
        return {"uri": sftp_url, "password": self.password}

    def get_sftp_uri_for_path(self, sftp_path):
        """
        Returns a formatted URI for the sftp server including
        prefix, user name, host, port and path
        :param sftp_path: SFTP remote directory
        """
        sftp_ipaddress = self.get_sftp_ipaddress(namespace=self.NAMESPACE)
        sftp_url = "sftp://{}@{}:{}/{}"\
            .format(self.user, sftp_ipaddress, self.SFTP_PORT, sftp_path)
        return sftp_url

    def get_sftp_ipaddress(self, namespace):
        """
        Returns the ip address of the sftp server
        :param namespace: namespace name
        """
        sftp_ipaddress = self.KUBE.get_pod_ip_address(
            name=self.SFTP_POD_NAME, namespace=namespace)

        return sftp_ipaddress

    def generate_ssh_keys(self, filename, pod, namespace,
                          algorithm="rsa", bits="2048",
                          removefiles=False):
        """
        Generates public and private keys.
        Returns the public key and private key in base64 format
        and in raw form
        :param filename: name of the file to store the keys
        :param pod: name of the pod
        :param namespace: namespace name
        :param algorithm: algorithm for generation
        :param bits: bits for generation
        :param removefiles: remove files if they already exist
        """
        if removefiles is True:
            command = ["sh", "-c",
                       f"rm -f {filename} {filename}.pub base64{filename} "
                       f"base64{filename}.pub"]
            self.KUBE.exec_cmd_on_pod(pod, namespace, command)
        command = ["sh", "-c",
                   f"ssh-keygen -t {algorithm} -b {bits} -f {filename}"]
        self.KUBE.exec_cmd_on_pod(pod, namespace, command)
        command = [""]
        self.KUBE.exec_cmd_on_pod(pod, namespace, command)
        self.KUBE.exec_cmd_on_pod(pod, namespace, command)
        command = ["sh", "-c",
                   f"cat {filename}.pub | base64 -w 0 > base64{filename}.pub"]
        self.KUBE.exec_cmd_on_pod(pod, namespace, command)
        command = ["cat", f"{filename}.pub"]
        SFTP_PUBLIC_KEY = self.KUBE.exec_cmd_on_pod(pod, namespace, command)
        command = ["cat", f"{filename}"]
        SFTP_PRIVATE_KEY = self.KUBE.exec_cmd_on_pod(pod, namespace, command)
        command = ["cat", f"base64{filename}.pub"]
        BASE64_SFTP_PUBLIC_KEY = self.KUBE.exec_cmd_on_pod(
            pod, namespace, command)
        command = ["sh", "-c",
                   f"cat {filename} | base64 -w 0 > base64{filename}"]
        self.KUBE.exec_cmd_on_pod(pod, namespace, command)
        command = ["cat", f"base64{filename}"]
        BASE64_SFTP_PRIVATE_KEY = self.KUBE.exec_cmd_on_pod(
            pod, namespace, command)
        list_of_keys = [SFTP_PUBLIC_KEY, BASE64_SFTP_PUBLIC_KEY,
                        SFTP_PRIVATE_KEY, BASE64_SFTP_PRIVATE_KEY]

        return list_of_keys

    def add_user_to_sftp_server(self, pod_name, namespace, public_key):
        """
        Adds the user to the sftp server
        :param pod: name of the pod
        :param namespace: namespace name
        :param public_key: public key in raw form
        """
        command = ["sh", "-c", "mkdir -p /home/foo"]
        self.KUBE.exec_cmd_on_pod(pod_name, namespace, command)
        command = ["sh", "-c", "useradd foo --home /home/foo"]
        self.KUBE.exec_cmd_on_pod(pod_name, namespace, command)
        command = ["sh", "-c", "chown foo /home/foo"]
        self.KUBE.exec_cmd_on_pod(pod_name, namespace, command)
        command = ["mkdir", "/home/foo/.ssh"]
        self.KUBE.exec_cmd_on_pod(pod_name, namespace, command)
        command = ["touch", "/home/foo/.ssh/authorized_keys"]
        self.KUBE.exec_cmd_on_pod(pod_name, namespace, command)
        command = ["sh", "-c",
                   f"echo '{public_key}' > /home/foo/.ssh/authorized_keys"]
        self.KUBE.exec_cmd_on_pod(pod_name, namespace, command)

    def get_host_key_from_sftp_server(self, pod_name, namespace):
        """
        Gets the host key from the sftp server
        Returns the host key
        :param pod: name of the pod
        :param namespace: namespace name
        """
        command = ["sh", "-c", f"cat /etc/ssh/ssh_host_rsa_key.pub | base64 "
                   "-w 0 > /etc/ssh/base64_ssh_host_rsa_key.pub"]
        self.KUBE.exec_cmd_on_pod(pod_name, namespace, command)
        command = ["cat", "/etc/ssh/base64_ssh_host_rsa_key.pub"]
        host_key = self.KUBE.exec_cmd_on_pod(pod_name, namespace, command)

        return host_key
