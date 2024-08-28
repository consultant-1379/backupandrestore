# lazy-fossa instructions

These are the instructions for running the lazy-fossa script

## Prerequisites

* WSL or Linux VM
* FOSSA is installed
* jq is installed
* yq is installed
* docker server started

### How to install FOSSA

1. cd /usr/local/bin/
2. wget https://github.com/fossas/fossa-cli/releases/download/v1.1.0/fossa-cli_1.1.0_linux_amd64.tar.gz | tar xzvf
3. cd ~
4. fossa --version

### How to install jq

#### Debian based systems eg Ubuntu

1. sudo apt-get install jq
2. jq --version

#### Manual install

1. cd ~/local/bin
2. wget https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64
3. mv jq-linux64 jq
4. cd ~
5. jq --version

### How to install yq

1. cd /usr/local/bin/
2. wget https://github.com/mikefarah/yq/releases/download/3.4.0/yq_linux_amd64
3. mv yq_linux_amd64 yq
4. cd ~
5. yq --version

## Create a configuration file

A yml configuration file needs to be created for each project that lazy-fossa needs to be run on. The project directory is expected to be a sub-directory of the present working directory. "." can be used as the directory name if it is required to run FOSSA in the present working directory.

The format is:
```
lazy-fossa:
  directory: [PROJECT_DIR]
  project: [PROJECT_NAME]
  team: [TEAM_NAME]
  primary-list: [PRIMARY_FILE_LOCATION]
  version: [TARGET_RELEASE]
  product-structure: [PRODUCT_STRUCTURE_FILE_LOCATION]
```
Example:
```
lazy-fossa:
  directory: "service"
  project: "My Microservice"
  team: "The-I-Team"
  primary-list: "fossa/parents.txt"
  version: 1.0.0
  product-structure: "fossa/product_structure.yaml"
```

## Running lazy-fossa

The lazy fossa script performs several functions in order to provide two resources needed for release.
1. The license agreement file
2. The dependencies.yaml file

The license agreement file indicates all licenses that are in play in the service and is needed to allow Ericsson to fulfill its obligations to the license holders.
The dependencies.yaml contains a full Bill Of Materials (BOM) and is used to allow for the identification and traceability of software in Ericsson products.

These files must be created in a specific order along with a few others in order to provide the required output.

The first step is to run a FOSSA scan this will scan the repo and identify all software used and store its results in a report file.
This report file is then used to create the dependencies.yaml

```
export FOSSA_API_KEY="<Your FOSSA API Key>"
./lazy-fossa -f [CONFIG_FILE_NAME]
```
Note: This command will generate a new dependencies.yaml every time it is executed.


Once the basic dependencies file has been created there are a number of fields left in a default state which need to be populated.
The key fields required are:
1. The Version
2. The Selected License
3. The PRIM assigned number
4. If the dependency is a primary

To populate the PRIM number and version field a bazaar scan is run this will pull in the necessary info from bazaar.
In the event the bazaar scan fails to find a 3PP a manual search of Bazaar will be required.
If this also does not identify the 3PP needed a generic FOSS request will be needed to have it added before proceeding.

```
export BAZAAR_USER="<Your signame>"
export BAZAAR_TOKEN="<Your bazaar token>"
./lazy-fossa -b [CONFIG_FILE_NAME]
```
Note:
1.some of the libraries can't be fetched from scas for some reason. You may find some libraries' prim number and src_download_link are empty.
2.You may need to manually add the correct prim number and src_download_link when you find they are empty, otherwise the munin scan will fail.

To populate the license number field it is necessary to kick off the license file generation during the generation of this file the user will be asked to select a license where appropriate.

```
./lazy-fossa -l [CONFIG_FILE_NAME]
```
Note: Some Licenses are skipped. It may be required to manually edit or append Documentation/license.agreement.json still.

Finally it is necessary to assess the status of the BOM in munin, a munin search can be run to check if the PRIM numbers have been registered for use in products.
The command will generate a report file any entries that have been identified will need to be added to munin.
Contact the 3PP team with the list reported to get them added for use.

```
export MUNIN_TOKEN="<Your munin token>"
./lazy-fossa -m [CONFIG_FILE_NAME]
```
Note: Make sure all the libraries pass the munin scan when uplifting a library.


It is also possible to run all of these steps together

```
./lazy-fossa -a [CONFIG_FILE_NAME]
```

Additional manual actions may be required at any stage. In the event manual intervention is needed it is better to run from that step on rather then all together.
If a previous step is run it may regenerate or overwrite input given by the user to fix issues.

Further actions that can be performed:
Anchore scan perform a local scan of a given image using Anchore.
```
docker pull "<IMAGE_NAME>"
export IMAGE_TO_SCAN="<IMAGE_NAME>"
./lazy-fossa -s [CONFIG_FILE_NAME]
```

Product version creation in munin, should not be run unless absolutely required. really only here for trouble shooting, required mimer data writer permission.
```
./lazy-fossa -c [CONFIG_FILE_NAME]
```
