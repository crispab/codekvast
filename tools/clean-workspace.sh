#!/usr/bin/env bash

source $(dirname $0)/.build-common.sh

if [ "$BUILT_FROM_VERSION" == "${COMMITTED_VERSION}" -a ${NUM_DIRTY_FILES} -eq 0 ]; then
  echo "Build is up-to-date with ${COMMITTED_VERSION} and workspace is clean; will not clean"
  exit 0
fi

if [ ! -f "$POST_COMMIT_HOOK" ]; then
    echo "Installing Git post-commit hook..."
    cat << EOF > ${POST_COMMIT_HOOK}
#!/bin/bash
rm -f ${BUILD_STATE_FILE}
EOF
    chmod +x ${POST_COMMIT_HOOK}
elif [ $(grep ${BUILD_STATE_FILE} ${POST_COMMIT_HOOK} | wc -l) == 0 ]; then
    echo "Augmenting Git post-commit hook..."
    echo "rm -f ${BUILD_STATE_FILE}" >> ${POST_COMMIT_HOOK}
fi

echo "Cleaning Gradle build state..."
rm -fr ./.gradle

echo "Cleaning Gradle build cache..."
rm -fr ./.build-cache

echo "Cleaning /tmp/codekvast..."
rm -fr /tmp/codekvast

echo "Cleaning workspace..."
find product -name build -type d | grep -v node_modules | xargs rm -fr
