#!/usr/bin/env bash

set -e

declare GRADLE_PROPERTIES=$HOME/.gradle/gradle.properties

if [ ! -e  ${GRADLE_PROPERTIES} ]; then
    echo "$GRADLE_PROPERTIES is missing"
    exit 1
fi

grep -Eq '^\s*bintrayUser\s*[:=]\s*\S+$' ${GRADLE_PROPERTIES} || {
    echo "bintrayUser=xxx is missing in $GRADLE_PROPERTIES"
    exit 1
}

grep -Eq '^\s*bintrayKey\s*[:=]\s*\S+$' ${GRADLE_PROPERTIES} || {
    echo "bintrayKey=xxx is missing in $GRADLE_PROPERTIES"
    exit 1
}

git status --porcelain --branch | egrep -q '^## master\.\.\.origin/master' || {
    echo "The Git workspace is not on the master branch. Git status:"
    git status --short --branch
    exit 2
}

if [ $(git status --porcelain | wc -l) -gt 0 ]; then
    echo "The Git workspace is not clean. Git status:"
    git status --short --branch
    exit 2
fi

git status --porcelain --branch | egrep -q '^## master\.\.\.origin/master$' || {
    echo "The Git workspace is not synced with origin. Git status:"
    git status --short --branch
    exit 2
}

docker info 2>/dev/null |grep -Eq "^Username: " || {
    echo "Not logged in to Docker"
    exit 3
}

declare GRADLEW=$(dirname $0)/gradlew

echo "Cleaning workspace..."
${GRADLEW} :product:clean

echo "Building product..."
${GRADLEW} :product:build

echo "Uploading distributions to Bintray..."
#${GRADLEW} :product:bintrayUpload

echo "Uploading codekvast-collector.jar to Bintray and jcenter..."
#${GRADLEW} :product:agent:collector:bintrayUpload

echo "Pushing codekvast-warehouse to Docker Hub..."
#${GRADLEW} :product:warehouse:pushDockerImage
