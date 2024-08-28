
{{/*
Create a map from global values with defaults if not in the values file.
*/}}
{{ define "eric-ctrl-bro.globalMap" }}
{{- $globalDefaults := dict "timezone" "UTC" -}}
{{- $globalDefaults := merge $globalDefaults (dict "security" (dict "tls" (dict "enabled" true))) -}}
{{- $globalDefaults := merge $globalDefaults (dict "security" (dict "tls" (dict "trustedInternalRootCa" (dict "secret" "eric-sec-sip-tls-trusted-root-cert")))) -}}
{{- $globalDefaults := merge $globalDefaults (dict "securityPolicy" (dict "rolekind" "")) -}}
{{- $globalDefaults := merge $globalDefaults (dict "security" (dict "policyBinding" (dict "create" false))) -}}
{{- $globalDefaults := merge $globalDefaults (dict "security" (dict "policyReferenceMap" (dict "default-restricted-security-policy" "default-restricted-security-policy"))) -}}
{{- $globalDefaults := merge $globalDefaults (dict "registry" (dict "imagePullPolicy" "IfNotPresent")) -}}
{{- $globalDefaults := merge $globalDefaults (dict "registry" (dict "repoPath")) -}}
{{- $globalDefaults := merge $globalDefaults (dict "hooklauncher" (dict "executor" "service")) -}}
{{- $globalDefaults := merge $globalDefaults (dict "log" (dict "streamingMethod" "")) -}}
{{ if .Values.global }}
{{- mergeOverwrite $globalDefaults .Values.global | toJson -}}
{{ else }}
{{- $globalDefaults | toJson -}}
{{ end }}
{{ end }}

{{/*
KVDB-RD Max Attempts value
*/}}
{{- define "eric-ctrl-bro.kvdbrd.retry.maxAttempts" -}}
{{- ternary .Values.keyValueDatabaseRD.retry.maxAttempts .Values.keyValueDatabaseRd.retry.maxAttempts ((((.Values.global).keyValueDatabaseRD).newOperator).enabled  | default false) -}}
{{- end -}}

{{/*
KVDB-RD Max Delay value
*/}}
{{- define "eric-ctrl-bro.kvdbrd.retry.maxDelay" -}}
{{- ternary .Values.keyValueDatabaseRD.retry.maxDelay .Values.keyValueDatabaseRd.retry.maxDelay ((((.Values.global).keyValueDatabaseRD).newOperator).enabled  | default false) -}}
{{- end -}}

{{/*
KVDB-RD enabled value
*/}}
{{- define "eric-ctrl-bro.kvdbrd.enabled" -}}
{{- ternary .Values.keyValueDatabaseRD.enabled .Values.keyValueDatabaseRd.enabled ((((.Values.global).keyValueDatabaseRD).newOperator).enabled  | default false) -}}
{{- end -}}

{{/*
KVDB-RD hostname value
*/}}
{{- define "eric-ctrl-bro.kvdbrd.hostname" -}}
{{- ternary ((.Values.keyValueDatabaseRD).clusterName | default "eric-cloud-native-kvdb-rd-operand") ((.Values.keyValueDatabaseRd).hostname | default "eric-data-key-value-database-rd-operand")  ((((.Values.global).keyValueDatabaseRD).newOperator).enabled  | default false) -}}
{{- end -}}

{{/*
KVDB-RD port value
*/}}
{{- define "eric-ctrl-bro.kvdbrd.port" -}}
{{- ternary .Values.keyValueDatabaseRD.port .Values.keyValueDatabaseRd.port ((((.Values.global).keyValueDatabaseRD).newOperator).enabled  | default false) -}}
{{- end -}}

{{/*
KVDB-RD tlsPort value
*/}}
{{- define "eric-ctrl-bro.kvdbrd.tlsPort" -}}
{{- ternary .Values.keyValueDatabaseRD.tlsPort .Values.keyValueDatabaseRd.tlsPort ((((.Values.global).keyValueDatabaseRD).newOperator).enabled  | default false) -}}
{{- end -}}

{{/*
KVDB-RD maxLen value
*/}}
{{- define "eric-ctrl-bro.kvdbrd.maxLen" -}}
{{- ternary .Values.keyValueDatabaseRD.maxLen .Values.keyValueDatabaseRd.maxLen ((((.Values.global).keyValueDatabaseRD).newOperator).enabled  | default false) -}}
{{- end -}}

{{/*
kvdbrd.acl.enabled value
*/}}
{{- define "eric-ctrl-bro.acl.enabled" -}}
{{- ternary true .Values.keyValueDatabaseRd.acl.enabled ((((.Values.global).keyValueDatabaseRD).newOperator).enabled  | default false) -}}
{{- end -}}

{{/*
KVDB-RD acl username value
*/}}
{{- define "eric-ctrl-bro.acl.username" -}}
{{- ternary .Values.keyValueDatabaseRD.acl.user .Values.keyValueDatabaseRd.acl.username ((((.Values.global).keyValueDatabaseRD).newOperator).enabled  | default false) -}}
{{- end -}}

