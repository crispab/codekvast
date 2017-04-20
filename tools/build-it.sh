#!/bin/bash

set -e

cd $(dirname $0)/..
declare GRADLEW=./gradlew
declare GRADLE_OPTS="${GRADLE_OPTS:--Dorg.gradle.configureondemand=false}"
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

echo "Generating coverage report..."
${GRADLEW} ${GRADLE_OPTS} coverageReport
