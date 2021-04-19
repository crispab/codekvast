#!/usr/bin/env bash

source $(dirname $0)/.build-common.sh

declare GRADLE_PROPERTIES=$HOME/.gradle/gradle.properties

echo "Checking that we have Bintray credentials..."
if [[ -n "$CODEKVAST_BINTRAY_USER" && -n "$CODEKVAST_BINTRAY_KEY" ]]; then
    echo "Environment variables CODEKVAST_BINTRAY_USER and CODEKVAST_BINTRAY_KEY are defined"
else
    if [[ ! -e  ${GRADLE_PROPERTIES} ]]; then
        echo "$GRADLE_PROPERTIES is missing and CODEKVAST_BINTRAY_USER and/or CODEKVAST_BINTRAY_KEY is undefined"
        exit 1
    fi

    egrep --quiet '^\s*codekvast.bintray.user\s*[:=]\s*\S+$' ${GRADLE_PROPERTIES} || {
        echo "codekvast.bintray.user=xxx is missing in $GRADLE_PROPERTIES"
        exit 1
    }

    egrep --quiet '^\s*codekvast.bintray.key\s*[:=]\s*\S+$' ${GRADLE_PROPERTIES} || {
        echo "codekvast.bintray.key=xxx is missing in $GRADLE_PROPERTIES"
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

if [[ ${NUM_DIRTY_FILES} -gt 0 ]]; then
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
if [[ "${answer}" != 'y' ]]; then
    echo "Nothing done."
    exit 4
fi

tools/clean-workspace.sh
tools/build-it.sh --no-daemon --no-build-cache --max-workers=1 build

echo "Creating Git tag ${CODEKVAST_VERSION}"
git tag --force --message="Version ${CODEKVAST_VERSION}" ${CODEKVAST_VERSION}
git push --force --tags

# Continue after errors
set +e

echo "Uploading to downloads.codekvast.io ..."
${GRADLEW} ${GRADLE_OPTS} :product:dist:uploadToS3

echo "Uploading codekvast-agent-${CODEKVAST_VERSION}.jar to jcenter ..."
${GRADLEW} ${GRADLE_OPTS} :product:agent:java-agent:bintrayUpload

cat << EOF
----------------------------------------------------------------------------------------------------------------------------------------

Almost done.

Here's the ToDo-list for post-release stuff:

* Edit product/dist/src/html/index.html and add the previous version to the list of older versions

* gradlew :product:dist:uploadToS3

* Step codekvastVersion in gradle.properties

* Start a new version in RELEASE-NOTES.md

* Update codekvast-site:
    * Write a news flash about the new version
    * Update src/jbake/content/pages/getting-started.adoc
    * Update src/jbake/content/pages/heroku-add-on.md
    * Preview, commit and push
    * ./publish-site.sh
    * Copy heroku-add-on.md to https://devcenter.heroku.com/admin/articles/4065/edit (without the front matter).

----------------------------------------------------------------------------------------------------------------------------------------
EOF