{{/*
Create CA name
*/}}
{{- define "eric-ctrl-bro.kvdbrd.ca" -}}
{{- ternary (printf "%s-redis-client-ca" (include "eric-ctrl-bro.kvdbrd.hostname" .)) (printf "%s-client-ca" (include "eric-ctrl-bro.kvdbrd.hostname" .)) ((((.Values.global).keyValueDatabaseRD).newOperator).enabled  | default false) -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "eric-ctrl-bro.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}


{{/*
Chart version.
*/}}
{{- define "eric-ctrl-bro.version" -}}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Merge global tolerations with service tolerations and optionally the deployment
tolerations.
*/}}
{{- define "eric-ctrl-bro.tolerations" -}}
  {{- $deploymentTolerations := list -}}
  {{- $serviceTolerations := list -}}
  {{- $mergedTolerations := list -}}
  {{- $globalTolerations := list -}}
  {{- if .Values.osmn.enabled -}}
  {{- $tolDict := dict -}}
  {{- $tolDict := set $tolDict "key" "node.kubernetes.io/not-ready" -}}
  {{- $tolDict := set $tolDict "operator" "Exists" -}}
  {{- $tolDict := set $tolDict "effect" "NoExecute" -}}
  {{- $tolDict := set $tolDict "tolerationSeconds" 0 -}}
  {{- $deploymentTolerations = append $deploymentTolerations $tolDict -}}
  {{- $tolDict := dict -}}
  {{- $tolDict := set $tolDict "key" "node.kubernetes.io/unreachable" -}}
  {{- $tolDict := set $tolDict "operator" "Exists" -}}
  {{- $tolDict := set $tolDict "effect" "NoExecute" -}}
  {{- $tolDict := set $tolDict "tolerationSeconds" 0 -}}
  {{- $deploymentTolerations = append $deploymentTolerations $tolDict -}}
  {{- end -}}

  {{- if .Values.tolerations -}}
      {{- $serviceTolerations = .Values.tolerations.backupAndRestore -}}
  {{- end -}}

  {{- if (.Values.global).tolerations }}
      {{- $globalTolerations = .Values.global.tolerations -}}
  {{- end -}}
      {{- $result := list -}}
      {{- $nonMatchingItems := list -}}
      {{- $matchingItems := list -}}
      {{- range $globalItem := $globalTolerations -}}
        {{- $globalItemId := include "eric-ctrl-bro.merge-tolerations.get-identifier" $globalItem -}}
        {{- range $serviceItem := $serviceTolerations -}}
          {{- $serviceItemId := include "eric-ctrl-bro.merge-tolerations.get-identifier" $serviceItem -}}
          {{- if eq $serviceItemId $globalItemId -}}
            {{- $matchingItems = append $matchingItems $serviceItem -}}
          {{- end -}}
        {{- end -}}
      {{- end -}}
      {{- range $globalItem := $globalTolerations -}}
        {{- $globalItemId := include "eric-ctrl-bro.merge-tolerations.get-identifier" $globalItem -}}
        {{- $matchCount := 0 -}}
        {{- range $matchItem := $matchingItems -}}
          {{- $matchItemId := include "eric-ctrl-bro.merge-tolerations.get-identifier" $matchItem -}}
          {{- if eq $matchItemId $globalItemId -}}
            {{- $matchCount = add1 $matchCount -}}
          {{- end -}}
        {{- end -}}
        {{- if eq $matchCount 0 -}}
          {{- $nonMatchingItems = append $nonMatchingItems $globalItem -}}
        {{- end -}}
      {{- end -}}
      {{- range $serviceItem := $serviceTolerations -}}
        {{- $serviceItemId := include "eric-ctrl-bro.merge-tolerations.get-identifier" $serviceItem -}}
        {{- $matchCount := 0 -}}
        {{- range $matchItem := $matchingItems -}}
          {{- $matchItemId := include "eric-ctrl-bro.merge-tolerations.get-identifier" $matchItem -}}
          {{- if eq $matchItemId $serviceItemId -}}
            {{- $matchCount = add1 $matchCount -}}
          {{- end -}}
        {{- end -}}
        {{- if eq $matchCount 0 -}}
          {{- $nonMatchingItems = append $nonMatchingItems $serviceItem -}}
        {{- end -}}
      {{- end -}}
	  {{- $mergedTolerations = (concat $result $matchingItems $nonMatchingItems) -}}


  {{- $res := list -}}
  {{- $nonMatchingItem := list -}}
  {{- $matchingItem := list -}}
  {{- range $deploymentItem := $deploymentTolerations -}}
    {{- $deploymentItemId := include "eric-ctrl-bro.merge-tolerations.get-identifier" $deploymentItem -}}
    {{- range $mergedItem := $mergedTolerations -}}
      {{- $mergedItemId := include "eric-ctrl-bro.merge-tolerations.get-identifier" $mergedItem -}}
      {{- if eq $mergedItemId $deploymentItemId -}}
        {{- $matchingItem = append $matchingItem $mergedItem -}}
      {{- end -}}
    {{- end -}}
  {{- end -}}
  {{- range $deploymentItem := $deploymentTolerations -}}
    {{- $deploymentItemId := include "eric-ctrl-bro.merge-tolerations.get-identifier" $deploymentItem -}}
    {{- $matchCounts := 0 -}}
    {{- range $matchItems := $matchingItem -}}
      {{- $matchItemId := include "eric-ctrl-bro.merge-tolerations.get-identifier" $matchItems -}}
      {{- if eq $matchItemId $deploymentItemId -}}
        {{- $matchCounts = add1 $matchCounts -}}
      {{- end -}}
    {{- end -}}
    {{- if eq $matchCounts 0 -}}
      {{- $nonMatchingItem = append $nonMatchingItem $deploymentItem -}}
    {{- end -}}
  {{- end -}}
  {{- range $mergedItem := $mergedTolerations -}}
    {{- $mergedItemId := include "eric-ctrl-bro.merge-tolerations.get-identifier" $mergedItem -}}
    {{- $matchCounts := 0 -}}
    {{- range $matchItems := $matchingItem -}}
      {{- $matchItemId := include "eric-ctrl-bro.merge-tolerations.get-identifier" $matchItems -}}
      {{- if eq $matchItemId $mergedItemId -}}
        {{- $matchCounts = add1 $matchCounts -}}
      {{- end -}}
    {{- end -}}
    {{- if eq $matchCounts 0 -}}
      {{- $nonMatchingItem = append $nonMatchingItem $mergedItem -}}
    {{- end -}}
  {{- end -}}
  {{- $finalTolerations := concat $res $matchingItem $nonMatchingItem -}}
  {{ $finalTolerations | toYaml }}
{{- end -}}

{{/*
Helper function to get the identifier of a tolerations array element.
Assumes all keys except tolerationSeconds are used to uniquely identify
a tolerations array element.
*/}}
{{ define "eric-ctrl-bro.merge-tolerations.get-identifier" }}
  {{- $keyValues := list -}}
  {{- range $key := (keys . | sortAlpha) -}}
    {{- if eq $key "effect" -}}
      {{- $keyValues = append $keyValues (printf "%s=%s" $key (index $ $key)) -}}
    {{- else if eq $key "key" -}}
      {{- $keyValues = append $keyValues (printf "%s=%s" $key (index $ $key)) -}}
    {{- else if eq $key "operator" -}}
      {{- $keyValues = append $keyValues (printf "%s=%s" $key (index $ $key)) -}}
    {{- else if eq $key "value" -}}
      {{- $keyValues = append $keyValues (printf "%s=%s" $key (index $ $key)) -}}
    {{- end -}}
  {{- end -}}
  {{- printf "%s" (join "," $keyValues) -}}
{{ end }}

{{/*
Expand the name of the chart.
*/}}
{{- define "eric-ctrl-bro.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Standard labels of Helm and Kubernetes.
*/}}
{{- define "eric-ctrl-bro.standard-labels" }}
app.kubernetes.io/instance: {{.Release.Name | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service | quote }}
app.kubernetes.io/name: {{ template "eric-ctrl-bro.name" . }}
app.kubernetes.io/version: {{ template  "eric-ctrl-bro.version" . }}
chart: {{ template "eric-ctrl-bro.chart" . }}
{{- end }}

