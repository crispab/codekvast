#!/usr/bin/env bash

# Abort on errors
set -e

cd $(dirname $0)/..

declare CODEKVAST_VERSION=$(grep codekvastVersion gradle.properties | egrep --only-matching '[0-9.]+')
declare GIT_HASH=$(git rev-parse --short HEAD)
declare NUM_DIRTY_FILES=$(git status --porcelain | wc -l)
declare COMMITTED_VERSION="${COMMITTED_VERSION}"
declare BUILD_STATE_FILE=.buildState
declare BUILT_FROM_VERSION=$(cat ${BUILD_STATE_FILE} 2>/dev/null)
declare POST_COMMIT_HOOK=.git/hooks/post-commit
