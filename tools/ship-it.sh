#!/usr/bin/env bash

set -e

cd $(dirname $0)/..
declare GRADLEW=./gradlew
declare GRADLE_PROPERTIES=$HOME/.gradle/gradle.properties

echo "Checking that we have Bintray credentials..."
if [ -n "$BINTRAY_USER" -a -n "$BINTRAY_KEY" ]; then
    echo "Environment variables BINTRAY_USER and BINTRAY_KEY are defined"
else
    if [ ! -e  ${GRADLE_PROPERTIES} ]; then
        echo "$GRADLE_PROPERTIES is missing and BINTRAY_USER and/or BINTRAY_KEY is undefined"
        exit 1
    fi

    egrep --quiet '^\s*bintrayUser\s*[:=]\s*\S+$' ${GRADLE_PROPERTIES} || {
        echo "bintrayUser=xxx is missing in $GRADLE_PROPERTIES"
        exit 1
    }

    egrep --quiet '^\s*bintrayKey\s*[:=]\s*\S+$' ${GRADLE_PROPERTIES} || {
        echo "bintrayKey=xxx is missing in $GRADLE_PROPERTIES"
        exit 1
    }
    echo "Found Bintray credentials in  $GRADLE_PROPERTIES"
fi

echo "Checking that Git workspace is clean..."
git status --porcelain --branch | egrep --quiet '^## master\.\.\.origin/master' || {
    echo "The Git workspace is not on the master branch. Git status:"
    git status --short --branch
    exit 2
}

if [ $(git status --porcelain | wc -l) -gt 0 ]; then
    echo "The Git workspace is not clean. Git status:"
    git status --short --branch
    exit 2
fi

echo "Checking that we are in sync with Git origin..."
git fetch --quiet
git status --porcelain --branch | egrep --quiet '^## master\.\.\.origin/master$' || {
    echo "The Git workspace is not synced with origin. Git status:"
    git status --short --branch
    exit 2
}

echo "Checking that we are logged in to Docker Hub..."
docker info 2>/dev/null |egrep --quiet "^Username: " || {
    echo "Not logged in to Docker"
    exit 3
}

echo -n "Everything looks fine.
About to build and publish $(grep codekvastVersion gradle.properties)
Are you sure [N/y]? "
read answer
if [ "${answer}" != 'y' ]; then
    echo "Nothing done."
    exit 4
fi

echo "Stopping the Gradle daemon..."
${GRADLEW} --stop

echo "Cleaning Gradle build state..."
rm -fr ./.gradle

echo "Cleaning workspace..."
${GRADLEW} :product:clean

echo "Building product..."
${GRADLEW} :product:build

echo "Uploading distributions to Bintray codekvast repo..."
${GRADLEW} :product:dist:bintrayUpload

echo "Uploading codekvast-collector to Bintray maven-repo (and jcenter)..."
${GRADLEW} :product:agent:collector:bintrayUpload

echo "Pushing codekvast-warehouse to Docker Hub..."
${GRADLEW} :product:warehouse:pushDockerImage
