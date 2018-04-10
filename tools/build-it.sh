#!/bin/bash

source $(dirname $0)/.build-common.sh

if [ "$BUILT_FROM_VERSION" == "${COMMITTED_VERSION}" -a ${NUM_DIRTY_FILES} -eq 0 ]; then
  echo "Build is up-to-date with ${COMMITTED_VERSION} and workspace is clean. No need to build. Use $(dirname $0)/real-clean-workspace.sh to force."
  exit 0
fi

declare tasks=${@:-build}

echo "Resolving frontend dependencies..."
${GRADLEW} ${GRADLE_OPTS} frontendInstall --max-workers=1

echo "Building..."
${GRADLEW} ${GRADLE_OPTS} ${tasks}

echo "Generating aggregated javadoc..."
${GRADLEW} ${GRADLE_OPTS} :product:aggregateJavadoc

echo "Generating coverage report..."
${GRADLEW} ${GRADLE_OPTS} coverageReport

if [ ${NUM_DIRTY_FILES} -eq 0 -a "${tasks}" == "build" ]; then
    echo "Recorded that ${COMMITTED_VERSION} has been built in a clean workspace."
    echo "${COMMITTED_VERSION}" > ${BUILD_STATE_FILE}
fi
