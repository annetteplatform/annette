#!/usr/bin/env bash
#
# Annette Platform deployment entry point.
#
#   ./deploy.sh              # development: core infrastructure only (default)
#   ./deploy.sh dev          # same as above
#   ./deploy.sh full         # full stack: infra + traefik + frontend + api-gateway +
#                            #   all backend services + demo-ignition (seeds demo data)
#   ./deploy.sh full --tools # additionally start pgAdmin, OpenSearch Dashboards and traefik
#
# Image versions are pinned here (the only place):
#   - backend services + demo-ignition: local builds (docker:publishLocal via build-local.sh)
#   - frontend: pulled from Docker Hub
# Project name is pinned to "annette" in docker-compose.yml, so all commands work
# from any directory with `docker compose -p annette ...`.

set -eo pipefail

export FRONTEND_VERSION="0.5.1"
export BACKEND_VERSION="0.6.0-RC"

MODE="dev"
PROFILE_OPTS=""

while [ $# -gt 0 ]; do
  case "$1" in
    dev | development)
      MODE="dev"
      ;;
    full)
      MODE="full"
      ;;
    --tools)
      PROFILE_OPTS="$PROFILE_OPTS --profile tools"
      ;;
    *)
      echo "Usage: $0 [dev|full] [--tools]" >&2
      exit 1
      ;;
  esac
  shift
done

if [ "$MODE" = "full" ]; then
  PROFILE_OPTS="--profile full $PROFILE_OPTS"
fi

# shellcheck disable=SC2086
exec docker compose $PROFILE_OPTS up -d
