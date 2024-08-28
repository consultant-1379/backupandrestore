"""
This script requires python3.9+ and was run with the following
libraries installed at the time of writing.

oyaml==1.0
PyYAML==3.11

"""

import oyaml as yaml
import math

print("________________________________________________")
print("1. BRO \n2. BRO API \n3. Log4j Appender ")
print("________________________________________________")

repo = input('\nEnter the number for the repo you would like to update: ')
repo_name = ""

if repo == "1":
    repo_name = "service"
elif repo == "2":
    repo_name = "bro-agent-api"
elif repo == "3":
    repo_name = "eric-log4j2-socket-appender"

# Read in all dependencies from currentdeptree.txt and create a list
f = open("./{}/currentdeptree.txt".format(repo_name))
contents = f.read()
lines = contents.splitlines()

# Generate Primary Dependency List

# Grab all the dependencies starting with "+-" and split it at the second occurance of ":"
primary_list = sorted([line[3:].split(":", 2) for line in lines if line[:2] == "+-"])

# This will leave us without +{version} so we can use it to compare with what is in the dependencies.yaml
updated_list = [line[0] + ":" +  line[1] for line in primary_list]

# Convert dependencies into dict
with open('./{}/fossa/dependencies.yaml'.format(repo_name)) as dependencies:
    dataMap = yaml.safe_load(dependencies)

already_primary = []
print("\n_______________Selected Primaries_______________\n")
for dependency in dataMap["dependencies"]:
    name = dependency['ID'].split("+")[0]
    if name in updated_list and name.split(":")[0] not in already_primary:
        # If the dependencies in './fossa/dependencies.yaml' is in the primary list created from currentdeptree.txt
        # then we will mark it as it as the primary and add it to already_primary=[] and use it as a check
        # and mark the rest of the libaries with the same primary as false
        already_primary.append(name.split(":")[0])
        print("Dependency: {} \n  Stako: {} \n  Stako Comment:{}".format(name, dependency['bazaar']['stako'], dependency['bazaar']['stako_comment']))
        dependency['primary'] = ['this']
        dependency['mimer']['primary'] = 'True'
    else:
        dependency['primary'] = ['false']
        dependency['mimer']['primary'] = 'False'
print("________________________________________________")

# By default yaml will not indent when writing the file
# This is a custom class which will indent the file to the correct level
class IndentDumper(yaml.Dumper):
    def increase_indent(self, flow=False, indentless=False):
        return super(IndentDumper, self).increase_indent(flow, False)

with open('./{}/fossa/dependencies.yaml'.format(repo_name), 'w') as outfile:
    yaml.dump(dataMap, outfile, default_style=None, default_flow_style=False,
              Dumper=IndentDumper, width=math.inf, allow_unicode=True)
