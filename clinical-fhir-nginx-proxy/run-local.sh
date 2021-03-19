#!/usr/bin/env bash
set -euo pipefail
# =~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=

usage() {
  cat <<EOF
  Usage:
    ${0} <nginx-configuration-file>

${1}
EOF
  exit 1
}

# =~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=

init() {
  WORKING_DIR=$(dirname $0)/..
  cd ${WORKING_DIR}
}

main() {
  local conf=${1:-}

  if [ -z "${conf:-}" ]; then usage "nginx-configuration-file location must be specified."; fi

  local version=$(projectVersion)
  echo "Found project version: ${version}"
  if [ -z "$(docker images --filter=reference=vasdvp/health-apis-clinical-fhir-nginx-proxy:${version} -q)" ]
  then
    echo "Couldn't find docker image for ${version} locally... Rebuilding..."
    mvn clean package -Prelease
  fi

  docker run --rm --detach \
    --name clinical-fhir-proxy \
    --volume ${conf}:/local-nginx.conf \
    --net="host" \
    --env NGINX_CONF=/local-nginx.conf \
    vasdvp/health-apis-clinical-fhir-nginx-proxy:${version}
}

# =~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=

projectVersion() {
  cat pom.xml \
    | grep -A 3 '<artifactId>clinical-fhir-parent</artifactId>' \
    | grep version \
    | sed 's/.*<version>\(.*\)<[/]version>/\1/'
}

# =~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=

init
main $@
