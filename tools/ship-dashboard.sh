#!/usr/bin/env bash

provisioning/start-staging.sh

tools/clean-workspace.sh
tools/build-it.sh --console=plain --no-daemon --no-build-cache --max-workers=1 build

provisioning/deploy-application.sh

provisioning/stop-staging.sh
