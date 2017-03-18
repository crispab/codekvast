#!/usr/bin/env bash

set -e

cd $(dirname $0)/..
declare GRADLEW=./gradlew

${GRADLEW} licenseFormat --rerun-tasks