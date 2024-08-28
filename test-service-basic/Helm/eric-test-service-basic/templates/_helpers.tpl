{{/*
Chart name and version used in chart label.
*/}}
{{- define "eric-test-service-basic.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Chart version.
*/}}
{{- define "eric-test-service-basic.version" -}}
{{- printf "%s" .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}


{{/*
Allow for override of chart name
*/}}
{{- define "eric-test-service-basic.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "eric-test-service-basic.serviceAccountName" -}}
{{ include "eric-test-service-basic.name" . }}
{{- end -}}


{{/*
Allow for override of agent name
*/}}
{{- define "eric-test-service-basic.agentname" -}}
{{ template "eric-test-service-basic.name" . }}-agent
{{- end -}}

{{/*
Template for k8s labels.
*/}}
{{- define "eric-test-service-basic.k8sLabels" -}}
app.kubernetes.io/managed-by: {{ .Release.Service | quote }}
chart: {{ template "eric-test-service-basic.chart" . }}
app.kubernetes.io/name: {{ template "eric-test-service-basic.agentname" . }}
app.kubernetes.io/version: {{ template "eric-test-service-basic.version" . }}
app.kubernetes.io/instance: {{.Release.Name | quote }}
{{- end -}}

{{/*
Pull secret handling
*/}}
{{- define "eric-test-service-basic.pullsecret" -}}
{{- if .Values.imageCredentials.pullSecret }}
  imagePullSecrets:
    - name: {{ .Values.imageCredentials.pullSecret | quote}}
{{- else if .Values.global -}}
    {{- if .Values.global.pullSecret }}
      imagePullSecrets:
        - name: {{ .Values.global.pullSecret | quote }}
    {{- end -}}
{{- end }}
{{- end -}}


{{/*
Ericsson product info annotations.
*/}}
{{- define "eric-test-service-basic.prodInfoAnnotations" }}
ericsson.com/product-name: "test-service-basic"
ericsson.com/product-number: "CXC 000 0000"
ericsson.com/product-revision: {{regexReplaceAll "(.*)[+].*" .Chart.Version "${1}" }}
{{- end -}}

{{/*
Define the name for configmap used on backuptype
*/}}
{{- define "eric-test-service-basic.backuptype.name" -}}
{{- $name_basic := include "eric-test-service-basic.name" . -}}
{{- $name_compound := default (printf "%s-backuptype" $name_basic ) .Values.brAgent.backupDataModelConfig . -}}
{{- ternary $name_compound (printf "%s-backuptype" $name_basic)  (empty .Values.nameOverride) -}}
{{- end -}}

{{/*
Semi-colon separated list of backup types
*/}}
{{- define "eric-test-service-basic.backupTypes" }}

{{- range $i, $e := .Values.brAgent.backupTypeList -}}
{{- if eq $i 0 -}}{{- printf " " -}}{{- else -}}{{- printf ";" -}}{{- end -}}{{- . -}}
{{- end -}}
{{- end -}}

{{/*
Create the list for backupTypes.
*/}}
{{- define "{{.Chart.Name}}.list.backuptype" -}}
{{- range $idx, $value := .Values.brAgent.backupTypeData }}{{- if $idx }}~{{ end }}{{ $value.name }},{{ $value.fromIndex }},{{ $value.toIndex }}
{{- end -}}
{{- end -}}

{{/*
Global Security
*/}}
{{- define "eric-test-service-basic.globalSecurity" -}}
{{- if .Values.global.security -}}
    {{- if eq .Values.global.security.tls.enabled false -}}
        false
    {{- else -}}
        true
    {{- end -}}
{{- else -}}
    true
{{- end -}}
{{- end -}}

{{/*
TTL for the client cert.
*/}}
{{- define "eric-test-service-basic.clientCertTtl" -}}
{{- .Values.service.endpoints.client.ttl -}}
{{- end -}}

{{/*
configmap volumes + additional volumes
*/}}
{{- define "eric-test-service-basic.volumes" -}}
{{- if (eq (include "eric-test-service-basic.globalSecurity" .) "true") -}}
- name: {{ template "eric-test-service-basic.name" . }}-siptls-ca
  secret:
    secretName: "eric-sec-sip-tls-trusted-root-cert"
- name: {{ template "eric-test-service-basic.name" . }}-client-cert
  secret:
    secretName: {{ template "eric-test-service-basic.name" . }}-client-cert
{{- end }}
- name: {{ template "eric-test-service-basic.name" . }}-logging
  configMap:
    defaultMode: 0444
    name: {{ template "eric-test-service-basic.name" . }}-logging
{{- end -}}

{{/*
configmap volumemounts + additional volume mounts
*/}}
{{- define "eric-test-service-basic.volumeMounts" -}}
- name: {{ template "eric-test-service-basic.name" . }}-logging
  mountPath: "{{ .Values.brAgent.logging.logDirectory }}/{{ .Values.brAgent.logging.log4j2File }}"
  subPath: "{{ .Values.brAgent.logging.log4j2File }}"
{{ if (eq (include "eric-test-service-basic.globalSecurity" .) "true") -}}
- name: {{ template "eric-test-service-basic.name" . }}-siptls-ca
  mountPath: "/run/sec/cas/siptlsca/"
- name: {{ template "eric-test-service-basic.name" . }}-client-cert
  mountPath: "/run/sec/certs/client"
{{ if .Values.volumeMounts -}}
{{ .Values.volumeMounts -}}
{{- end }}
{{ end -}}
{{ end -}}

