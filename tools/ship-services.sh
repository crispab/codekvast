#!/usr/bin/env bash

set -e

deploy/start-staging.sh

tools/clean-workspace.sh
tools/build-it.sh --console=plain --no-daemon --no-build-cache --max-workers=1 build

deploy/deploy-login.sh
deploy/deploy-dashboard.sh

deploy/stop-staging.sh
