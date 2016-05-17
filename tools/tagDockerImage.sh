#!/usr/bin/env bash

declare IMAGE_NAME=$1
declare VERSION=$2
declare GIT_HASH=$3
declare TAG=${VERSION}-${GIT_HASH}
declare IMAGE_ID=$(docker images | grep ${IMAGE_NAME} | grep "latest" | awk '{print $3}')

if [ -z "$IMAGE_ID" ]; then
    echo "No Docker image $IMAGE_NAME:latest has been built, do 'gradle distDocker' first!"
    exit 2
fi

echo "Tagging image $IMAGE_ID with :$VERSION ..."
docker tag ${IMAGE_ID} ${IMAGE_NAME}:${VERSION}

echo "Tagging image $IMAGE_ID with :$TAG ..."
docker tag ${IMAGE_ID} ${IMAGE_NAME}:${TAG}

docker images | grep ${IMAGE_NAME}
