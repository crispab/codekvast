#!/usr/bin/env bash
source $(dirname $0)/.build-common.sh

echo "Removing build state file..."
rm -f ${BUILD_STATE_FILE}

tools/clean-workspace.sh