{{/*
Ericsson product info values.
*/}}
{{- define "eric-ctrl-bro.productName" -}}
{{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
{{- printf "%s" $productInfo.productName -}}
{{- end -}}
{{- define "eric-ctrl-bro.productNumber" -}}
{{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
{{- printf "%s" $productInfo.productNumber -}}
{{- end -}}

{{/*
Ericsson pod priority.
*/}}
{{- define "eric-ctrl-bro.priority" -}}
{{- $priority:= .Values.podPriority }}
{{- $priorityPod:= index $priority (include "eric-ctrl-bro.name" .) }}
{{- if $priorityPod }}
{{- $classname:= index $priorityPod "priorityClassName" }}
{{- if $classname }}
{{- printf "%s" $classname }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Ericsson pod resources.
*/}}
{{- define "eric-ctrl-bro.resources" -}}
requests:
{{- if index .Values.resources.backupAndRestore.requests "cpu" }}
  cpu: {{ index .Values.resources.backupAndRestore.requests "cpu" | quote -}}
{{- end }}
  memory: {{ required "memory requests must be set" .Values.resources.backupAndRestore.requests.memory | quote -}}
{{- if index .Values.resources.backupAndRestore.requests "ephemeral-storage" }}
  ephemeral-storage: {{ index .Values.resources.backupAndRestore.requests "ephemeral-storage" | quote -}}
{{- end }}
limits:
{{- if index .Values.resources.backupAndRestore.limits "cpu" }}
  cpu: {{ index .Values.resources.backupAndRestore.limits "cpu" | quote -}}
{{- end }}
  memory: {{ required "memory limits must be set" .Values.resources.backupAndRestore.limits.memory | quote -}}
{{- if index .Values.resources.backupAndRestore.limits "ephemeral-storage" }}
  ephemeral-storage: {{ index .Values.resources.backupAndRestore.limits "ephemeral-storage" | quote -}}
{{- end }}
{{- end -}}

{{/*
Create a user defined label (DR-D1121-068, DR-D1121-060).
*/}}
{{ define "eric-ctrl-bro.config-labels" }}
  {{- $global := (.Values.global).labels -}}
  {{- $service := .Values.labels -}}
  {{- include "eric-ctrl-bro.mergeLabels" (dict "location" .Template.Name "sources" (list $global $service)) -}}
{{- end }}

{{/*
Merged labels for default, which includes standard-labels and config-labels.
*/}}
{{- define "eric-ctrl-bro.labels" -}}
  {{- $standard := include "eric-ctrl-bro.standard-labels" . | fromYaml -}}
  {{- $config := include "eric-ctrl-bro.config-labels" . | fromYaml -}}
  {{- include "eric-ctrl-bro.mergeLabels" (dict "location" .Template.Name "sources" (list $standard $config)) | trim }}
{{- end -}}

{{/*
Create a user defined annotation (DR-D1121-065, DR-D1121-060).
*/}}
{{ define "eric-ctrl-bro.config-annotations" }}
  {{- $global := (.Values.global).annotations -}}
  {{- $service := .Values.annotations -}}
  {{- include "eric-ctrl-bro.mergeAnnotations" (dict "location" .Template.Name "sources" (list $global $service)) -}}
{{- end }}

{{/*
Ericsson product information annotations
*/}}
{{- define "eric-ctrl-bro.product-info" -}}
ericsson.com/product-name: "{{ template "eric-ctrl-bro.productName" . }}"
ericsson.com/product-number: "{{ template "eric-ctrl-bro.productNumber" . }}"
ericsson.com/product-revision: "{{ regexReplaceAll "(.*)[+].*" .Chart.Version "${1}" }}"
{{- end -}}

{{/*
Merged annotations for default, which includes product-info and config-annotations.
*/}}
{{- define "eric-ctrl-bro.annotations" }}
  {{- $productInfo := include "eric-ctrl-bro.product-info" . | fromYaml -}}
  {{- $config := include "eric-ctrl-bro.config-annotations" . | fromYaml -}}
  {{- include "eric-ctrl-bro.mergeAnnotations" (dict "location" .Template.Name "sources" (list $productInfo $config)) | trim }}
{{- end }}

{{/*
Comma separated list of product numbers
*/}}
{{- define "eric-ctrl-bro.productNumberList" }}
{{- range $i, $e := .Values.bro.productNumberList -}}
{{- if eq $i 0 -}}{{- printf " " -}}{{- else -}}{{- printf "," -}}{{- end -}}{{- . -}}
{{- end -}}
{{- end -}}

{{/*
livenessProbeConfig
*/}}
{{- define "eric-ctrl-bro.livenessProbeConfig" }}
periodSeconds : {{ .Values.probes.backupAndRestore.livenessProbe.periodSeconds }}
failureThreshold : {{ .Values.probes.backupAndRestore.livenessProbe.failureThreshold }}
initialDelaySeconds : {{ .Values.probes.backupAndRestore.livenessProbe.initialDelaySeconds }}
timeoutSeconds : {{ .Values.probes.backupAndRestore.livenessProbe.timeoutSeconds }}
{{- end -}}


{{/*
LivenessProbe
*/}}
{{- define "eric-ctrl-bro.livenessProbe" }}
{{- if eq (include "eric-ctrl-bro.globalSecurity" .) "true" -}}
    {{- if eq .Values.service.endpoints.restActions.tls.enforced "required" -}}
        {{- if eq .Values.service.endpoints.restActions.tls.verifyClientCertificate "required" -}}
                    httpGet:
            path: /health/liveness
            port: 9005
            scheme: HTTP
        {{- else -}}
                    httpGet:
            path: /v1/health
            port: {{ .Values.bro.restTlsPort }}
            scheme: HTTPS
        {{- end -}}
    {{- else -}}
                  httpGet:
            path: /v1/health
            port: {{ .Values.bro.restPort }}
    {{- end -}}
{{- else -}}
              httpGet:
            path: /v1/health
            port: {{ .Values.bro.restPort }}
{{- end -}}
{{- end -}}

{{/*
readinessProbeConfig
*/}}
{{- define "eric-ctrl-bro.readinessProbeConfig" }}
periodSeconds : {{ .Values.probes.backupAndRestore.readinessProbe.periodSeconds }}
failureThreshold : {{ .Values.probes.backupAndRestore.readinessProbe.failureThreshold }}
successThreshold : {{ .Values.probes.backupAndRestore.readinessProbe.successThreshold }}
initialDelaySeconds : {{ .Values.probes.backupAndRestore.readinessProbe.initialDelaySeconds }}
timeoutSeconds : {{ .Values.probes.backupAndRestore.readinessProbe.timeoutSeconds }}
{{- end -}}

{{/*
ReadinessProbe
*/}}
{{- define "eric-ctrl-bro.readinessProbe" }}
{{- if eq (include "eric-ctrl-bro.globalSecurity" .) "true" -}}
    {{- if eq .Values.service.endpoints.restActions.tls.enforced "required" -}}
        {{- if eq .Values.service.endpoints.restActions.tls.verifyClientCertificate "required" -}}
                    httpGet:
            path: /health/readiness
            port: 9005
            scheme: HTTP
        {{- else -}}
                    httpGet:
            path: /v1/health
            port: {{ .Values.bro.restTlsPort }}
            scheme: HTTPS
        {{- end -}}
    {{- else -}}
                  httpGet:
            path: /v1/health
            port: {{ .Values.bro.restPort }}
    {{- end -}}
{{- else -}}
              httpGet:
            path: /v1/health
            port: {{ .Values.bro.restPort }}
{{- end -}}
{{- end -}}

{{/*
startupProbeConfig
*/}}
{{- define "eric-ctrl-bro.startupProbeConfig" }}
periodSeconds : {{ .Values.probes.backupAndRestore.startupProbe.periodSeconds }}
failureThreshold : {{ .Values.probes.backupAndRestore.startupProbe.failureThreshold }}
initialDelaySeconds : {{ .Values.probes.backupAndRestore.startupProbe.initialDelaySeconds }}
timeoutSeconds : {{ .Values.probes.backupAndRestore.startupProbe.timeoutSeconds }}
{{- end -}}

{{/*
startupProbe
*/}}
{{- define "eric-ctrl-bro.startupProbe" }}
{{- if eq (include "eric-ctrl-bro.globalSecurity" .) "true" -}}
    {{- if eq .Values.service.endpoints.restActions.tls.enforced "required" -}}
        {{- if eq .Values.service.endpoints.restActions.tls.verifyClientCertificate "required" -}}
                    httpGet:
            path: /health/startup
            port: 9005
            scheme: HTTP
        {{- else -}}
                    httpGet:
            path: /v1/health
            port: {{ .Values.bro.restTlsPort }}
            scheme: HTTPS
        {{- end -}}
    {{- else -}}
                  httpGet:
            path: /v1/health
            port: {{ .Values.bro.restPort }}
    {{- end -}}
{{- else -}}
              httpGet:
            path: /v1/health
            port: {{ .Values.bro.restPort }}
{{- end -}}
{{- end -}}

{{/*
Global Security
*/}}
{{- define "eric-ctrl-bro.globalSecurity" -}}
{{- $g := fromJson (include "eric-ctrl-bro.globalMap" .) -}}
{{ index $g.security.tls "enabled" }}
{{- end -}}

{{/*
PM Server Security Enabled
*/}}
{{- define "eric-ctrl-bro.pmServerSecurityType" -}}
{{- if eq .Values.service.endpoints.scrape.pm.tls.enforced "required" -}}
    {{- if eq .Values.service.endpoints.scrape.pm.tls.verifyClientCertificate "required" -}}
        need
    {{- else -}}
        want
    {{- end -}}
{{- else -}}
    all
{{- end -}}
{{- end -}}

{{/*
CMM Notification Server Security Enabled
*/}}
{{- define "eric-ctrl-bro.cmmNotifServer" -}}
{{- if eq .Values.service.endpoints.cmmHttpNotif.tls.enforced "required" -}}
    {{- if eq .Values.service.endpoints.cmmHttpNotif.tls.verifyClientCertificate "required" -}}
        need
    {{- else -}}
        want
    {{- end -}}
{{- else -}}
    all
{{- end -}}
{{- end -}}

{{/*-------------------------------------------------------------------*/}}
{{/*---------------------- CMEIA (MS1) support ------------------------*/}}
{{/*---- CMEIA MS1 is a feature by Cloud Ran                      -----*/}}
{{/*---- Callbacks arrive from different sources                  -----*/}}
{{/*-------------------------------------------------------------------*/}}
{{/*-----If cmyang.host is set then CMEIA will not be enabled       ---*/}}
{{/*-----To enabled CMEIA set cmyang.host to ""                     ---*/}}
{{- define "eric-ctrl-bro.cmeia.active" -}}
    {{- if .Values.cmyang -}}
      {{- if .Values.cmyang.host -}}

      {{- else }}
        "true"
      {{- end }}
    {{- else }}
      "true"
    {{- end }}
{{- end }}

{{/* Folder where the client CA Cert of Action callback invoker will be mounted */}}
{{- define "eric-ctrl-bro.actionCallbackServer.client.cacert.mountFolder" }}
    {{- printf "%s" "/var/run/secrets/action-client-cacert" -}}
{{- end}}

{{/* Path to the actionService CA Cert */}}
{{- define "eric-ctrl-bro.actionCallbackServer.client.cacertPath" }}
    {{- printf "%s/%s" (include "eric-ctrl-bro.actionCallbackServer.client.cacert.mountFolder" .) "client-cacert.pem" -}}
{{- end}}

{{/* Folder where the client CA Cert of State Data callback invoker will be mounted */}}
{{- define "eric-ctrl-bro.statedataCallbackServer.client.cacert.mountFolder" }}
    {{- printf "%s" "/var/run/secrets/statedata-client-cacert" -}}
{{- end}}

{{/* Path to the statedataService CA Cert */}}
{{- define "eric-ctrl-bro.statedataCallbackServer.client.cacertPath" }}
    {{- printf "%s/%s" (include "eric-ctrl-bro.statedataCallbackServer.client.cacert.mountFolder" .) "client-cacert.pem" -}}
{{- end}}

{{/* Folder where the client CA Cert of Validator callback invoker will be mounted */}}
{{- define "eric-ctrl-bro.validatorCallbackServer.client.cacert.mountFolder" }}
    {{- printf "%s" "/var/run/secrets/validator-client-cacert" -}}
{{- end}}

{{/* Path to the validatorService CA Cert */}}
{{- define "eric-ctrl-bro.validatorCallbackServer.client.cacertPath" }}
    {{- printf "%s/%s" (include "eric-ctrl-bro.validatorCallbackServer.client.cacert.mountFolder" .) "client-cacert.pem" -}}
{{- end}}

{{- define "eric-ctrl-bro.cmyp.ca.mount.paths" -}}
    security.cmm.notifications.client.ca.path = /run/secrets/certs/cmmserver/ca/client-cacertbundle.pem
  {{- if (include "eric-ctrl-bro.cmeia.active" .) }}
    security.cmyp.client.ca.path = NOT_USED
    {{ printf "%s%s" "security.cmm.client.ca.path.action = " (include "eric-ctrl-bro.actionCallbackServer.client.cacertPath" .) }}
    {{ printf "%s%s" "security.cmm.client.ca.path.state =" (include "eric-ctrl-bro.statedataCallbackServer.client.cacertPath" .) }}
    {{ printf "%s%s" "security.cmm.client.ca.path.validator =" (include "eric-ctrl-bro.validatorCallbackServer.client.cacertPath" .) }}
  {{- else }}
    security.cmyp.client.ca.path = /run/secrets/cas/cmyp/client-cacert.pem
  {{- end }}
{{- end }}

{{/*
OSMN secret name
*/}}
{{- define "eric-ctrl-bro.osmn.secretName" -}}
{{- $secretName := printf "%s-%s" .Values.osmn.host "secret" }}
{{- printf "%s" $secretName -}}
{{- end }}

{{/*
Trusted Root Ca
*/}}
{{- define "eric-ctrl-bro.trustedInternalRootCa" -}}
  {{- $g := fromJson (include "eric-ctrl-bro.globalMap" .) -}}
  {{ $g.security.tls.trustedInternalRootCa.secret }}
{{- end -}}

{{/*
configmap volumes + additional volumes
*/}}
{{- define "eric-ctrl-bro.volumes" -}}
- name: health-status-volume
  emptyDir: {}
- name: writeable-tmp-volume
  emptyDir: {}
- name: {{ template "eric-ctrl-bro.name" . }}-logging
  configMap:
    defaultMode: 0444
    name: {{ template "eric-ctrl-bro.name" . }}-logging
{{- if eq .Values.osmn.enabled true }}
- name: {{ template "eric-ctrl-bro.name" . }}-object-store-secret
  secret:
    secretName: {{ template "eric-ctrl-bro.osmn.secretName" . }}
{{- end }}
{{- if (eq (include "eric-ctrl-bro.globalSecurity" .) "true") }}
- name: {{ template "eric-ctrl-bro.name" . }}-server-cert
  secret:
    secretName: {{ template "eric-ctrl-bro.name" . }}-server-cert
- name: {{ template "eric-ctrl-bro.name" . }}-ca
  secret:
    secretName: {{ template "eric-ctrl-bro.name" . }}-ca
- name: {{ template "eric-ctrl-bro.name" . }}-siptls-root-ca
  secret:
    secretName: {{ template "eric-ctrl-bro.trustedInternalRootCa" . }}
{{- if eq .Values.metrics.enabled true }}
- name: eric-pm-server-ca
  secret:
    secretName: {{ template "eric-ctrl-bro.pm-server.name" . }}-ca
{{- end }}
{{- with . }}
{{- $logstreaming := include "eric-ctrl-bro.logstreaming" . | fromYaml }}
{{/* direct or dual log streaming method enables tcp output */}}
{{- if or (eq "direct" (get $logstreaming "logOutput")) (eq "dual" (get $logstreaming "logOutput")) }}
- name: {{ template "eric-ctrl-bro.name" . }}-lt-client-cert
  secret:
    secretName: {{ template "eric-ctrl-bro.name" . }}-lt-client-certificate
{{- end }}
{{- end }}
{{- if .Values.bro.enableConfigurationManagement }}
- name: eric-cmm-tls-client-ca
  secret:
    secretName: {{ template "eric-ctrl-bro.eric-cm-mediator.name" . }}-tls-client-ca-secret
{{/*---------------------- CMEIA (MS1) support ------------------------*/}}
{{- if (include "eric-ctrl-bro.cmeia.active" .) -}}
- name: "action-client-cacert"
  secret:
    secretName: {{ .Values.service.endpoints.action.actionService }}-client-ca
    defaultMode: 0440
- name: "statedata-client-cacert"
  secret:
    secretName: {{ .Values.service.endpoints.statedata.statedataService }}-client-ca
    defaultMode: 0440
- name: "validator-client-cacert"
  secret:
    secretName: {{ .Values.service.endpoints.validator.validatorService }}-client-ca
    defaultMode: 0440
{{- else }}
- name: eric-cmyp-server-ca
  secret:
    secretName: {{ template "eric-ctrl-bro.eric-cm-yang-provider.name" . }}{{ .Values.cmyang.caSecretSuffix }}
{{- end }}
- name: {{ template "eric-ctrl-bro.name" . }}-cmm-client-cert
  secret:
    secretName: {{ template "eric-ctrl-bro.name" . }}-cmm-client-secret
{{- end }}
{{- if .Values.bro.enableNotifications }}
{{- if or .Values.kafka.enabled .Values.messageBusKF.enabled}}
- name: {{ template "eric-ctrl-bro.name" . }}-mbkf-client-cert
  secret:
    secretName: {{ template "eric-ctrl-bro.mbkfClientSecret" . }}
{{- end }}
{{- if eq (include "eric-ctrl-bro.kvdbrd.enabled" .) "true" }}
- name: {{ template "eric-ctrl-bro.name" . }}-kvdb-rd-client-cert
  secret:
    secretName: {{ template "eric-ctrl-bro.name" . }}-kvdb-rd-client-secret
{{- end }}
{{- end }}
{{- end }}
- name: {{ template "eric-ctrl-bro.name" . }}-serviceproperties
  configMap:
    defaultMode: 0444
    name: {{ template "eric-ctrl-bro.name" . }}-serviceproperties
{{ if .Values.volumes -}}
{{ .Values.volumes -}}
{{ end -}}
{{ end -}}

{{/*
configmap volumemounts + additional volume mounts
*/}}
{{- define "eric-ctrl-bro.volumeMounts" -}}
- name: health-status-volume
  mountPath: /healthStatus
- name: writeable-tmp-volume
  mountPath: /temp
- name: {{ template "eric-ctrl-bro.name" . }}-logging
  mountPath: "{{ .Values.bro.logging.logDirectory }}"
- name: {{ template "eric-ctrl-bro.name" . }}-serviceproperties
  mountPath: "/opt/ericsson/br/application.properties"
  subPath: "application.properties"
{{- if eq .Values.osmn.enabled true }}
- name: {{ template "eric-ctrl-bro.name" . }}-object-store-secret
  mountPath: "/run/secrets/certs/objectstore/credentials"
{{- end }}
{{- if (eq (include "eric-ctrl-bro.globalSecurity" .) "true") }}
- name: {{ template "eric-ctrl-bro.name" . }}-server-cert
  mountPath: "/run/secrets/certs/server"
- name: {{ template "eric-ctrl-bro.name" . }}-ca
  mountPath: "/run/secrets/cas/broca/"
- name: {{ template "eric-ctrl-bro.name" . }}-siptls-root-ca
  readOnly: true
  mountPath: /run/secrets/cas/siptls
{{- if eq .Values.metrics.enabled true }}
- name: eric-pm-server-ca
  readOnly: true
  mountPath: /run/secrets/cas/pm
{{- end }}
{{- with . }}
{{- $logstreaming := include "eric-ctrl-bro.logstreaming" . | fromYaml }}
{{/* direct or dual log streaming method enables tcp output */}}
{{- if or (eq "direct" (get $logstreaming "logOutput")) (eq "dual" (get $logstreaming "logOutput"))  }}
- name: {{ template "eric-ctrl-bro.name" . }}-lt-client-cert
  readOnly: true
  mountPath: /run/secrets/certs/logtransformer
{{- end }}
{{- end }}
{{/*---------------------- CMEIA (MS1) support ------------------------*/}}
{{- if .Values.bro.enableConfigurationManagement }}
{{- if (include "eric-ctrl-bro.cmeia.active" .) -}}
- name: "action-client-cacert"
  readOnly: true
  mountPath: {{ include "eric-ctrl-bro.actionCallbackServer.client.cacert.mountFolder" . }}
- name: "statedata-client-cacert"
  readOnly: true
  mountPath: {{ include "eric-ctrl-bro.statedataCallbackServer.client.cacert.mountFolder" . }}
- name: "validator-client-cacert"
  readOnly: true
  mountPath: {{ include "eric-ctrl-bro.validatorCallbackServer.client.cacert.mountFolder" . }}
{{- else }}
- name: eric-cmyp-server-ca
  readOnly: true
  mountPath: /run/secrets/cas/cmyp
{{- end }}
- name: eric-cmm-tls-client-ca
  mountPath: "/run/secrets/certs/cmmserver/ca"
- name: {{ template "eric-ctrl-bro.name" . }}-cmm-client-cert
  mountPath: "/run/secrets/certs/cmmserver"
{{- end }}
{{- if .Values.bro.enableNotifications }}
{{- if or .Values.kafka.enabled .Values.messageBusKF.enabled}}
- name: {{ template "eric-ctrl-bro.name" . }}-mbkf-client-cert
  readOnly: true
  mountPath: /run/secrets/certs/mbkfserver
{{- end }}
{{- if eq (include "eric-ctrl-bro.kvdbrd.enabled" .) "true" }}
- name: {{ template "eric-ctrl-bro.name" . }}-kvdb-rd-client-cert
  readOnly: true
  mountPath: /run/secrets/certs/kvdbrdserver
{{- end }}
{{- end }}
{{- end }}
{{ if .Values.volumeMounts -}}
{{ .Values.volumeMounts -}}
{{ end -}}
{{ end -}}

{{/*
Volume mount name used for StatefulSet.
*/}}
{{- define "eric-ctrl-bro.persistence.persistentVolumeClaim.name" -}}
  {{- printf "%s" "backup-data" -}}
{{- end -}}

{{/*
Create the name of the service account to use. BRO needs the service account (containing cm-key) to access the KMS and decrypt the password.
*/}}
{{- define "eric-ctrl-bro.serviceAccountName" -}}
{{ include "eric-ctrl-bro.name" . }}-cm-key
{{- end -}}

{{- define "eric-ctrl-bro.pullpolicy" -}}
{{- $g := fromJson (include "eric-ctrl-bro.globalMap" .) -}}
{{- $defaultPolicy := index $g.registry "imagePullPolicy" -}}
{{- if ((((.Values.imageCredentials).bro).registry).imagePullPolicy) -}}
imagePullPolicy: {{ default $defaultPolicy .Values.imageCredentials.bro.registry.imagePullPolicy | quote }}
{{- else -}}
imagePullPolicy: {{ $defaultPolicy | quote }}
{{- end -}}
{{- end -}}

{{- define "eric-ctrl-bro.internalIPFamilyAndPolicy" -}}
{{- $g := fromJson (include "eric-ctrl-bro.globalMap" .) -}}
  {{- if $g.internalIPFamily }}
ipFamilies: [{{ $g.internalIPFamily | quote }}]  # ipFamilies was introduced in K8s v1.20
ipFamilyPolicy: SingleStack
  {{- else }}
ipFamilyPolicy: PreferDualStack
  {{- end }}
{{- end -}}

{{- define "eric-ctrl-bro.image.path" -}}
    {{- $productInfo := fromYaml (.Files.Get "eric-product-info.yaml") -}}
    {{- $registryUrl := $productInfo.images.backupAndRestore.registry -}}
    {{- $repoPath := $productInfo.images.backupAndRestore.repoPath -}}
    {{- $name := $productInfo.images.backupAndRestore.name -}}
    {{- $tag := $productInfo.images.backupAndRestore.tag -}}
    {{- if ((.Values.global).registry) -}}
      {{- if .Values.global.registry.url -}}
        {{- $registryUrl = .Values.global.registry.url -}}
      {{- end -}}
      {{- if not (kindIs "invalid" .Values.global.registry.repoPath) -}}
        {{- $repoPath = .Values.global.registry.repoPath -}}
      {{- end -}}
    {{- end -}}
    {{- if .Values.imageCredentials -}}
        {{- if ((((.Values.imageCredentials).bro).registry).url) -}}
            {{- $registryUrl = .Values.imageCredentials.bro.registry.url -}}
        {{- end -}}
        {{- if not (kindIs "invalid" .Values.imageCredentials.bro.repoPath) -}}
            {{- $repoPath = .Values.imageCredentials.bro.repoPath -}}
        {{- end -}}
    {{- end -}}
    {{- if $repoPath -}}
        {{- $repoPath = printf "%s/" $repoPath -}}
    {{- end -}}
    {{- printf "\"%s/%s%s:%s\"" $registryUrl $repoPath $name $tag -}}
{{- end -}}

{{- define "eric-ctrl-bro.pullsecret" -}}
{{- if .Values.imageCredentials }}
  {{- if .Values.imageCredentials.pullSecret }}
      imagePullSecrets:
        - name: {{ .Values.imageCredentials.pullSecret | quote}}
  {{- else if .Values.global -}}
      {{- if .Values.global.pullSecret }}
      imagePullSecrets:
        - name: {{ .Values.global.pullSecret | quote }}
      {{- end -}}
  {{- end }}
{{- else if .Values.global -}}
  {{- if .Values.global.pullSecret }}
      imagePullSecrets:
        - name: {{ .Values.global.pullSecret | quote }}
  {{- end -}}
{{- end }}
{{- end -}}

{{/*
Return the GRPC port set via global parameter if it's set, otherwise 3000
*/}}
{{- define "eric-ctrl-bro.globalBroGrpcServicePort"}}
{{- if .Values.global -}}
    {{- if .Values.global.adpBR -}}
        {{- .Values.global.adpBR.broGrpcServicePort | default 3000 -}}
    {{- else -}}
        3000
    {{- end -}}
{{- else -}}
    3000
{{- end -}}
{{- end -}}

{{/*
Return the brLabelKey set via global parameter if it's set, otherwise adpbrlabelkey
*/}}
{{- define "eric-ctrl-bro.globalBrLabelKey"}}
{{- if .Values.global -}}
    {{- if .Values.global.adpBR -}}
        {{- .Values.global.adpBR.brLabelKey | default "adpbrlabelkey" -}}
    {{- else -}}
        adpbrlabelkey
    {{- end -}}
{{- else -}}
    adpbrlabelkey
{{- end -}}
{{- end -}}

{{/*
Create a merged set of nodeSelectors from global and service level.
*/}}
{{- define "eric-ctrl-bro.nodeSelector" }}
  {{- $serviceNodeSelector := .Values.nodeSelector.backupAndRestore -}}
  {{- if (.Values.global).nodeSelector -}}
    {{- $mergedConfig := mustMergeOverwrite (dict) .Values.global.nodeSelector $serviceNodeSelector -}}
    {{- toYaml $mergedConfig -}}
  {{- else -}}
    {{- toYaml $serviceNodeSelector -}}
  {{- end -}}
{{- end }}

{{/*
Return the fsgroup set via global parameter if it's set, otherwise 10000
*/}}
{{- define "eric-ctrl-bro.fsGroup.coordinated" -}}
{{- if .Values.global -}}
    {{- if .Values.global.fsGroup -}}
        {{- if .Values.global.fsGroup.manual -}}
            {{ .Values.global.fsGroup.manual }}
        {{- else -}}
            {{- if eq .Values.global.fsGroup.namespace true -}}
                 # The 'default' defined in the Security Policy will be used.
            {{- else -}}
                10000
            {{- end -}}
        {{- end -}}
    {{- else -}}
        10000
    {{- end -}}
{{- else -}}
    10000
{{- end -}}
{{- end -}}

{{/*
Define supplementalGroups according to DR-D1123-135.
*/}}
{{- define "eric-ctrl-bro.supplementalGroups" -}}
    {{- $globalGroups := (list) -}}
    {{- if .Values.global -}}
        {{- if .Values.global.podSecurityContext -}}
            {{- if .Values.global.podSecurityContext.supplementalGroups -}}
                {{- $globalGroups = .Values.global.podSecurityContext.supplementalGroups -}}
            {{- end -}}
        {{- end -}}
    {{- end -}}
    {{- $localGroups := (list) -}}
    {{- if .Values.podSecurityContext -}}
        {{- if .Values.podSecurityContext.supplementalGroups -}}
            {{- $localGroups = .Values.podSecurityContext.supplementalGroups -}}
        {{- end -}}
    {{- end -}}
    {{- $mergedGroups := (list) -}}
    {{- if $globalGroups -}}
        {{- $mergedGroups = $globalGroups -}}
    {{- end -}}
    {{- if $localGroups -}}
        {{- $mergedGroups = concat $mergedGroups $localGroups | uniq -}}
    {{- end -}}
    {{- if $mergedGroups -}}
        {{- toYaml $mergedGroups | nindent 8 -}}
    {{- end -}}
    {{- /* Do nothing if both global and local groups are not set */ -}}
{{- end -}}

{{/*
Issuer for LT client cert
*/}}
{{- define "eric-ctrl-bro.certificate-authorities.eric-log-transformer" -}}
{{- if .Values.service.endpoints.lt -}}
  {{- if .Values.service.endpoints.lt.tls -}}
    {{- if .Values.service.endpoints.lt.tls.issuer -}}
      {{- .Values.service.endpoints.lt.tls.issuer -}}
    {{- else -}}
      eric-log-transformer
    {{- end -}}
  {{- else -}}
    eric-log-transformer
  {{- end -}}
{{- else -}}
  eric-log-transformer
{{- end -}}
{{- end -}}

{{- define "eric-ctrl-bro.kafkaOperatorEnabled" }}
{{- $glob := fromJson (include "eric-ctrl-bro.globalMap" .) -}}
{{- $operator := false -}}
{{- if $glob.messageBusKF -}}
    {{- if $glob.messageBusKF.operator -}}
        {{- if hasKey $glob.messageBusKF.operator "enabled" -}}
            {{- $operator = $glob.messageBusKF.operator.enabled -}}
        {{- end -}}
    {{- end -}}
{{- end -}}
{{- $operator -}}
{{- end -}}

{{/*
Define Message Bus KF port
*/}}
{{- define "eric-ctrl-bro.message-bus-kf-port" -}}
{{- $glob := fromJson (include "eric-ctrl-bro.globalMap" .) -}}
{{- $port := int .Values.messageBusKF.tlsPort -}}
{{- if .Values.kafka.tlsPort -}}
  {{- $port = int .Values.kafka.tlsPort -}}
{{- end -}}
{{- if hasKey $glob.security.tls "enabled" -}}
  {{- if not $glob.security.tls.enabled -}}
    {{- if .Values.kafka.port -}}
      {{- $port = int .Values.kafka.port -}}
    {{- else -}}
      {{- $port = int .Values.messageBusKF.port -}}
    {{- end -}}
  {{- end -}}
{{- end -}}
{{- print $port -}}
{{- end -}}

{{/*
Define Message Bus KF endpoint
*/}}
{{- define "eric-ctrl-bro.message-bus-kf-endpoint" -}}
{{- $kafkaEndpoint := "" -}}
{{- $port := include "eric-ctrl-bro.message-bus-kf-port" . -}}
{{- if and (eq (include "eric-ctrl-bro.kafkaOperatorEnabled" .) "true") .Values.messageBusKF.enabled -}}
  {{- $kafkaEndpoint = printf "%s-kafka-bootstrap:%s" .Values.messageBusKF.clusterName $port | quote -}}
{{- else if and .Values.messageBusKF.enabled .Values.messageBusKF.clusterName -}}
  {{- $kafkaEndpoint = printf "%s-client:%s" .Values.messageBusKF.clusterName $port | quote -}}
{{- else if and (eq (include "eric-ctrl-bro.kafkaOperatorEnabled" .) "true") .Values.kafka.enabled -}}
  {{- $kafkaEndpoint = (printf "%s-kafka-bootstrap:%s" (regexReplaceAll  "-client$" .Values.kafka.hostname "") $port) | quote -}}
{{- else if .Values.kafka.hostname -}}
  {{- $kafkaEndpoint = printf "%s:%s" .Values.kafka.hostname $port | quote -}}
{{- end -}}
{{- print $kafkaEndpoint -}}
{{- end -}}

{{/*
Define Message Bus KF client certificate secret
*/}}
{{- define "eric-ctrl-bro.mbkfClientSecret" -}}
{{- $mbkfClientCert := printf "%s-mbkf-client-secret" (include "eric-ctrl-bro.name" .) -}}
{{- if and (eq (include "eric-ctrl-bro.kafkaOperatorEnabled" .) "true") .Values.messageBusKF.enabled (((.Values).messageBusKF).clientCertSecret) -}}
  {{- $mbkfClientCert = (.Values.messageBusKF.clientCertSecret) -}}
{{- end -}}
{{- printf "%s" $mbkfClientCert -}}
{{- end -}}

{{/*
Issuer for MBKF client cert
*/}}
{{- define "eric-ctrl-bro.certificate-authorities.message-bus-kf" -}}
{{ ternary .Values.messageBusKF.clusterName (.Values.kafka.hostname | replace "-client" "") .Values.messageBusKF.enabled }}
{{- end -}}

{{- define "eric-ctrl-bro.eric-sec-sip-tls.name" -}}
{{- if .Values.sipTls -}}
  {{- if .Values.sipTls.host -}}
    {{ .Values.sipTls.host }}
  {{- else -}}
    eric-sec-sip-tls
  {{- end -}}
{{- else -}}
  eric-sec-sip-tls
{{- end -}}
{{- end -}}

{{- define "eric-ctrl-bro.eric-cm-mediator.name" -}}
{{- if .Values.cmm -}}
  {{- if .Values.cmm.host -}}
    {{ .Values.cmm.host }}
  {{- else -}}
    eric-cm-mediator
  {{- end -}}
{{- else -}}
  eric-cm-mediator
{{- end -}}
{{- end -}}

{{- define "eric-ctrl-bro.eric-cm-yang-provider.name" -}}
{{- if .Values.cmyang -}}
  {{- if .Values.cmyang.host -}}
    {{ .Values.cmyang.host }}
  {{- else -}}
    eric-cm-yang-provider
  {{- end -}}
{{- else -}}
  eric-cm-yang-provider
{{- end -}}
{{- end -}}

{{- define "eric-ctrl-bro.pm-server.name" -}}
{{- if .Values.pm -}}
  {{- if .Values.pm.host -}}
    {{ .Values.pm.host }}
  {{- else -}}
    eric-pm-server
  {{- end -}}
{{- else -}}
  eric-pm-server
{{- end -}}
{{- end -}}

{{/*
Issuer for CMM client cert
*/}}
{{- define "eric-ctrl-bro.certificate-authorities.cmm" -}}
{{- if .Values.cmm -}}
  {{- if .Values.cmm.host -}}
    {{ .Values.cmm.host }}
  {{- else -}}
    eric-cm-mediator
  {{- end -}}
{{- else -}}
  eric-cm-mediator
{{- end -}}
{{- end -}}

{{/*
Service logging level. Preference order is log.level, bro.logging.level, default of "info"
log.level left purposefully unset in default values.yaml to avoid NBC
*/}}
{{- define "eric-ctrl-bro.log.level" -}}
{{- if .Values.log.level -}}
  {{ .Values.log.level }}
{{- else -}}
  {{- if .Values.bro.logging.level -}}
    {{ .Values.bro.logging.level }}
  {{- else -}}
    info
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
Service logging root level. Preference order is log.rootLevel, bro.logging.rootLevel, default of "info"
log.rootLevel left purposefully unset in default values.yaml to avoid NBC
*/}}
{{- define "eric-ctrl-bro.log.rootLevel" -}}
{{- if .Values.log.rootLevel -}}
  {{ .Values.log.rootLevel }}
{{- else -}}
  {{- if .Values.bro.logging.rootLevel -}}
    {{ .Values.bro.logging.rootLevel }}
  {{- else -}}
    info
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
Service logging log4j2 level. Preference order is log.log4j2Level, bro.logging.log4j2Level, default of "info"
log.log4j2Level left purposefully unset in default values.yaml to avoid NBC
*/}}
{{- define "eric-ctrl-bro.log.log4j2Level" -}}
{{- if .Values.log.log4j2Level -}}
  {{ .Values.log.log4j2Level }}
{{- else -}}
  {{- if .Values.bro.logging.log4j2Level -}}
    {{ .Values.bro.logging.log4j2Level }}
  {{- else -}}
    info
  {{- end -}}
{{- end -}}
{{- end -}}

