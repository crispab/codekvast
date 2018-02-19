#!/usr/bin/env bash

set -e

provisioning/start-staging.sh

tools/clean-workspace.sh
tools/build-it.sh --console=plain --no-daemon --no-build-cache --max-workers=1 build

provisioning/deploy-login.sh
provisioning/deploy-dashboard.sh

provisioning/stop-staging.sh
