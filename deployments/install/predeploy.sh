#!/usr/bin/env sh

if [ ! -f ./atp-common-scripts/openshift/common.sh ]; then
  echo "ERROR: Cannot locate ./atp-common-scripts/openshift/common.sh"
  exit 1
fi

# shellcheck source=../atp-common-scripts/openshift/common.sh
. ./atp-common-scripts/openshift/common.sh

_ns="${NAMESPACE}"
MIA_DB_NAME="$(env_default "${MIA_DB_NAME}" "${SERVICE_NAME}" "${_ns}")"
MIA_USER="$(env_default "${MIA_USER}" "${SERVICE_NAME}" "${_ns}")"
MIA_PASSWORD="$(env_default "${MIA_PASSWORD}" "${SERVICE_NAME}" "${_ns}")"
MIA_DB="$(env_default "${MIA_DB}" "${SERVICE_NAME}" "${_ns}")"
MIA_DB_USER="$(env_default "${MIA_DB_USER}" "${SERVICE_NAME}" "${_ns}")"
MIA_DB_PASSWORD="$(env_default "${MIA_DB_PASSWORD}" "${SERVICE_NAME}" "${_ns}")"

echo "***** Initializing databases ******"
init_pg "${PG_DB_ADDR}" "${MIA_DB_NAME}" "${MIA_USER}" "${MIA_PASSWORD}" "${PG_DB_PORT}" "${pg_user}" "${pg_pass}"
if [ "${MIA_DB_ENABLE:-true}" = "true" ]; then
  init_mongo "${MONGO_DB_ADDR}" "${MIA_DB}" "${MIA_DB_USER}" "${MIA_DB_PASSWORD}" "${MONGO_DB_PORT}" "${mongo_user}" "${mongo_pass}"
fi

if [ "${MIA_EI_DB_ENABLE:-true}" = "true" ]; then
  init_mongo "${EI_GRIDFS_DB_ADDR:-$MONGO_DB_ADDR}" "${EI_GRIDFS_DB}" "${EI_GRIDFS_USER}" "${EI_GRIDFS_PASSWORD}" "${EI_GRIDFS_DB_PORT:-$GRIDFS_DB_PORT}" "${ei_gridfs_user:-$mongo_user}" "${ei_gridfs_password:-$mongo_pass}"
fi

case ${PAAS_PLATFORM:-OPENSHIFT} in
  COMPOSE)
    echo "***** Copying default config *****"
    mkdir -p data -m 777 && cp -u projects_config.json data/
    ;;
  OPENSHIFT|KUBERNETES)
    ;;
  *)
    echo "ERROR: Unsupported PAAS_PLATFORM '${PAAS_PLATFORM}'. Expected values: COMPOSE, OPENSHIFT, KUBERNETES"
    exit 1
    ;;
esac

echo "***** Setting up encryption *****"
encrypt "${ENCRYPT}" "${SERVICE_NAME}"
