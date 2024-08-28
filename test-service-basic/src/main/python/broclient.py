#!/usr/bin/env python3
import sys
import os

from jproperties import Properties

import time

import grpc
import logging
import argparse

from bro_agent.agent.AgentBehavior import AgentBehavior
from bro_agent.agent.AgentFactory import AgentFactory
from bro_agent.agent.OrchestratorConnectionInformation import OrchestratorConnectionInformation
from bro_agent.fragment.BackupFragmentInformation import BackupFragmentInformation
import bro_agent.generated.Fragment_pb2 as Fragment
from bro_agent.registration.RegistrationInformation import RegistrationInformation
from bro_agent.registration.SoftwareVersion import SoftwareVersion

logging.basicConfig(level=logging.INFO, format='%(asctime)s [%(levelname)s]: %(message)s')

import signal

class SignalHandlerClass:
    def __init__(self):
        self.terminated = False
        signal.signal(signal.SIGTERM, self.signalHandler)

    def signalHandler(self, signum, frame):
        self.terminated = True

class AgentDiesDuringBackupTestAgentBehavior(AgentBehavior):
    def __init__(self, properties):
        self.properties=properties
        pass

    def get_registration_information(self):
        softwareVersion = SoftwareVersion(productName=self.properties["test.agent.softwareVersion.productName"],
                                          productNumber=self.properties["test.agent.softwareVersion.productNumber"],
                                          revision=self.properties["test.agent.softwareVersion.revision"],
                                          productionDate=self.properties["test.agent.softwareVersion.productionDate"],
                                          description=self.properties["test.agent.softwareVersion.description"],
                                          type=self.properties["test.agent.softwareVersion.type"])
        self.registrationInformation = RegistrationInformation(self.properties["test.agent.id"], self.properties["test.agent.scope"], softwareVersion)
        return self.registrationInformation

    def execute_backup(self, backup_execution_actions):
        raise "Simulating dying agent"

class AgentDiesDuringRestoreTestAgentBehavior(AgentBehavior):
    def __init__(self, properties):
        self.properties=properties
        pass

    def get_registration_information(self):
        softwareVersion = SoftwareVersion(productName=self.properties["test.agent.softwareVersion.productName"],
                                          productNumber=self.properties["test.agent.softwareVersion.productNumber"],
                                          revision=self.properties["test.agent.softwareVersion.revision"],
                                          productionDate=self.properties["test.agent.softwareVersion.productionDate"],
                                          description=self.properties["test.agent.softwareVersion.description"],
                                          type=self.properties["test.agent.softwareVersion.type"])
        self.registrationInformation = RegistrationInformation(self.properties["test.agent.id"], self.properties["test.agent.scope"], softwareVersion)
        return self.registrationInformation

    def execute_backup(self, backup_execution_actions):
        paths=self.properties["test.agent.fragment.backup.data.path"].split(",")
        customMetadataPaths=self.properties["test.agent.fragment.custom.backup.data.path"].split(",")
        i=1
        for path,customPath in zip(paths,customMetadataPaths):
            fragmentInformation = BackupFragmentInformation(f'{self.properties["test.agent.id"]}_{i}', "0.1", os.stat(path).st_size, None, path, customPath if customPath!='' else None)
            i=i+1
            backup_execution_actions.send_backup(fragmentInformation)

        backup_execution_actions.backup_complete(True, "Successfully backed up content!")

    def execute_restore(self, backup_execution_actions):
        raise "Simulating dying agent"


class FailingRestoreTestAgentBehavior(AgentBehavior):
    def __init__(self, properties):
        self.properties=properties
        pass

    def get_registration_information(self):
        softwareVersion = SoftwareVersion(productName=self.properties["test.agent.softwareVersion.productName"],
                                          productNumber=self.properties["test.agent.softwareVersion.productNumber"],
                                          revision=self.properties["test.agent.softwareVersion.revision"],
                                          productionDate=self.properties["test.agent.softwareVersion.productionDate"],
                                          description=self.properties["test.agent.softwareVersion.description"],
                                          type=self.properties["test.agent.softwareVersion.type"])
        self.registrationInformation = RegistrationInformation(self.properties["test.agent.id"], self.properties["test.agent.scope"], softwareVersion)
        return self.registrationInformation

    def execute_backup(self, backup_execution_actions):
        paths=self.properties["test.agent.fragment.backup.data.path"].split(",")
        customMetadataPaths=self.properties["test.agent.fragment.custom.backup.data.path"].split(",")
        i=1
        for path,customPath in zip(paths,customMetadataPaths):
            fragmentInformation = BackupFragmentInformation(f'{self.properties["test.agent.id"]}_{i}', "0.1", os.stat(path).st_size, None, path, customPath if customPath!='' else None)
            i=i+1
            backup_execution_actions.send_backup(fragmentInformation)

        backup_execution_actions.backup_complete(True, "Successfully backed up content!")

    def execute_restore(self, backup_execution_actions):
        restore_execution_actions.restore_complete(False, "Simulating failing agent")


