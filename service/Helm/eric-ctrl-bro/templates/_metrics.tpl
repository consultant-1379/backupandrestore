{{/*---------------------------------------------------------------*/}}
{{/*-------------------- Metrics server ---------------------------*/}}
{{/*---------------------------------------------------------------*/}}

{{/* Metrics server listening port*/}}
{{- define "eric-ctrl-bro.metrics.server.port" }}
        {{- if (eq (include "eric-ctrl-bro.globalSecurity" .) "true") }}
            {{- .Values.bro.pmTlsPort -}}
        {{- else -}}
            {{- if .Values.metrics.enableNewScrapePattern -}}
                {{- .Values.bro.pmPort -}}
            {{- else -}}
                {{- .Values.bro.restPort -}}
            {{- end -}}
        {{- end -}}
{{- end }}

{{/* Metric server scheme HTTP | HTTPS */}}
{{- define "eric-ctrl-bro.metrics.server.scheme" }}
    {{- if (eq (include "eric-ctrl-bro.globalSecurity" .) "true") }}
        {{- printf "%s" "https" -}}
    {{- else }}
        {{- printf "%s" "http" -}}
    {{- end }}
{{- end }}

{{/* Metrics server listening name */}}
{{- define "eric-ctrl-bro.metrics.server.name" }}
    {{- if .Values.metrics.enableNewScrapePattern -}}
        {{- if (eq (include "eric-ctrl-bro.globalSecurity" .) "true") }}
            {{- printf "%s" "https-metrics" -}}
        {{- else -}}
            {{- printf "%s" "http-metrics" -}}
        {{- end -}}
    {{- else -}}
        {{- if (eq (include "eric-ctrl-bro.globalSecurity" .) "true") }}
            {{- printf "%s" "pm-tls" -}}
        {{- else -}}
            {{- printf "%s" "http-metric" -}}
        {{- end -}}
    {{- end -}}
{{- end }}