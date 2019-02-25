#!/usr/bin/env bash
source $(dirname $0)/.build-common.sh

echo "Removing build state file..."
rm -f ${BUILD_STATE_FILE}

$(dirname $0)/clean-workspace.sh

echo "Removing **/node_modules/..."
find product -name node_modules -type d | xargs rm -fr

echo "Removing **/typings/..."
find product -name typings -type d | xargs rm -fr
