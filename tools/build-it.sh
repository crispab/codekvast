#!/bin/bash

set -e

cd $(dirname $0)/..
declare GRADLEW=./gradlew

echo "Stopping the Gradle daemon..."
${GRADLEW} --stop

echo "Cleaning Gradle build state..."
rm -fr ./.gradle

echo "Cleaning /tmp/codekvast..."
rm -fr /tmp/codekvast

echo "Cleaning workspace..."
find product -name build -type d | grep -v node_modules | xargs rm -fr

if [ -z "$PHANTOMJS_BIN" ]; then
    echo "Locating phantomjs ..."
    export PHANTOMJS_BIN=$(which phantomjs)
fi

if [ -z "$PHANTOMJS_BIN" ]; then
    echo "phantomjs is missing, cannot run JavScript tests"
    exit 1
fi

echo "Building..."
${GRADLEW} -Dorg.gradle.configureondemand=false build $@

echo "Generating coverage report..."
${GRADLEW} coverageReport
