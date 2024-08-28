#!/usr/bin/env python3
"""
This module integrates methods to verify backup and restore operations when
 BRO is deployed with OSMN.
"""
import os
import base64
import boto3
import botocore
import helm3procs
import utilprocs
import bro_utils

OSMN_URL_REPO = "https://arm.sero.gic.ericsson.se/artifactory/" \
                "proj-adp-eric-data-object-storage-mn-released-helm/"
OSMN_REPO_NAME = "osmn-chart-repo"
OSMN_SECRETS_NAME = 'eric-data-object-storage-mn-secret'
OSMN_RELEASE_NAME = 'osmn-test-server'
OSMN_CHART_NAME = 'eric-data-object-storage-mn'
NAMESPACE = os.environ.get('kubernetes_namespace')

# pylint: disable=too-few-public-methods


class _SingletonWrapper:
    """
    A singleton wrapper class
    """

    def __init__(self, cls):
        self.__wrapped__ = cls
        self._instance = None

    def __call__(self, *args, **kwargs):
        """
        Returns a single instance of decorated class
        """
        if self._instance is None:
            self._instance = self.__wrapped__(*args, **kwargs)
        return self._instance


def singleton(cls):
    """
    A singleton decorator
    """
    return _SingletonWrapper(cls)


@singleton
class BroS3():
    """
    This class is used for access to OSMN Client and Server
    """

    def __init__(self, endpoint="eric-data-object-storage-mn", port=9000):
        self.endpoint = endpoint
        self.region_name = "us-west-2"
        self.port = port
        self.client = None

    def start_client(self):
        """
        start osmn client
        """
        endpoint_url = ("http://{}:{}".format(self.endpoint, self.port))
        osmn_secret = bro_utils.get_secret_data(NAMESPACE, OSMN_SECRETS_NAME)
        access_key = osmn_secret["accesskey"]
        secret_key = osmn_secret["secretkey"]
        self.client = boto3.resource('s3',
                                     endpoint_url=endpoint_url,
                                     aws_access_key_id=access_key,
                                     aws_secret_access_key=secret_key,
                                     region_name=self.region_name)

    def stop_server(self):
        """
        Uninstall the OSMN Server
        """
        utilprocs.log("Stopping OSMN server " + self.endpoint)
        return helm3procs.helm_delete_release(OSMN_RELEASE_NAME, NAMESPACE)

    def start_server(self, osmn_mode="standalone", enable_global_tls="false"):
        """
        start osmn server
        """
        utilprocs.log("Starting OSMN server " + self.endpoint)
        delete_repo = "helm repo remove {0} ".format(OSMN_REPO_NAME)
        try:
            utilprocs.execute_command(delete_repo)
        except Exception:
            pass
        helm3procs.add_helm_repo(OSMN_URL_REPO, OSMN_REPO_NAME)
        helm_settings = {"mode": osmn_mode,
                         "global.pullSecret": "armdocker",
                         "autoEncryption.enabled": "false",
                         "persistence.persistentVolumeClaim.size": "25Gi",
                         "global.security.tls.enabled": enable_global_tls
                         }
        osmn_chart_version = \
            helm3procs.get_latest_chart_version(OSMN_CHART_NAME,
                                                helm_repo_name=OSMN_REPO_NAME,
                                                development_version=False)
        helm3procs.helm_install_chart_from_repo_with_dict(
            release_name=OSMN_RELEASE_NAME,
            helm_repo_name=OSMN_REPO_NAME,
            chart_name=OSMN_CHART_NAME,
            target_namespace_name=NAMESPACE,
            chart_version=osmn_chart_version,
            settings_dict=helm_settings,
            should_wait=True)

    def get_bucket_file_list(self, path=None, bucket_name="bro"):
        """
        get the file content from OSMN
        :param filename: Filename to get the content
        :param backupmanager: Backup manager to validate
        :param backupname: Backupname to validate
        :param bucket_name: bucket name to validate, default "bro"

        return List object content
        """
        bucket_list = []
        if path is None:
            return bucket_list
        bucket_content = self.client.Bucket(bucket_name)
        for my_bucket_object in bucket_content.objects.filter(Prefix=path):
            bucket_list.append(os.path.basename(my_bucket_object.key))
        return bucket_list

    def get_file_content(self, path, bucket_name="bro"):
        """
        Retrieve the file content from OSMN
        It will be used to calculate the checksum in memory
        checksum is not implemented yet
        :param filename: Filename to get the content
        :param backupmanager: Backup manager to validate
        :param backupname: Backupname to validate
        :param bucket_name: bucket name to validate, default "bro"

        return file content
        """
        try:
            bucket_content = self.client.Bucket(bucket_name)
            for my_bucket_object in bucket_content.objects.filter(Prefix=path):
                body = my_bucket_object.get()['Body'].read()
            return body
        except botocore.exceptions.ClientError as exception:
            if exception.response['Error']['Code'] == "404":
                return None
            raise exception

    def backup_exist(self, backupmanager=None, backupname=None,
                     bucket_name="bro"):
        """
        Validates if a backup exists in S3
        :param backupmanager: Backup manager to validate
        :param backupname: Backupname to validate
        :param bucket_name: bucket name to validate, default "bro"

        :return: true if it exists otherwise false
        """
        backup = (
            "/{}/backups/{}/{}/brIntStorage.json"
            .format(bucket_name, backupmanager, backupname))
        try:
            self.client.Object(bucket_name, backup).load()
            return True
        except botocore.exceptions.ClientError as exception:
            if exception.response['Error']['Code'] == "404":
                return False
            raise exception

    def get_settings(self):
        """
        return the current settings used to start OSMN
        """
        properties = {
            "endpoint": self.endpoint,
            "region_name": self.region_name,
            "port": self.port
        }
        return properties


if __name__ == '__main__':
    BRO_1 = BroS3("127.0.0.1")
    BRO_2 = BroS3("127.0.0.1")
    assert BRO_1 is BRO_2
    BRO_1.start_client()
    assert BRO_2.backup_exist(backupmanager="configuration-data",
                              backupname="buyp1") is False
    FILES = BRO_2.get_bucket_file_list(
        path="bro/backups/DEFAULT/bu1/bravo/bravo_1/data/")
    CONTENT = BRO_2.get_file_content(FILES[0])
    BRO_1.stop_server()
