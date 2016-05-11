#!/usr/bin/env bash

if [ $(git status --short | wc -l) -gt 0 ]; then
    echo "The Git workspace is not clean. Git status:"
    git status --short
    # exit 1
fi

if [ $(git status --short --branch | grep -i '\[after ' | wc -l) -gt 0 ]; then
    echo "The Git workspace is not pushed to origin. Git status:"
    git status --short --branch
    exit 2
fi