class NormalAgentBehavior(AgentBehavior):
    def __init__(self, properties):
        self.properties=properties

    def get_registration_information(self):
        softwareVersion = SoftwareVersion(productName=self.properties["test.agent.softwareVersion.productName"],
                                          productNumber=self.properties["test.agent.softwareVersion.productNumber"],
                                          revision=self.properties["test.agent.softwareVersion.revision"],
                                          productionDate=self.properties["test.agent.softwareVersion.productionDate"],
                                          description=self.properties["test.agent.softwareVersion.description"],
                                          type=self.properties["test.agent.softwareVersion.type"])
        self.registrationInformation = RegistrationInformation(self.properties["test.agent.id"], self.properties["test.agent.scope"], softwareVersion)
        return self.registrationInformation

    def execute_backup(self, backup_execution_actions):
        paths=self.properties["test.agent.fragment.backup.data.path"].split(",")
        customMetadataPaths=self.properties["test.agent.fragment.custom.backup.data.path"].split(",")
        i=1
        for path,customPath in zip(paths,customMetadataPaths):
            fragmentInformation = BackupFragmentInformation(f'{self.properties["test.agent.id"]}_{i}', "0.1", os.stat(path).st_size, None, path, customPath if customPath!='' else None)
            i=i+1
            backup_execution_actions.send_backup(fragmentInformation)

        backup_execution_actions.backup_complete(True, "Successfully backed up content!")

    def execute_restore(self, restore_execution_actions):
        for fragment in restore_execution_actions.restore_information.fragment:
            restore_execution_actions.download_fragment(fragment, self.properties["test.agent.download.location"])
        restore_execution_actions.restore_complete(True, "Successfully restored content!")

    def on_error(self):
        logging.error("Agent depleted max number of attempts to re-register to BRO!")
        os.exit(1)

Behaviors={"AgentDiesDuringBackupTestAgentBehavior":AgentDiesDuringBackupTestAgentBehavior,
           "AgentDiesDuringRestoreTestAgentBehavior":AgentDiesDuringRestoreTestAgentBehavior,
           "FailingRestoreTestAgentBehavior":FailingRestoreTestAgentBehavior,
           "NormalAgentBehavior":NormalAgentBehavior}

def selectBehavior(behaviorJavaClassName):
    if behaviorJavaClassName=="":
        behaviorClassName="NormalAgentBehavior"
    else:
        behaviorClassName=behaviorJavaClassName.split(".")[-1]
    logging.info(f"Using behaviour: {behaviorClassName}")
    return Behaviors[behaviorClassName]


def mymain(properties):
    signalHandler = SignalHandlerClass()
    logging.info("Test python agent for backupandrestore service!")
    behavior=selectBehavior(properties["test.agent.agentBehavior"])(properties)
    if properties["test.agent.download.location"]!='':
        if os.path.isfile(properties["test.agent.download.location"]):
            os.remove(properties["test.agent.download.location"])
        if not os.path.exists(properties["test.agent.download.location"]):
            os.mkdir(properties["test.agent.download.location"])
    connectionInfo = OrchestratorConnectionInformation(properties["orchestrator.host"], properties["orchestrator.port"])
    agentFactory = AgentFactory("NoCred")
    agent = agentFactory.create_agent(connectionInfo, behavior)
    while not signalHandler.terminated:
        time.sleep(1)
    agent.shutdown()

defaultProperties={
    "orchestrator.host": "",
    "orchestrator.port": "",
    "test.agent.id": "",
    "test.agent.scope": "",
    "test.agent.softwareVersion.description": "",
    "test.agent.softwareVersion.productionDate": "",
    "test.agent.softwareVersion.productName": "",
    "test.agent.softwareVersion.productNumber": "",
    "test.agent.softwareVersion.type": "",
    "test.agent.softwareVersion.revision": "",
    "test.agent.fragment.backup.data.path": "",
    "test.agent.fragment.custom.backup.data.path":"",
    "test.agent.download.location":"",
    "test.agent.agentBehavior":""
}

parser = argparse.ArgumentParser()
parser.add_argument("applicationProperties")
args = parser.parse_args()

p = Properties()
p.properties=defaultProperties
with open(args.applicationProperties, "rb") as f:
    p.load(f, "utf-8")
try:
    mymain(p.properties)
except Exception as e:
    logging.error("Exception in mymain")
    logging.error(e)
finally:
    logging.info("Finally exiting")
