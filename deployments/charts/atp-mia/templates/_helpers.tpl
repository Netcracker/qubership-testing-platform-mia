{{/* Helper functions, do NOT modify */}}
{{- define "env.default" -}}
{{- $ctx := get . "ctx" -}}
{{- $def := get . "def" | default $ctx.Values.SERVICE_NAME -}}
{{- $pre := get . "pre" | default (eq $ctx.Values.PAAS_PLATFORM "COMPOSE" | ternary "" $ctx.Release.Namespace) -}}
{{- get . "val" | default ((empty $pre | ternary $def (print $pre "_" (trimPrefix "atp-" $def))) | nospace | replace "-" "_") -}}
{{- end -}}

{{- define "env.factor" -}}
{{- $ctx := get . "ctx" -}}
{{- get . "def" | default (eq $ctx.Values.PAAS_PLATFORM "COMPOSE" | ternary "1" (default "3" $ctx.Values.KAFKA_REPLICATION_FACTOR)) -}}
{{- end -}}

{{- define "env.compose" }}
{{- range $key, $val := merge (include "env.lines" . | fromYaml) (include "env.secrets" . | fromYaml) }}
{{ printf "- %s=%s" $key $val }}
{{- end }}
{{- end }}

{{- define "env.cloud" }}
{{- range $key, $val := (include "env.lines" . | fromYaml) }}
- name: {{ $key }}
  value: "{{ $val }}"
{{- end }}
{{- $keys := (include "env.secrets" . | fromYaml | keys | uniq | sortAlpha) }}
{{- if eq (default "" .Values.ENCRYPT) "secrets" }}
{{- $keys = concat $keys (list "ATP_CRYPTO_KEY" "ATP_CRYPTO_PRIVATE_KEY") }}
{{- end }}
{{- range $keys }}
- name: {{ . }}
  valueFrom:
    secretKeyRef:
      name: {{ $.Values.SERVICE_NAME }}-secrets
      key: {{ . }}
{{- end }}
{{- end }}

{{- define "env.host" -}}
{{- $url := .Values.ATP_MIA_URL -}}
{{- if $url -}}
{{- regexReplaceAll "http(s)?://(.*)" $url "${2}" -}}
{{- else -}}
{{- $hosts := dict "KUBERNETES" "atp2k8.managed.telecom.cloud" "OPENSHIFT" "dev-atp-cloud.telecom.com" -}}
{{- print .Values.SERVICE_NAME "-" .Release.Namespace "." (.Values.CLOUD_PUBLIC_HOST | default (index $hosts .Values.PAAS_PLATFORM)) -}}
{{- end -}}
{{- end -}}

{{- define "securityContext.pod" -}}
runAsNonRoot: true
seccompProfile:
  type: "RuntimeDefault"
{{- with .Values.podSecurityContext }}
{{ toYaml . }}
{{- end -}}
{{- end -}}

{{- define "securityContext.container" -}}
allowPrivilegeEscalation: false
capabilities:
  drop: ["ALL"]
{{- with .Values.containerSecurityContext }}
{{ toYaml . }}
{{- end -}}
{{- end -}}
{{/* Helper functions end */}}

