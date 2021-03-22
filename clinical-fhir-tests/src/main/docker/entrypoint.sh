#!/usr/bin/env bash
set -euo pipefail
# =~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=

init() {
  test -n "${K8S_ENVIRONMENT}"

  if [ -z "${SENTINEL_BASE_DIR:-}" ]; then SENTINEL_BASE_DIR=/sentinel; fi
  cd $SENTINEL_BASE_DIR

  SYSTEM_PROPERTIES=()

  if [ -z "${SENTINEL_ENV:-}" ]; then SENTINEL_ENV=$K8S_ENVIRONMENT; fi
  if [ -z "${CHAPI_URL:-}" ]; then CHAPI_URL=https://$K8S_LOAD_BALANCER; fi
  if [ -z "${DQ_AVAILABLE:-}" ]; then DQ_AVAILABLE="true"; fi
  if [ -z "${VFQ_AVAILABLE:-}" ]; then VFQ_AVAILABLE="true"; fi
}

main() {
  addToSystemProperties "sentinel" "${SENTINEL_ENV}"
  addToSystemProperties "sentinel.chapi.url" "${CHAPI_URL}"
  addToSystemProperties "data-query.is-available" "${DQ_AVAILABLE}"
  addToSystemProperties "vista-fhir-query.is-available" "${VFQ_AVAILABLE}"

  java-tests \
    --module-name "clinical-fhir-tests" \
    --regression-test-pattern ".*IT\$" \
    --smoke-test-pattern ".*ClinicalFhirIT\$" \
    ${SYSTEM_PROPERTIES[@]} \
    $@ \
    2>&1 | grep -v "WARNING: "

  exit $?
}

# =~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=

addToSystemProperties() {
  SYSTEM_PROPERTIES+=("-D$1=$2")
}

# =~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=~=

init
main $@
