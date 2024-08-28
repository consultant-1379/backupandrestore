# Performance Testing of BRO

The scripts in this directory were created to make the process of running performance tests easier. This document describes how to use these scripts.
To get an overview of the performance testing process, or for information on how to collect the metrics after running these scripts, please see here:

* https://confluence-oss.seli.wh.rnd.internal.ericsson.com/display/ADP/Process+for+performance+tests


There are 2 main files to know about before running any tests. These are:

* full_perf_test.py - Runs the performance tests. It:
    * Installs the necessary agents
    * Performs a backup
    * Performs a restore
    * Deletes the backup
    * Deletes the agents
* config.py - Used to configure the performance tests

# Configuring the Performance Tests

To configure the performance tests, some values must be set in config.py

## Mandatory Config Values
There are 3 mandatory values that must be set. They are:

* NAMESPACE
* AGENT_RELEASE_NAME
* AGENT_INSTALL_CMD

### NAMESPACE

NAMESPACE is the namespace that the tests will run in.

### AGENT_RELEASE_NAME

AGENT_RELEASE_NAME is the name that will be given to the agent helm releases. The performance tests will potentially be installing many agents.
Of course, the agents can't all have the same release name. To cope with this, the test scripts will do some formatting on AGENT_RELEASE_NAME.
Inside of the scripts, AGENT_RELEASE_NAME will be formatted as follows:

* `str.format(agent_number)`

`agent_number` will be an integer. The value of AGENT_RELEASE_NAME should contain `{0}` in it. For example:

* `'performance_test_agent_{0}'`

This will allow the scripts to insert the agent number into the value provided.
If 3 agents must be installed as part of the tests and the example value above is used, the agent release names will be:

* performance_test_agent_0
* performance_test_agent_1
* performance_test_agent_2

### AGENT_INSTALL_CMD

This is the helm command that will be used to install the agents. The performance tests will potentially be installing many agents.
These agents will have different release names and will potentially have different backup sizes.
Therefore, the same `helm install` command won't work for all agents. To cope with this, the test scripts will do some formatting on AGENT_INSTALL_CMD.
Inside of the scripts, AGENT_INSTALL_CMD will be formatted as follows:

* `str.format(release_name, backup_size)`

`release_name` is the agent's helm release name. This will be constructed using the value that was provided for AGENT_RELEASE_NAME.
`backup_size` is the size of the agent's backup in megabytes.

The value of AGENT_INSTALL_CMD should contain `{0}` wherever the release name should be inserted.
It should contain `{1}` wherever the backup size should be inserted. Note that `{0}` and `{1}` can be used as many times as required in AGENT_INSTALL_CMD.

An example of AGENT_INSTALL_CMD can be seen below:

`'helm install myrepo/eric-test-service-basic --version 0.0.1-10 --name {0} --set nameOverride={0} --set brAgent.properties.applicationProperties.large\.backup\.file\.agent\.backup\.size={1} --set brAgent.properties.applicationProperties.test\.agent\.agentBehavior=com.ericsson.adp.mgmt.brotestagent.agent.behavior.LargeBackupFileAgentBehavior --set brAgent.brLabelValue={0}'`

## Optional Config Values

There is also the option to set the value of _LOG_LOCATION in config.py.
_LOG_LOCATION is used to set the location on the file system where the logs for the performance tests should be stored.
It specifies a directory. All log files will be created inside of that directory.
If _LOG_LOCATION is not set, then `./performance_logs` will be used.

# Running the Performance Tests

Before running the tests, there are 2 things that must be done:

* Configure the tests using the instructions in the "Configuring the Performance Tests" section
* Install the Orchestrator. The install_orch.py script may be used to do this.

To run the performance tests, use the full_perf_test.py script. This file is run as follows:

* `full_perf_test.py <backup_name> <number_of_agents> <backup_size_of_agent_0> [<backup_size_of_agent_1> <backup_size_of_agent_2> ....]`

Let's take the following example:

* `full_perf_test.py my_backup 3 1024 512 51200`

In this example, full_perf_test.py will install 3 agents. The first agent's backup size will be 1024 megabytes.
The second agent's backup size will be 512 megabytes. The third agent's backup size will be 51200 megabytes.
It will perform a backup with the name `my_backup`. Once the backup is complete, it will do a restore of that backup.
It will then delete the backup to free up space on the orchestrator. Finally, it will delete all of the agents which it installed.

The example above would also create 3 log files:

* output.txt - This is the output of the tests. This is the same output as it would be found in the console.
* backup.txt - This contains some details about the backup, including the backup name, start time, finish time and duration.
* restore.txt - This contains some details about the restore, including the backup name, start time, finish time and duration.

These files will be stored in `<_LOG_LOCATION>/<backup_name>`.
If the backup or the restore fails, it will also record the logs of the Orchestrator and all of the Agent pods.
These logs will also be stored in `<_LOG_LOCATION>/<backup_name>`.

# Additional Scripts

full_perf_test.py is the main script for running the performance tests. It uses many other files such as backup_perf_test.py, restore_perf_test.py, install_agents.py, etc.
This allows it to do the full set of actions required. However, there may be situations where it is not required to run everything.
For example, the only thing required might be:

* To do test for restoring an already existing backup
* Install a set of agents
* Delete a set of agents

Many of the files that full_perf_test.py uses can actually be run as standalone scripts.
To find out if a file can be used as a standalone script, run the script with the --help option. It will give information about how to use the script.

## Installing the Orchestrator with install_orchestrator.py

There is a script called install_orchestrator.py. This script can be used to install the Orchestrator. It exists purely for convenience.
It is not mandatory to use it, and it is not used by any of the performance testing scripts.

To use install_orchestrator.py, some values must be set in config.py:

* ORCH_RELEASE_NAME
* ORCH_INSTALL_CMD

### ORCH_RELEASE_NAME

ORCH_RELEASE_NAME is the name that will be given to the orchestrator helm release.

### ORCH_INSTALL_CMD

This is the helm command that will be used to install the orchestrator.
Similar to AGENT_INSTALL_CMD, it will be formated by install_orch.py. It will be formatted as follows:

* `str.format(release_name, pvc_size)`

`release_name` is the Orchestrator's helm release name. `pvc_size` is the size of the Orchestrator's PVC.
Note that if the storage class is NFS, the value set for PVC size will not be respected.

An example of ORCH_INSTALL_CMD can be seen below:

`'helm install myrepo/eric-ctrl-bro --version 0.0.1-518 --name {0} --set persistence.persistentVolumeClaim.size="{1}Gi"'`
