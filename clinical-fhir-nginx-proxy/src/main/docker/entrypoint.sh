#!/usr/bin/env bash
set -eo pipefail
set -x

test -n "${DU_AWS_BUCKET_NAME:-}"
test -n "${DU_S3_FOLDER:-}"

log() {
  echo "$(date --iso-8601=seconds) $1"
}

CONF_FILE=${NGINX_CONF:-nginx.conf}

mkdir /nginx

log "Pulling nginx configuration from s3..."
aws s3 cp s3://$DU_AWS_BUCKET_NAME/$DU_S3_FOLDER/$CONF_FILE /nginx/nginx.conf

log "Starting nginx..."
nginx -c /nginx/nginx.conf
