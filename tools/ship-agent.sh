#!/usr/bin/env bash

source $(dirname $0)/.build-common.sh

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

if [ ${NUM_DIRTY_FILES} -gt 0 ]; then
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

echo -n "Everything looks fine.
About to build and publish ${COMMITTED_VERSION}.
Are you sure [N/y]? "
read answer
if [ "${answer}" != 'y' ]; then
    echo "Nothing done."
    exit 4
fi

tools/clean-workspace.sh
tools/build-it.sh --console=plain --no-daemon --no-build-cache --max-workers=1 build

echo "Creating Git tag ${CODEKVAST_VERSION}"
git tag --force --message="Version ${CODEKVAST_VERSION}" ${CODEKVAST_VERSION}
git push --force --tags

# Continue after errors
set +e

echo "Uploading to downloads.codekvast.io ..."
${GRADLEW} --console=plain :product:dist:uploadToS3

echo "Uploading codekvast-agent-${CODEKVAST_VERSION}.jar to jcenter ..."
${GRADLEW} --console=plain :product:java-agent:bintrayUpload