{{/*
Defines time in milliseconds before channel timeout for SFTP client.
*/}}
{{- define "eric-ctrl-bro.sftpTimeout" -}}
{{- if .Values.sftpTimeout -}}
    {{ .Values.sftpTimeout }}
{{- else -}}
    5000
{{- end -}}
{{- end -}}

{{/*
Defines timeout in seconds waiting for bytes from SFTP.
*/}}
{{- define "eric-ctrl-bro.monitorTimeout" -}}
{{- $broftp:= .Values.sftp }}
{{- if $broftp }}
    {{- $inactivityTimeout:= index $broftp "inactivity" }}
    {{- if $inactivityTimeout }}
        {{- .Values.sftp.inactivity.seconds | default 0 -}}
    {{- else -}}
        0
    {{- end }}
{{- else -}}
    0
{{- end }}
{{- end }}

{{/*
Create a merged set of parameters for log streaming from global and service level.
Expectation is that the user calls fromYaml on the other side, e.g.
  {{ $data := include "eric-ctrl-bro.logstreaming" . | fromYaml }}
  port={{ $data.logtransformer.port | quote }}
*/}}
{{ define "eric-ctrl-bro.logstreaming" }}
  {{- $globalValues := dict }}
  {{- $globalValues = merge $globalValues (dict "logOutput" (list)) -}}
  {{- $globalValues = merge $globalValues (dict "logtransformer" (dict "host" "eric-log-transformer")) -}}
  {{- $globalValues = merge $globalValues (dict "logtransformer" (dict "port" "5015")) -}}
  {{- $logStreamingMethod := "indirect" -}}


