#!/bin/bash

set -e

cd $(dirname $0)/..
declare GRADLEW=./gradlew
declare GRADLE_OPTS="${GRADLE_OPTS:--Dorg.gradle.configureondemand=false}"
declare CODEKVAST_VERSION=$(grep codekvastVersion gradle.properties | egrep --only-matching '[0-9.]+')
declare GIT_HASH=$(git rev-parse --short HEAD)
declare BUILD_STATE_FILE=.buildState
declare lastBuilt=$(cat ${BUILD_STATE_FILE} 2>/dev/null)
if [ "$lastBuilt" == "${CODEKVAST_VERSION}-${GIT_HASH}" -a $(git status --porcelain | wc -l) -eq 0 ]; then
  echo "Build is up-to-date with ${CODEKVAST_VERSION}-${GIT_HASH} and workspace is clean; will not build"
  exit 0
fi

declare tasks=${@:-build}

if [ -z "$PHANTOMJS_BIN" ]; then
    echo "Locating phantomjs ..."
    export PHANTOMJS_BIN=$(which phantomjs)
fi

if [ -z "$PHANTOMJS_BIN" ]; then
    echo "phantomjs is missing, cannot run JavScript tests"
    exit 1
fi

echo "Building..."
${GRADLEW} ${GRADLE_OPTS} ${tasks}

echo "Generating aggregated javadoc..."
${GRADLEW} ${GRADLE_OPTS} :product:aggregateJavadoc

echo "Generating coverage report..."
${GRADLEW} ${GRADLE_OPTS} coverageReport

if [ $(git status --porcelain | wc -l) -eq 0 ]; then
    echo "Recording that $CODEKVAST_VERSION-$GIT_HASH has been build..."
    echo "$CODEKVAST_VERSION-$GIT_HASH" > ${BUILD_STATE_FILE}
fi
