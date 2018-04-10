#!/usr/bin/env bash

# Abort on errors
set -e

cd $(dirname $0)/..

declare GRADLEW=./gradlew
declare GRADLE_OPTS="${GRADLE_OPTS:--Dorg.gradle.configureondemand=false}"
declare CODEKVAST_VERSION=$(grep codekvastVersion gradle.properties | egrep --only-matching '[0-9.]+')
declare GIT_HASH=$(git rev-parse --short HEAD)
declare COMMITTED_VERSION="${CODEKVAST_VERSION}-${GIT_HASH}"
declare NUM_DIRTY_FILES=$(git status --porcelain | grep "product/" | wc -l)
declare BUILD_STATE_FILE=.buildState
declare BUILT_FROM_VERSION=$(cat ${BUILD_STATE_FILE} 2>/dev/null)
declare POST_COMMIT_HOOK=.git/hooks/post-commit
