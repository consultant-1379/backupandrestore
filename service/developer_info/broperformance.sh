#!/bin/bash
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

pipelines=("BRO_Performance" "BRO_Performance_minimum_resource" "BRO_Performance_OSMN" "BRO_Performance_parallel_actions" "Large_Data_Transfer_Verification")
usage() {
   echo "Usage: $0 [-h] [-u <user>] [-p <password>] [-s <service Name>] "
   echo "   collect performance data from https://fem41s11-eiffel004.eiffel.gic.ericsson.se:8443/jenkins/job/NightlyJobs/job/Performance/"
   echo "   -u username"
   echo "   -p password"
   echo "   -s pipeline name"
   exit 1;
}

  while getopts ":u:p:s:h" o; do
     case "${o}" in
       u)
         USER=${OPTARG}
         ;;
       p)
         USERPASSWORD=${OPTARG}
         ;;
       s)
         SERVICENAME=${OPTARG}
        ;;
       h)
         usage
         exit -1
         ;;
       *)
         usage
         ;;
     esac
  done
if [ -z "${USER}" ]; then
   read -rp "Enter user name " USER
fi

if [ -z "${USERPASSWORD}" ]; then
   read -srp "Enter $USER password " USERPASSWORD
fi
if [ -z "${SERVICENAME}" ]; then
     echo ""
     select option in ${pipelines[@]}; do
       if [ 1 -le "$REPLY" ] && [ "$REPLY" -le ${#pipelines[@]} ];
       then
         SERVICENAME="$option"
         break
       else
         echo "Incorrect Input: Select a number 1-$#"
         exit -1
       fi
     done
fi
servicename="${SERVICENAME,,}"
case "$servicename" in
   "bro_performance")
      servicename="Performance/job/BRO_Performance"
      ;;
   "bro_performance_minimum_resource")
      servicename="Performance/job/BRO_Performance_minimum_resource"
      ;;
   "bro_performance_osmn")
      servicename="Performance/job/BRO_Performance_OSMN"
      ;;
   "bro_performance_parallel_actions")
      servicename="Performance/job/BRO_Performance_parallel_actions"
      ;;
   "large_data_transfer_verification")
      servicename="Large_Data_Transfer_Verification"
      ;;
   "*")
      echo "Select a right pipeline name"
      exit -1
esac
buildtotal=$( curl  --user $USER:$USERPASSWORD -s -X GET https://fem41s11-eiffel004.eiffel.gic.ericsson.se:8443/jenkins/view/Plan%20B/job/NightlyJobs/job/$servicename/ | sed -n 's/.*<a href="\([^"]*\)".*/\1/p' | grep -v changes | grep -v build | head -n 1 | grep -Eo '[0-9]+' | tail -1)


if [ -z "$buildtotal" ] || [[ ! $buildtotal =~ ^[0-9]+$ ]]; then
   read -rp "Enter the last build number: " buildtotal
   if [[ "$buildtotal" =~ ^[0-9]+$ ]]; then
       echo "Last build number: $buildtotal"
   else
       echo "Invalid build number."
       exit -1
   fi
fi

processArtifactJson () 
{
   buildNumber=$1

   jsonfile=$(curl --user $USER:$USERPASSWORD -s -X GET https://fem41s11-eiffel004.eiffel.gic.ericsson.se:8443/jenkins/job/NightlyJobs/job/$servicename/$buildNumber/artifact/updated_adp_char_report.json |  grep -v sha256 | jq -r '.["ADP-Microservice-Characteristics-Report"]["results"][] | "\(.["use-case"]) | \(.["duration"]) | \(.["metrics"][] | "\(.["pod"]) | \(.["container"]) | \(.["metrics"]["cpu_avg_milli_cores"]) | \(.["metrics"]["cpu_max_milli_cores"]) | \(.["metrics"]["memory_avg_mib"]) | \(.["metrics"]["memory_max_mib"]) | \(.["metrics"]["img_version"])")"' | grep -v hooklauncher)


if [ $? -ne 0 ]; then
    echo "${buildNumber} failed."
else
   arrayjf=()
   while IFS= read -r line; do
     arrayjf+=("$line")
   done <<< "$jsonfile"
   #IFS=$'\r\n' read -r -a lines <<< "$jsonfile"
   #for ((i=0; i<${#lines[@]}; i++)); do
   for ((i=0; i<${#arrayjf[@]}; i++)); do
    arrayline=${arrayjf[i]}
    IFS="|" read -ra arrayItems <<< "$arrayline"
    duration=${arrayItems[1]}
    trimmed_duration="${duration#"${duration%%[![:space:]]*}"}"
    trimmed_duration="${trimmed_duration%"${trimmed_duration##*[![:space:]]}"}"
    # Convert the trimmed value to a number
    numberDuration=$(echo "$trimmed_duration" | bc)
    result=$(echo "scale=2; $numberDuration/3" | bc)
    arrayItems[1]=$result
    IFS="|" joined_line="${arrayItems[*]}"
    #echo "${buildNumber}|${arrayjf[$i]} | ${SERVICENAME}"
    echo "${buildNumber}|${joined_line} | ${SERVICENAME}"
   done
fi
}

analyzecontent () 
{
   number=0
   while [ $number -lt 50 ]; do
     buildNumber=$((buildtotal - number))
     processArtifactJson $buildNumber
     ((number++)) 
   done
}

analyzecontent