{{/*
The ordering here is relevant, as we want local settings for host to be overridden by global host settings. The outputs
streams are merged in such a way that the order in which the merge occurs is irrelevant
*/}}
  {{- if .Values.logtransformer -}}
    {{- $globalValues = mergeOverwrite $globalValues (dict "logtransformer" (dict "host" .Values.logtransformer.host)) -}}
  {{- end -}}

  {{- if .Values.global -}}
    {{- if .Values.global.logtransformer }}
      {{- $globalValues = mergeOverwrite $globalValues (dict "logtransformer" (dict "host" .Values.global.logtransformer.host)) -}}
    {{- end }}
  {{- end -}}

  {{- $outputs := get $globalValues "logOutput" -}}
  {{- if .Values.log -}}
  {{- if .Values.log.streamingMethod -}}
     {{- $logStreamingMethod = .Values.log.streamingMethod }}
  {{- else -}}
    {{- $global := fromJson (include "eric-ctrl-bro.globalMap" .) -}}
    {{- if $global.log.streamingMethod }}
      {{- $logStreamingMethod = $global.log.streamingMethod }}
    {{- else }}
      {{- if has "tcp" $outputs }}
        {{- $logStreamingMethod = "direct" }}
        {{- if has "console" $outputs}}
          {{- $logStreamingMethod = "dual" }}
        {{- end }}
      {{- end }}
    {{- end }}
  {{- end }}
  {{- end }}
  {{- $globalValues = mergeOverwrite $globalValues (dict "logOutput" $logStreamingMethod) -}}

  {{- if (eq (include "eric-ctrl-bro.globalSecurity" .) "true") -}}
    {{- $globalValues = mergeOverwrite $globalValues (dict "logtransformer" (dict "port" .Values.logtransformer.tlsPort)) -}}
  {{- else -}}
   {{- $globalValues = mergeOverwrite $globalValues (dict "logtransformer" (dict "port" .Values.logtransformer.port)) -}}
  {{- end -}}
  {{ toJson $globalValues -}}
{{ end }}

