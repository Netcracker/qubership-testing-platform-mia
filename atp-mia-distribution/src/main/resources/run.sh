#!/usr/bin/env sh
## all parameter coming from OpenShift

if [ "${ATP_MIA_PROJECTS_CONFIG##*.}" = "json" ] && [ -e "${ATP_MIA_PROJECTS_CONFIG}" ]; then
  cp ${ATP_MIA_PROJECTS_CONFIG} ./config/project/
fi

if [ "${ATP_INTERNAL_GATEWAY_ENABLED}" = "true" ]; then
  echo "Internal gateway integration is enabled."
  FEIGN_ATP_CATALOGUE_NAME=${FEIGN_ATP_INTERNAL_GATEWAY_NAME}
  FEIGN_ATP_ENVIRONMENTS_NAME=${FEIGN_ATP_INTERNAL_GATEWAY_NAME}
  FEIGN_ATP_MACROS_NAME=${FEIGN_ATP_INTERNAL_GATEWAY_NAME}
  FEIGN_ATP_NOTIFICATION_NAME=${FEIGN_ATP_INTERNAL_GATEWAY_NAME}
  FEIGN_ATP_USERS_NAME=${FEIGN_ATP_INTERNAL_GATEWAY_NAME}
  FEIGN_ATP_EI_NAME=${FEIGN_ATP_INTERNAL_GATEWAY_NAME}
else
  echo "Internal gateway integration is disabled."
  FEIGN_ATP_CATALOGUE_ROUTE=
  FEIGN_ATP_ENVIRONMENTS_ROUTE=
  FEIGN_ATP_MACROS_ROUTE=
  FEIGN_ATP_NOTIFICATION_ROUTE=
  FEIGN_ATP_USERS_ROUTE=
  FEIGN_ATP_EI_ROUTE=
fi

if [ "${SPRING_PROFILES}" = "secured" ]; then
  SPRING_CONFIG_LOCATION="./config/application-secured.properties"
else
  SPRING_CONFIG_LOCATION="./config/application.properties"
fi

GIT_COMMITTER_NAME=hive-team
GIT_COMMITTER_EMAIL=example@example.com

export HAZELCAST_SERVER_ADDRESS="${HAZELCAST_ADDRESS%%:*}"
export HAZELCAST_SERVER_PORT="${HAZELCAST_ADDRESS##*:}"

JAVA_OPTIONS="${JAVA_OPTIONS} -Dspring.devtools.add-properties=false"
JAVA_OPTIONS="${JAVA_OPTIONS} -XX:+PrintFlagsFinal"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dnashorn.args=\\"-strict --no-java --no-syntax-extensions\\""
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.graylog.on=${GRAYLOG_ON:-false}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.graylog.host=${GRAYLOG_HOST}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.graylog.port=${GRAYLOG_PORT}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dlog.graylog.level=${GRAYLOG_LEVEL:-INFO}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dspring.config.location=${SPRING_CONFIG_LOCATION}"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dspring.cloud.bootstrap.location=./config/bootstrap.properties"
JAVA_OPTIONS="${JAVA_OPTIONS} -Dspring.datasource.url=jdbc:postgresql://${PG_DB_ADDR:?}:${PG_DB_PORT:?}/${MIA_DB_NAME:?}"

# ---
# ./assets/env-variables.json preparation and putting into a file
# which stores variables from deployment.yaml
# for standalone MIA (in case of Catalogue this file is defined on catalogue-fe side)
# ---
# /assets/env-variables.json preparation and putting into a file
# which stores variables from deployment.yaml
mkdir -p "${FE_VARIABLES_FILE_PATH}"
cat >"${HOME_EX}/${FE_VARIABLES_FILE_PATH}/${FE_VARIABLES_FILE_NAME}" <<EOF
{
  "test": "test",
  "WS_EVENT_SOURCE_HEARTBEAT_TIMEOUT": "${WS_EVENT_SOURCE_HEARTBEAT_TIMEOUT:-45000}"
}
EOF

# find and add heap dump path if omitted when HeapDumpOnOutOfMemoryError is ON
if echo "${JAVA_OPTIONS}" | grep -qF '+HeapDumpOnOutOfMemoryError' ; then
  if ! echo "${JAVA_OPTIONS}" | grep -qF 'HeapDumpPath=' ; then
    _storage="$(df --output=source,target | grep -i -m 1 'pvc' | awk '{print $2}')"
    if [ -d "${_storage}" ]; then
      JAVA_OPTIONS="${JAVA_OPTIONS} -XX:HeapDumpPath=${_storage}"
    fi
  fi
fi
/usr/bin/java --add-opens java.base/java.lang=ALL-UNNAMED -XX:MaxRAM=${MAX_RAM:-1024m} ${JAVA_OPTIONS} -cp "./config/:./lib/*" org.qubership.atp.mia.Main
