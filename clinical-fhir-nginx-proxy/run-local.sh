#!/usr/bin/env bash
#=============================================

usage() {
  cat <<EOF
Usage:
  ${0} [options]

Options:
  --debug               Turn on debug output
  -h|--help             Display this menu
  -s|--secrets-conf     Location of secrets file for environment substitution

${1}
EOF
exit 1
}

#=============================================

init() {
  set -euo pipefail

  WORK="$(readlink -f $(dirname $0))"
  cd ${WORK}

  # Clinical-Fhir was renamed. In the unlikely scenario that someone is using the old name, allow override of the var.
  DEPLOYMENT_UNIT="${DEPLOYMENT_UNIT:-health-apis-clinical-fhir-api-deployment}"

  SECRETS_CONF="${WORK}/secrets.conf"
  DEV_CONF="${WORK}/dev-nginx.conf"
}

main() {
  ARGS=$(getopt -n $(basename ${0}) \
      -l "debug,help,nginx-conf:,secrets-conf:" \
      -o "hn:s:" -- "$@")
  [ $? != 0 ] && usage
  eval set -- "$ARGS"
  while true
  do
    case "$1" in
      --debug) set -x;;
      -h|--help) usage "Just don't. Don't even. I can't.";;
      -n|--nginx-conf) NGINX_CONF="${2:-}";;
      -s|--secrets-conf) SECRETS_CONF="${2:-}";;
      --) shift;break;;
    esac
    shift;
  done

  # Source if found, else try anyway (file may have been pre-populated)
  if [ -f "${SECRETS_CONF}" ]
  then 
    source ${SECRETS_CONF}
  else
    echo "Could not find file (${SECRETS_CONF}). Attempting to run anyway..."
  fi

  if [ -z "${NGINX_CONF:-}" ]
  then 
    echo "Using default nginx configuration from deployment-unit."
    NGINX_CONF="$(defaultNginxConf)"
  fi

  cat ${NGINX_CONF} | replacePorts | envsubst > ${DEV_CONF}

  local appVersion="$(determineProjectVersion)"
  findDockerImageForVersion "${appVersion}"

  runClinicalFhirNginxProxy "${appVersion}"
}

#=============================================

defaultNginxConf() {
  local du=$(find "${SHANKTOPUS_WORKSPACE:-${HOME}}" -type d -name "${DEPLOYMENT_UNIT}")
  if [ -z "${du:-}" ]; then usage "Couldn't find clinical-fhir deployment unit: ${DEPLOYMENT_UNIT}";fi
  local conf="$(find ${du} -type f -name 'nginx.properties' | head -n +1)"
  if [ ! -f "${conf:-}" ]; then usage "Couldn't find default nginx.conf file."; fi
  echo "${conf}"
}

determineProjectVersion() {
  cat ${WORK}/../pom.xml \
    | grep -A 3 '<artifactId>clinical-fhir-parent</artifactId>' \
    | grep version \
    | sed 's/.*<version>\(.*\)<[/]version>/\1/'
}

findDockerImageForVersion() {
  local version="${1:-}"

  echo "Version: ${version}"
  if [ -z "$(docker images --filter=reference=vasdvp/health-apis-clinical-fhir-nginx-proxy:${version} -q)" ]
  then
    echo "Couldn't find docker image for ${version} locally... Rebuilding..."
    mvn clean package -Prelease
  fi
}

replacePorts() {
  cat | sed \
    -e "/set.*dq.*/s/\${BLUE_LOAD_BALANCER_PORT}/${DQ_PROXY_PORT:-${BLUE_LOAD_BALANCER_PORT}}/" \
    -e "/set.*vfq.*/s/\${BLUE_LOAD_BALANCER_PORT}/${VFQ_PROXY_PORT:-${BLUE_LOAD_BALANCER_PORT}}/"
}

runClinicalFhirNginxProxy() {
  local version="${1:-}"

  docker run --rm --detach \
    --name clinical-fhir-proxy \
    --volume ${DEV_CONF}:/local-nginx.conf \
    --net="host" \
    --env NGINX_CONF=/local-nginx.conf \
    vasdvp/health-apis-clinical-fhir-nginx-proxy:${version}
}

#=============================================

init
main $@

