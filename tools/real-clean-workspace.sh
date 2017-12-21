#!/usr/bin/env bash

# Abort on errors
set -e

cd $(dirname $0)/..

declare CODEKVAST_VERSION=$(grep codekvastVersion gradle.properties | egrep --only-matching '[0-9.]+')
declare GIT_HASH=$(git rev-parse --short HEAD)
declare BUILD_STATE_FILE=.buildState

declare lastBuilt=
if [ "$(cat ${BUILD_STATE_FILE} 2>/dev/null)" == "${CODEKVAST_VERSION}-${GIT_HASH}" ]; then
  echo "No need to clean, build is up-to-date with ${CODEKVAST_VERSION}-${GIT_HASH}"
  exit 0
fi

echo "Cleaning Gradle build state..."
rm -fr ./.gradle

echo "Cleaning Gradle build cache..."
rm -fr ./.build-cache

echo "Cleaning /tmp/codekvast..."
rm -fr /tmp/codekvast

echo "Cleaning workspace..."
find product -name build -type d | grep -v node_modules | xargs rm -fr
