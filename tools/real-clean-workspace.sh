#!/usr/bin/env bash

# Abort on errors
set -e

cd $(dirname $0)/..

echo "Cleaning Gradle build state..."
rm -fr ./.gradle

echo "Cleaning Gradle build cache..."
rm -fr ./.build-cache

echo "Cleaning /tmp/codekvast..."
rm -fr /tmp/codekvast

echo "Cleaning workspace..."
find product -name build -type d | grep -v node_modules | xargs rm -fr