{{/* Environment variables to be used AS IS */}}
{{- define "env.lines" }}
ATP_CLEAN_LOGS_INTERVAL_HOURS: "{{ .Values.ATP_CLEAN_LOGS_INTERVAL_HOURS }}"
ATP_HTTP_LOGGING: "{{ .Values.ATP_HTTP_LOGGING }}"
ATP_HTTP_LOGGING_HEADERS: "{{ .Values.ATP_HTTP_LOGGING_HEADERS }}"
ATP_HTTP_LOGGING_HEADERS_IGNORE: "{{ .Values.ATP_HTTP_LOGGING_HEADERS_IGNORE }}"
ATP_HTTP_LOGGING_URI_IGNORE: "{{ .Values.ATP_HTTP_LOGGING_URI_IGNORE }}"
ATP_INTERNAL_GATEWAY_ENABLED: "{{ .Values.ATP_INTERNAL_GATEWAY_ENABLED }}"
ATP_MIA_CRON_CLEAN_GRIDFS: "{{ .Values.ATP_MIA_CRON_CLEAN_GRIDFS }}"
ATP_MIA_CRON_CLEAN_GRIDFS_PROJECT_FILES: "{{ .Values.ATP_MIA_CRON_CLEAN_GRIDFS_PROJECT_FILES }}"
ATP_MIA_CRON_CLEAN_LOGS: "{{ .Values.ATP_MIA_CRON_CLEAN_LOGS }}"
ATP_MIA_CRON_CLEAN_METRIC: "{{ .Values.ATP_MIA_CRON_CLEAN_METRIC }}"
ATP_MIA_CRON_CLEAN_POSTGRESQL: '{{ .Values.ATP_MIA_CRON_CLEAN_POSTGRESQL }}'
ATP_MIA_PROJECTS_CONFIG: "{{ .Values.ATP_MIA_PROJECTS_CONFIG }}"
ATP_MIA_REST_EXECUTION_TIMEOUT: "{{ .Values.ATP_MIA_REST_EXECUTION_TIMEOUT }}"
ATP_SERVICE_PATH: "{{ .Values.ATP_SERVICE_PATH }}"
ATP_SERVICE_PUBLIC: "{{ .Values.ATP_SERVICE_PUBLIC }}"
AUDIT_LOGGING_ENABLE: "{{ .Values.AUDIT_LOGGING_ENABLE }}"
AUDIT_LOGGING_TOPIC_NAME: "{{ include "env.default" (dict "ctx" . "val" .Values.AUDIT_LOGGING_TOPIC_NAME "def" "audit_logging_topic") }}"
AUDIT_LOGGING_TOPIC_PARTITIONS: "{{ .Values.AUDIT_LOGGING_TOPIC_PARTITIONS }}"
AUDIT_LOGGING_TOPIC_REPLICAS: "{{ include "env.factor" (dict "ctx" . "def" .Values.AUDIT_LOGGING_TOPIC_REPLICAS) }}"
CATALOGUE_URL: "{{ .Values.CATALOGUE_URL }}"
CONSUL_ENABLED: "{{ .Values.CONSUL_ENABLED }}"
CONSUL_PORT: "{{ .Values.CONSUL_PORT }}"
CONSUL_PREFIX: "{{ .Values.CONSUL_PREFIX }}"
CONSUL_TOKEN: "{{ .Values.CONSUL_TOKEN }}"
CONSUL_URL: "{{ .Values.CONSUL_URL }}"
CONTENT_SECURITY_POLICY: "{{ .Values.CONTENT_SECURITY_POLICY }}"
DB_ALIVE_LENGTH: "{{ .Values.DB_ALIVE_LENGTH }}"
DB_CLOSE_DELAY: "{{ .Values.DB_CLOSE_DELAY }}"
DB_EXECUTION_TIMEOUT: "{{ .Values.DB_EXECUTION_TIMEOUT }}"
DB_EXECUTION_RECORDS_LIMIT: "{{ .Values.DB_EXECUTION_RECORDS_LIMIT }}"
RESPONSE_FILE_SIZE_LIMIT_BYTES: "{{ .Values.RESPONSE_FILE_SIZE_LIMIT_BYTES }}"
DB_SERVER_KEEP_ALIVE: "{{ .Values.DB_SERVER_KEEP_ALIVE }}"
EUREKA_CLIENT_ENABLED: "{{ .Values.EUREKA_CLIENT_ENABLED }}"
EUREKA_INSTANCE_PREFER_IP_ADDRESS: "{{ .Values.EUREKA_INSTANCE_PREFER_IP_ADDRESS }}"
EI_GRIDFS_DB: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_DB "def" "atp-ei-gridfs") }}"
EI_GRIDFS_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_PASSWORD "def" "atp-ei-gridfs") }}"
EI_GRIDFS_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.EI_GRIDFS_USER "def" "atp-ei-gridfs") }}"
FEIGN_ATP_CATALOGUE_NAME: "{{ .Values.FEIGN_ATP_CATALOGUE_NAME }}"
FEIGN_ATP_CATALOGUE_ROUTE: "{{ .Values.FEIGN_ATP_CATALOGUE_ROUTE }}"
FEIGN_ATP_CATALOGUE_URL: "{{ .Values.FEIGN_ATP_CATALOGUE_URL }}"
FEIGN_ATP_ENVIRONMENTS_NAME: "{{ .Values.FEIGN_ATP_ENVIRONMENTS_NAME }}"
FEIGN_ATP_ENVIRONMENTS_ROUTE: "{{ .Values.FEIGN_ATP_ENVIRONMENTS_ROUTE }}"
FEIGN_ATP_ENVIRONMENTS_URL: "{{ .Values.FEIGN_ATP_ENVIRONMENTS_URL }}"
FEIGN_ATP_INTERNAL_GATEWAY_NAME: "{{ .Values.FEIGN_ATP_INTERNAL_GATEWAY_NAME }}"
FEIGN_ATP_MACROS_ENABLED: "{{ .Values.FEIGN_ATP_MACROS_ENABLED }}"
FEIGN_ATP_MACROS_NAME: "{{ .Values.FEIGN_ATP_MACROS_NAME }}"
FEIGN_ATP_MACROS_ROUTE: "{{ .Values.FEIGN_ATP_MACROS_ROUTE }}"
FEIGN_ATP_MACROS_URL: "{{ .Values.FEIGN_ATP_MACROS_URL }}"
FEIGN_ATP_NOTIFICATION_NAME: "{{ .Values.FEIGN_ATP_NOTIFICATION_NAME }}"
FEIGN_ATP_NOTIFICATION_ROUTE: "{{ .Values.FEIGN_ATP_NOTIFICATION_ROUTE }}"
FEIGN_ATP_NOTIFICATION_URL: "{{ .Values.FEIGN_ATP_NOTIFICATION_URL }}"
FEIGN_ATP_USERS_NAME: "{{ .Values.FEIGN_ATP_USERS_NAME }}"
FEIGN_ATP_USERS_ROUTE: "{{ .Values.FEIGN_ATP_USERS_ROUTE }}"
FEIGN_ATP_USERS_URL: "{{ .Values.FEIGN_ATP_USERS_URL }}"
FEIGN_ATP_EI_NAME: "{{ .Values.FEIGN_ATP_EI_NAME }}"
FEIGN_ATP_EI_ROUTE: "{{ .Values.FEIGN_ATP_EI_ROUTE }}"
FEIGN_ATP_EI_URL: "{{ .Values.FEIGN_ATP_EI_URL }}"
FEIGN_CONNECT_TIMEOUT: {{ .Values.FEIGN_CONNECT_TIMEOUT | int | quote }}
FEIGN_LOGGER_LEVEL: "{{ .Values.FEIGN_LOGGER_LEVEL }}"
FEIGN_READ_TIMEOUT: {{ .Values.FEIGN_READ_TIMEOUT | int | quote }}
GIT_API_PATH: "{{ .Values.GIT_API_PATH }}"
GIT_EMAIL: "{{ .Values.GIT_EMAIL }}"
GIT_URL: "{{ .Values.GIT_URL }}"
GIT_USER_ID: "{{ .Values.GIT_USER_ID }}"
GRAYLOG_HOST: "{{ .Values.GRAYLOG_HOST }}"
GRAYLOG_LEVEL: "{{ .Values.GRAYLOG_LEVEL }}"
GRAYLOG_ON: "{{ .Values.GRAYLOG_ON }}"
GRAYLOG_PORT: "{{ .Values.GRAYLOG_PORT }}"
GRAYLOG_STREAM: "{{ .Values.GRAYLOG_STREAM }}"
EI_GRIDFS_DB_ADDR: "{{ .Values.EI_GRIDFS_DB_ADDR }}"
EI_GRIDFS_DB_PORT: "{{ .Values.EI_GRIDFS_DB_PORT }}"
HAZELCAST_CLUSTER_NAME: "{{ .Values.HAZELCAST_CLUSTER_NAME }}"
HAZELCAST_ENABLE: "{{ .Values.HAZELCAST_ENABLE }}"
HAZELCAST_ADDRESS: "{{ .Values.HAZELCAST_ADDRESS }}"
HAZELCAST_SERVER_ENABLED: "{{ .Values.HAZELCAST_SERVER_ENABLED }}"
HIKARI_MAX_LIFE_TIME: {{ .Values.HIKARI_MAX_LIFE_TIME | int | quote }}
HIKARI_MAX_POOL_SIZE: "{{ .Values.HIKARI_MAX_POOL_SIZE }}"
HIKARI_MIN_POOL_SIZE: "{{ .Values.HIKARI_MIN_POOL_SIZE }}"
JAVERS_ENABLED: "{{ .Values.JAVERS_ENABLED }}"
JAVA_OPTIONS: "{{ if .Values.HEAPDUMP_ENABLED }}-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/diagnostic{{ end }} -Dcom.sun.management.jmxremote={{ .Values.JMX_ENABLE }} -Dcom.sun.management.jmxremote.port={{ .Values.JMX_PORT }} -Dcom.sun.management.jmxremote.rmi.port={{ .Values.JMX_RMI_PORT }} -Djava.rmi.server.hostname=127.0.0.1 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false {{ .Values.ADDITIONAL_JAVA_OPTIONS }}"
JSCH_LOG_LEVEL: "{{ .Values.JSCH_LOG_LEVEL }}"
KAFKA_CATALOG_GROUP: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_CATALOG_GROUP "def" "atp_mia_catalog_notification_group") }}"
KAFKA_CATALOG_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_CATALOG_TOPIC "def" "catalog_notification_topic") }}"
KAFKA_ENABLE: "{{ .Values.KAFKA_ENABLE }}"
KAFKA_HANDLER_BACKOFF_INTERVAL: "{{ .Values.KAFKA_HANDLER_BACKOFF_INTERVAL }}"
KAFKA_MIA_EXECUTION_FINISH_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_MIA_EXECUTION_FINISH_TOPIC "def" "mia_execution_finish") }}"
KAFKA_MIA_EXECUTION_FINISH_TOPIC_PARTITIONS: "{{ .Values.KAFKA_MIA_EXECUTION_FINISH_TOPIC_PARTITIONS }}"
KAFKA_MIA_EXECUTION_FINISH_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_MIA_EXECUTION_FINISH_TOPIC_REPLICATION_FACTOR) }}"
KAFKA_ITF_IMPORT_END_PARTITIONS: "{{ .Values.KAFKA_ITF_IMPORT_END_PARTITIONS }}"
KAFKA_ITF_IMPORT_END_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_ITF_IMPORT_END_TOPIC "def" "itf_lite_to_mia_export_finish") }}"
KAFKA_ITF_IMPORT_END_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_ITF_IMPORT_END_TOPIC_REPLICATION_FACTOR) }}"
KAFKA_ITF_IMPORT_GROUP: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_ITF_IMPORT_GROUP "def" "atp_mia_itf_lite_to_mia_export_group") }}"
KAFKA_ITF_IMPORT_LISTENER_DELAY: {{ .Values.KAFKA_ITF_IMPORT_LISTENER_DELAY | int | quote }}
KAFKA_ITF_IMPORT_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_ITF_IMPORT_TOPIC "def" "itf_lite_to_mia_export_start") }}"
KAFKA_ENVIRONMENT_UPDATE_LISTEN_GROUP: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_ENVIRONMENT_UPDATE_LISTEN_GROUP "def" "atp_mia_env_update_listen_group") }}"
KAFKA_ENVIRONMENT_UPDATE_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_ENVIRONMENT_UPDATE_TOPIC "def" "environments_notification_topic") }}"
KAFKA_MIA_ITF_CONSUMER_THREAD: "{{ .Values.KAFKA_MIA_ITF_CONSUMER_THREAD }}"
KAFKA_REPORTING_SERVERS: "{{ .Values.KAFKA_REPORTING_SERVERS }}"
KAFKA_RETRY_BACKOFF_PERIOD: "{{ .Values.KAFKA_RETRY_BACKOFF_PERIOD }}"
KAFKA_SERVERS: "{{ .Values.KAFKA_SERVERS }}"
KAFKA_SERVICE_ENTITIES_TOPIC: "{{ include "env.default" (dict "ctx" . "val" .Values.KAFKA_SERVICE_ENTITIES_TOPIC "def" "service_entities") }}"
KAFKA_SERVICE_ENTITIES_TOPIC_PARTITIONS: "{{ .Values.KAFKA_SERVICE_ENTITIES_TOPIC_PARTITIONS }}"
KAFKA_SERVICE_ENTITIES_TOPIC_REPLICATION_FACTOR: "{{ include "env.factor" (dict "ctx" . "def" .Values.KAFKA_SERVICE_ENTITIES_TOPIC_REPLICATION_FACTOR) }}"
KEYCLOAK_AUTH_URL: "{{ .Values.KEYCLOAK_AUTH_URL }}"
KEYCLOAK_ENABLED: "{{ .Values.KEYCLOAK_ENABLED }}"
KEYCLOAK_REALM: "{{ .Values.KEYCLOAK_REALM }}"
LOCALE_RESOLVER: "{{ .Values.LOCALE_RESOLVER }}"
LOCK_DEFAULT_DURATION_SEC: "{{ .Values.LOCK_DEFAULT_DURATION_SEC }}"
LOCK_RETRY_PACE_SEC: "{{ .Values.LOCK_RETRY_PACE_SEC }}"
LOCK_RETRY_TIMEOUT_SEC: "{{ .Values.LOCK_RETRY_TIMEOUT_SEC }}"
MIA_LOG_LEVEL: "{{ .Values.MIA_LOG_LEVEL }}"
MAX_RAM: "{{ .Values.MAX_RAM }}"
MIA_DB: "{{ include "env.default" (dict "ctx" . "val" .Values.MIA_DB "def" .Values.SERVICE_NAME) }}"
MIA_DB_CLEAN_PERIOD_DAYS: "{{ .Values.MIA_DB_CLEAN_PERIOD_DAYS }}"
MIA_DB_ENABLE: "{{ .Values.MIA_DB_ENABLE }}"
MIA_EI_DB_ENABLE: "{{ .Values.MIA_EI_DB_ENABLE }}"
MIA_DB_NAME: "{{ include "env.default" (dict "ctx" . "val" .Values.MIA_DB_NAME "def" .Values.SERVICE_NAME) }}"
MIA_POT_MAX_LINES_LOGS: "{{ .Values.MIA_POT_MAX_LINES_LOGS }}"
MIA_POT_MAX_TABLE_ROWS: "{{ .Values.MIA_POT_MAX_TABLE_ROWS }}"
MIA_POT_MIN_LOG_LENGTH: "{{ .Values.MIA_POT_MIN_LOG_LENGTH }}"
MIA_SSE_TIMEOUT: "{{ .Values.MIA_SSE_TIMEOUT }}"
MIA_SSE_PING_TIMEOUT: "{{ .Values.MIA_SSE_PING_TIMEOUT }}"
MICROSERVICE_NAME: "{{ .Values.SERVICE_NAME }}"
MONGO_DB_ADDR: "{{ .Values.MONGO_DB_ADDR }}"
MONGO_DB_PORT: "{{ .Values.MONGO_DB_PORT }}"
PG_DB_ADDR: "{{ .Values.PG_DB_ADDR }}"
PG_DB_PORT: "{{ .Values.PG_DB_PORT }}"
PROFILER_ENABLED: "{{ .Values.PROFILER_ENABLED }}"
PROJECT_INFO_ENDPOINT: "{{ .Values.PROJECT_INFO_ENDPOINT }}"
REMOTE_DUMP_HOST: "{{ .Values.REMOTE_DUMP_HOST }}"
REMOTE_DUMP_PORT: "{{ .Values.REMOTE_DUMP_PORT }}"
SERVICE_ENTITIES_MIGRATION_ENABLED: "{{ .Values.SERVICE_ENTITIES_MIGRATION_ENABLED }}"
SERVICE_NAME: "{{ .Values.SERVICE_NAME }}"
SERVICE_REGISTRY_URL: "{{ .Values.SERVICE_REGISTRY_URL }}"
SPRING_PROFILES: "{{ .Values.SPRING_PROFILES }}"
SSH_CLOSE_DELAY: "{{ .Values.SSH_CLOSE_DELAY }}"
SWAGGER_ENABLED: "{{ .Values.SWAGGER_ENABLED }}"
VAULT_ENABLE: "{{ .Values.VAULT_ENABLE }}"
VAULT_NAMESPACE: "{{ .Values.VAULT_NAMESPACE }}"
VAULT_ROLE_ID: "{{ .Values.VAULT_ROLE_ID }}"
VAULT_URI: "{{ .Values.VAULT_URI }}"
ZIPKIN_ENABLE: "{{ .Values.ZIPKIN_ENABLE }}"
ZIPKIN_PROBABILITY: "{{ .Values.ZIPKIN_PROBABILITY }}"
ZIPKIN_URL: "{{ .Values.ZIPKIN_URL }}"
{{- end }}