{{/*
Define the security-policy reference
{{- define "eric-ctrl-bro.securityPolicy.reference" -}}
{{- $policyreference := index .Values "global" "security" "policyReferenceMap" "default-restricted-security-policy" -}}
{{- end -}}
*/}}

{{/*
Define the security-policy annotations
{{- define "eric-ctrl-bro.securityPolicy.annotations" -}}
ericsson.com/security-policy.name: "restricted/default"
ericsson.com/security-policy.privileged: "false"
ericsson.com/security-policy.capabilities: "N/A"
{{- end -}}
*/}}

{{/*
Security policy rolename
*/}}
{{- define "eric-ctrl-bro.securityPolicyRolename" -}}
{{- $policyrolename := (include "eric-ctrl-bro.name" .) -}}
{{- $rolename:= index .Values "securityPolicy" "eric-ctrl-bro" "rolename" }}
{{- if $rolename }}
    {{- $policyrolename = $rolename -}}
{{- end -}}
{{- $policyrolename -}}
{{- end -}}

{{/*
Create the security policy rolebinding name according to DR-D1123-134.
*/}}
{{- define "eric-ctrl-bro.security-policy-rolebinding.name" -}}
{{- $g := fromJson (include "eric-ctrl-bro.globalMap" .) -}}
{{- if (eq ($g.securityPolicy.rolekind) "Role") }}
{{- printf "%s-cm-key-r-%s-sp" (include "eric-ctrl-bro.name" .) (include "eric-ctrl-bro.securityPolicyRolename" .) | trunc 63 | trimSuffix "-" -}}
{{- else if (eq ($g.securityPolicy.rolekind) "ClusterRole") }}
{{- printf "%s-cm-key-c-%s-sp" (include "eric-ctrl-bro.name" .) (include "eric-ctrl-bro.securityPolicyRolename" .)  | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-cm-key-security-policy" (include "eric-ctrl-bro.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end }}
{{- end -}}

