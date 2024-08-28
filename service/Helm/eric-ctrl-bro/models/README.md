# Backup and Restore Orchestrator Life Cycle Management Fragment
The included interface description currently only targets internal network interfaces.
As such users should continue to monitor data interfaces and external interfaces for deprecations via JIRA until they are included here.

## Helm versions overview

The helm versions have been stepped due to the listed deprecations.
Note that the tracking of the helm version starts only at the 13.0.0 version as this is the current active version.
Ongoing deprecations have been shown here as minor version changes. Projected versions where the grace period for these end is also shown
Currently the OSMN is on a reduced timeframe ( < 13 Months ) due to the OSMN solution only supporting the option for a reduced time thus the need to align with there deprecation end date to ensure compatibility between the latest version of services.
While other items were provisioned for before this their end date and resulting NBC change to the version is planned for after the OSMN NBC version change.


The versions listed below cover the schema of the values.yaml delivered with the Backup and Restore Orchestrator Helm chart.
The schema of the values.yaml is versioned independently of the Backup and Restore Helm Chart.

01.0.0 - assumed starting point is the first BRO release so helm default configuration and parameters and BRO versions are matching
02.0.0 - move from bro 1.0 to 2.0 including sftp server removal
03.0.0 - enabling TLS by default
04.0.0 - removed the bro.backupLimit
05.0.0 - TLS param alignment
06.0.0 - TLS param alignment
07.0.0 - node selector moved location
08.0.0 - pull secret moved
09.0.0 - performance monitoring alignment
10.0.0 - probes alignment
11.0.0 - grace period changed
12.0.0 - ACL enabled by default for KVDB
13.0.0 - heap size changed due to SUSE patch (note all completed up to here, present day from here on)
13.1.0 - log streaming alignment deprecation introduced
13.2.0 - multiple containers added for SHH introduction update to the structure to facilitate this deprecation introduced
13.3.0 - removal of OSMN credentials parameters deprecation introduced as they are now auto generated. 
14.0.0 - removal of OSMN credentials NBC
14.1.0 - values.yaml now has fields for Semantic SW Version Check
15.0.0 - Updated K8S Security DR DR-D1123-134
15.1.0 - MBKF Operator
15.2.0 - KVDB RD Operator
15.3.0 - CMYP Server Secret Suffix Unhardcoding
15.4.0 - New port for non-TLS PM and new PM scraping pattern - DR-D470223-010
16.0.0 - Remove deprecated code for logging configuration design rules DR-470222-010/DR-470222-030

Planned future versions:
17.0.0 - multiple containers NBC

### BRO Agent API Java Library

The Java BRO Agent library is provided to allow for simplified and consistent behavior of the GRPC interfaces that BRO provides for backup and restore. 

There have been several version of the API catered for by this interface over time.

-V1 original specification, did not cater for individual steps (deprecated on approval of V2)
-V2 updated to allow for individual steps
-V3 updated to resolve an issue where all fragments were sent in a single message resulting in the potential to exceed the message size.
-V4 updated to enable direct streaming and feature support declaration

BRO Java library implementation

N/A - 1.0.0 - non 2PP and unsupported                                                     - BRO GRPC CTRL/DATA API 1.0.0 based implementation
N/A - 2.0.0 - non 2PP and unsupported                                                     - BRO GRPC CTRL/DATA API 2.0.0 based implementation
EOS - 3.0.0 - 3pp as of 3.0.4                                                             - BRO GRPC CTRL/DATA API 3.0.0 based implementation
EOM - 4.0.0 - moved from a Java 8 based build to a Java 11 based build up to 4.0.8 is EOM - BRO GRPC CTRL/DATA API 3.0.0 based implementation
    - 5.0.0 - introduction of the V4 API                                                  - BRO GRPC CTRL/DATA API 4.0.0 based implementation

Up to the 5.0.0 Java Agent implementation of the BRO Agent API required a BRO which supported a greater or equal version of the API.
As of the 5.0.0 Java Agent implementation of the BRO Agent API upon rejection by the BRO the implementation will fallback to the previous version allowing for backwards compatibility of the agent towards older BRO versions. e.g rejection of registration for a version 4 API by BRO as version 4 is unsupported the agent will fall back to attempting to register as a v3 agent. Note that this is an explicit error returned by BRO in this case and handled by the agent.