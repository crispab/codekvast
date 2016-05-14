#!/bin/bash

set -e

cd $(dirname $0)/..
declare GRADLEW=./gradlew

echo "Stopping the Gradle daemon..."
${GRADLEW} --stop

echo "Cleaning Gradle build state..."
rm -fr ./.gradle

echo "Cleaning workspace..."
find product -name build -type d | grep -v node_modules | xargs rm -fr

echo "Building..."
${GRADLEW} build

echo "Generating coverage report..."
${GRADLEW} coverageReport
