The objective of this procedure is to generate a global view of the status of the last 50 builds executed in a pipeline 
that generates plots and are presented in jenkins
You could analyze one of the following pipelines:
    * BRO_Performance
    * BRO_Performance_minimum_resource
    * BRO_Performance_OSMN
    * BRO_Performance_parallel_actions
    * Large_Data_Transfer_Verification

The process includes the use of a script that does the following:
Retrieve information from the artifact "updated_adp_char_report.json" 
of each of the last 50 builds executed

It extracts information like:
** use case
** Duration
** avg/max memory
** avg/max cpu

** Generates a CSV file (The separator is a "|")


The script requires the username and password with privileges to observe the status of each pipeline in Jenkins, so 
this can be entered on the command line or the script will request this information if it is not entered.
You could analyze one of the following pipelines:
BRO_Performance
BRO_Performance_minimum_resource
BRO_Performance_OSMN
BRO_Performance_parallel_actions
Large_Data_Transfer_Verification

broperformance.sh -h
Usage: /c/lislas/scripts/broperformance.sh [-h] [-u <user>] [-p <password>] [-s <service Name>]
    collect performance data from 
https://fem41s11-eiffel004.eiffel.gic.ericsson.se:8443/jenkins/job/NightlyJobs/job/Performance/
    -u username
    -p password
    -s pipeline name

we can execute the script as follows ie:

broperformance.sh -u eisljos -p MyPassword | grep -v "failed" | grep -v "Invalid" | awk 'NF' 2> /dev/null > broperformance_osmn.txt

This command will generate a broperformance_osmn.txt file.

