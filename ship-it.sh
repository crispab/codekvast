#!/usr/bin/env bash

set -e

declare GRADLEW=$(dirname $0)/gradlew
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

echo -n "About to build and publish $(grep codekvastVersion $(dirname $0)/gradle.properties)
Are you sure [N/y]? "
read answer
if [ "${answer}" != 'y' ]; then
    exit 4
fi

echo "Stopping the Gradle daemon..."
${GRADLEW} --stop

echo "Cleaning Gradle build state..."
rm -fr $(dirname $0)/.gradle

echo "Cleaning workspace..."
${GRADLEW} :product:clean

echo "Building product..."
${GRADLEW} :product:build

echo "Uploading distributions to Bintray codekvast repo..."
${GRADLEW} :product:bintrayUpload -PbintrayRepo=codekvast -PbintrayPkgName=distributions

echo "Uploading codekvast-collector to Bintray maven-repo (and jcenter)..."
${GRADLEW} :product:agent:collector:bintrayUpload -PbintrayRepo=maven-repo -PbintrayPkgName=codekvast-collector

echo "Pushing codekvast-warehouse to Docker Hub..."
${GRADLEW} :product:warehouse:pushDockerImage
