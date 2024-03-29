#!/usr/bin/env bash

source $(dirname $0)/.build-common.sh
set -e

deploy/start-staging.sh

tools/clean-workspace.sh
tools/build-it.sh --no-daemon --no-build-cache --max-workers=1 build

deploy/deploy-backoffice.sh
deploy/deploy-intake.sh
deploy/deploy-login.sh
deploy/deploy-dashboard.sh

deploy/stop-staging.sh
