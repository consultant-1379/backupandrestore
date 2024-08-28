#!/bin/bash

DOCUMENT_FILE="service/Documentation/documents.yaml"
MARKETPLACE_CONFIG_FILE="ci_config/marketplace/marketplace_release_upload_config.yaml"

# Define the mapping between variables and document titles
declare -A link_mapping=(
  ["PRI_DOC_LINK"]="PRI"
  ["RA_DOC_LINK"]="RA and PIA Report Backup and Restore Orchestrator"
  ["SECURE_CODING_REPORT_DOC_LINK"]="Secure Coding Report for Backup and Restore Orchestrator"
  ["BRO_VA_DOC_LINK"]="VA Report for Backup and Restore Orchestrator"
  ["TEST_SPECIFICATION_DOC_LINK"]="Backup and Restore Orchestrator Test Specification"
  ["TEST_REPORT_DOC_LINK"]="Backup and Restore Orchestrator Test Report"
)

# Iterate over the link_mapping array and update the external links
for var in "${!link_mapping[@]}"; do
  title="${link_mapping[$var]}"
  echo $title
  url=$(grep -A 5 "$title" $DOCUMENT_FILE | grep url | awk '{print $NF}')
  echo $url
  echo $var
  if grep -q "external-link: $var" $MARKETPLACE_CONFIG_FILE; then escaped_url=$(sed 's/[\/&]/\\&/g' <<< "$url"); sed -i 's#'"$var"'#'"$escaped_url"'#' $MARKETPLACE_CONFIG_FILE; fi
done
