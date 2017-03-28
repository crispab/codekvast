#!/usr/bin/env bash

declare IMAGE_NAME=$1
declare VERSION=$2
declare GIT_HASH=$3
declare TAG=${VERSION}-${GIT_HASH}

if [ $(git status --short | wc -l) -gt 0 ]; then
    echo "Your workspace is dirty. Do 'git commit' first!" >&2
    exit 1
fi

declare DOCKER_USERNAME=$(docker info 2>/dev/null|grep Username)
if [ -z "$DOCKER_USERNAME" ]; then
    echo "You must log in to Docker first!" >&2
    exit 2
fi

declare IMAGE_ID=$(docker images -q ${IMAGE_NAME}:${TAG})
if [ -z "$IMAGE_ID" ]; then
    echo "No Docker image $IMAGE_NAME:$TAG has been built, do 'gradle buildDockerImage' first!" >&2
    exit 3
fi

echo "Pushing image $IMAGE_ID to Docker Hub ..."
docker push ${IMAGE_NAME}:${VERSION}
docker push ${IMAGE_NAME}:${TAG}
docker push ${IMAGE_NAME}:latest
