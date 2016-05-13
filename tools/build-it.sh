#!/bin/bash

set -e

cd $(dirname $0)/..
declare GRADLEW=./gradlew

echo "Stopping the Gradle daemon..."
${GRADLEW} --stop

echo "Cleaning Gradle build state..."
rm -fr ./.gradle

echo "Cleaning workspace..."
find product -name node_modules -type d | xargs rm -fr
find product -name build -type d | xargs rm -fr

echo "Building..."
${GRADLEW} build