{{/*
Defines the appArmor profile annotation for the BRO container.
The configuration can be set per container, but it applies to all containers
when the container name is ommited.
*/}}
{{- define "eric-ctrl-bro.appArmorAnnotation" }}
{{- if .Values.appArmorProfile }}
{{- $profile := .Values.appArmorProfile }}
{{- $containerName := (include "eric-ctrl-bro.name" .)}}
{{- if index $profile $containerName }}
{{- $profile = index $profile $containerName }}
{{- end }}
{{- include "eric-ctrl-bro.getAppArmorAnnotationFromProfile" (dict "profile" $profile "containerName" $containerName)}}
{{- end }}
{{- end }}

{{/*
Gets the appArmor annotation for the BRO container
from the appArmor profile object
*/}}
{{- define "eric-ctrl-bro.getAppArmorAnnotationFromProfile" }}
{{- $profile := index . "profile" }}
{{- $containerName := index . "containerName" }}
{{- if $profile.type}}
{{- $appArmorProfile := lower $profile.type }}
{{- if eq "runtime/default" $appArmorProfile}}
container.apparmor.security.beta.kubernetes.io/{{ $containerName }}: "runtime/default"
{{- else if eq "unconfined" $appArmorProfile}}
container.apparmor.security.beta.kubernetes.io/{{ $containerName }}: "unconfined"
{{- else if eq "localhost" $appArmorProfile}}
{{- $localHostProfile := $profile.localhostProfile }}
{{- if $localHostProfile }}
{{- $localHostProfileList := (splitList "/" $localHostProfile)}}
{{- if (last $localHostProfileList) }}
container.apparmor.security.beta.kubernetes.io/{{ $containerName }}: "localhost/{{ (last $localHostProfileList) }}"
{{- end }}
{{- end }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Defines the Seccomp security context for the BRO container.
The configuration can be set per container, but it applies to all containers
when the container name is ommited.
*/}}
{{- define "eric-ctrl-bro.secCompSecurityContext" }}
{{- if .Values.seccompProfile }}
{{- $profile := .Values.seccompProfile }}
{{- $containerName := (include "eric-ctrl-bro.name" .)}}
{{- if index $profile $containerName }}
{{- $profile = index $profile $containerName }}
{{- end }}
{{- include "eric-ctrl-bro.getSeccompSecurityContextFromProfile" (dict "profile" $profile)}}
{{- end }}
{{- end }}