{{/* Sensitive data to be converted into secrets whenever possible */}}
{{- define "env.secrets" }}
GIT_PASS: "{{ default .Values.infraPassword .Values.GIT_PASS }}"
GIT_TOKEN: "{{ default "tGQJv9h9Bd84wdvoXxUV" .Values.GIT_TOKEN }}"
GIT_USER: "{{ default "x_kube2vcs" .Values.GIT_USER }}"
MIA_DB_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.MIA_DB_PASSWORD "def" .Values.SERVICE_NAME ) }}"
MIA_DB_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.MIA_DB_USER "def" .Values.SERVICE_NAME ) }}"
MIA_PASSWORD: "{{ include "env.default" (dict "ctx" . "val" .Values.MIA_PASSWORD "def" .Values.SERVICE_NAME ) }}"
MIA_USER: "{{ include "env.default" (dict "ctx" . "val" .Values.MIA_USER "def" .Values.SERVICE_NAME ) }}"
KEYCLOAK_CLIENT_NAME: "{{ default "atp-mia" .Values.KEYCLOAK_CLIENT_NAME }}"
KEYCLOAK_SECRET: "{{ default "947509d3-ccb9-49dc-88eb-f6d8ec8e893b" .Values.KEYCLOAK_SECRET }}"
VAULT_SECRET_ID: "{{ default "" .Values.VAULT_SECRET_ID }}"
{{- end }}

{{- define "env.deploy" }}
mongo_pass: "{{ .Values.mongo_pass }}"
mongo_user: "{{ .Values.mongo_user }}"
ei_gridfs_pass: "{{ .Values.ei_gridfs_pass }}"
ei_gridfs_user: "{{ .Values.ei_gridfs_user }}"
pg_pass: "{{ .Values.pg_pass }}"
pg_user: "{{ .Values.pg_user }}"
{{- end }}
