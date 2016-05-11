#!/usr/bin/env bash

set -e

declare GIT_STATUS=$(git status --porcelain --branch)

if [  $(echo "$GIT_STATUS" | egrep -v "^##" | wc -l) -gt 0 ]; then
    echo "The Git workspace is not clean. Git status:"
    echo "$GIT_STATUS"
    exit 1
fi

if [ $(echo "$GIT_STATUS" | egrep "^##" | grep -i '\[ahead ' | wc -l) -gt 0 ]; then
    echo "The Git workspace is not pushed to origin. Git status:"
    echo "$GIT_STATUS"
    # exit 2
fi

if [ $(echo "$GIT_STATUS" | egrep "^##" | grep -i '\[behind ' | wc -l) -gt 0 ]; then
    echo "The Git workspace is not pulled from origin. Git status:"
    echo "$GIT_STATUS"
    # exit 3
fi

declare GRADLEW=$(dirname $0)/gradlew

echo "Cleaning workspace..."
# $GRADLEW :product:clean

echo "Building product..."
# $GRADLEW :product:build

echo "Uploading distributions to Bintray..."

echo "Uploading codekvast-collector.jar to jcenter..."

echo "Pushing codekvast-warehouse to Docker Hub..."
