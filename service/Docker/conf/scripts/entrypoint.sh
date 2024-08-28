#!/bin/bash
umask 007
export KUBERNETES_SERVICE_HOST=$(java -jar /opt/ericsson/br/eric-ctrl-bro.jar $KUBERNETES_SERVICE_HOST)
# RUNBRO is an environment variable. "true" will run BRO; "false" will pause BRO
if [ "$RUNBRO" = true ]
then
    httpprobe &
    exec java -XX:InitialRAMPercentage=$INITIAL_RAM_PERCENTAGE -XX:MinRAMPercentage=$MIN_RAM_PERCENTAGE  -XX:MaxRAMPercentage=$MAX_RAM_PERCENTAGE -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager -Dlog4j.configurationFile=log4j2.xml -Djdk.tls.client.protocols=TLSv1.2,TLSv1.3 -Dspring.config.location=/opt/ericsson/br/application.properties -Dorg.owasp.esapi.logSpecial.discard=true -Djava.io.tmpdir=/temp -jar /opt/ericsson/br/eric-ctrl-bro.jar
else
    echo "{\"timestamp\":\"`date +"%Y-%m-%dT%H:%M:%S.%3N%:z"`\",\"severity\":\"info\",\"service_id\":\"$SERVICEID\",\"message\":\"BRO will not start until you update the startup configmap\"}"
    tail -f /dev/null
fi
