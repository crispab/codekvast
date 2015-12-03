#!/usr/bin/env bash

typeset -r IMAGE_NAME="crisp/codekvast-warehouse"

if [ $(git status --short | wc -l) -gt 0 ]; then
    echo "Your workspace is dirty. Do 'git commit' first!" >&2
    exit 1
fi

DOCKER_USERNAME=$(docker info 2>/dev/null|grep Username)
if [ -z "$DOCKER_USERNAME" ]; then
    echo "You must log in to Docker first!" >&2
    exit 2
fi

VERSION=$(grep codekvastVersion ../../gradle.properties | tr -d [:space:] | cut -d= -f2)
GIT_HASH=$(git log -1 --format=%h)

IMAGE_ID=$(docker images |grep $IMAGE_NAME | grep "$VERSION " | awk '{print $3}')
if [ -z "$IMAGE_ID" ]; then
    echo "No Docker image $IMAGE_NAME:$VERSION has been built, do 'gradle distDocker' first!" >&2
    exit 3
fi

$(dirname $0)/tagDockerImageWithLatest.sh

echo "Pushing image $IMAGE_ID to Docker Hub ..."
docker push $IMAGE_NAME:$VERSION
docker push $IMAGE_NAME:$VERSION-$GIT_HASH
docker push $IMAGE_NAME:latest
