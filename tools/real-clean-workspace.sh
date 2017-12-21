#!/usr/bin/env bash

# Abort on errors
set -e

cd $(dirname $0)/..

declare CODEKVAST_VERSION=$(grep codekvastVersion gradle.properties | egrep --only-matching '[0-9.]+')
declare GIT_HASH=$(git rev-parse --short HEAD)
declare BUILD_STATE_FILE=.buildState
declare lastBuilt=$(cat ${BUILD_STATE_FILE} 2>/dev/null)
if [ "$lastBuilt" == "${CODEKVAST_VERSION}-${GIT_HASH}" -a $(git status --porcelain | wc -l) -eq 0 ]; then
  echo "Build is up-to-date with ${CODEKVAST_VERSION}-${GIT_HASH} and workspace is clean; will not clean"
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