{{/*
Gets the Seccomp security context for the BRO container
from the Seccomp profile object.
*/}}
{{- define "eric-ctrl-bro.getSeccompSecurityContextFromProfile" }}
{{- $profile := index . "profile" }}
{{- if $profile.type}}
{{- $seccompProfile := lower $profile.type }}
{{- if eq "runtimedefault" $seccompProfile}}
seccompProfile:
  type: RuntimeDefault
{{- else if eq "unconfined" $seccompProfile}}
seccompProfile:
  type: Unconfined
{{- else if eq "localhost" $seccompProfile}}
{{- if $profile.localhostProfile }}
seccompProfile:
  type: Localhost
  localhostProfile: {{ $profile.localhostProfile }}
{{- end }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Gets the dscp for the egress traffic from the BRO pod
to the SFTP Server
*/}}
{{- define "eric-ctrl-bro.getEgressbackupStorageSvrDscp" }}
{{- with $.Values.egress.backupStorageSvr }}
{{- $dscp := int .dscp }}
{{- if and (ge $dscp 0) (le $dscp 63) }}
{{- $dscp }}
{{- else }}
{{- fail (print "The value for egress.backupStorageSvr.dscp is not within the allowed range [0...63] :  " .dscp) }}
{{- end }}
{{- end }}
{{- end }}
