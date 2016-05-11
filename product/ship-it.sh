#!/usr/bin/env bash

if [ $(git status --short | wc -l) -gt 0 ]; then
    echo "The Git workspace is not clean. Git status:"
    git status --short
    # exit 1
fi

if [ $(git status --short --branch | grep -i '\[ahead ' | wc -l) -gt 0 ]; then
    echo "The Git workspace is not pushed to origin. Git status:"
    git status --short --branch
    # exit 2
fi

if [ $(git status --short --branch | grep -i '\[behind ' | wc -l) -gt 0 ]; then
    echo "The Git workspace is not pulled from origin. Git status:"
    git status --short --branch
    # exit 3
fi

declare GRADLEW=$(dirname $0)/../gradlew

echo "Cleaning workspace..."
$GRADLEW :product:clean

echo "Building product..."
$GRADLEW :product:build
