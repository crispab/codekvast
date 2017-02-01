#!/usr/bin/env bash

declare IMAGE_NAME=crisp/codekvast-warehouse
declare count=$(docker images --quiet ${IMAGE_NAME} | wc -l)
if [ $count -gt 0 ]; then
    echo "Removing $count Docker images for $IMAGE_NAME"
    docker rmi --force $(docker images --quiet ${IMAGE_NAME})
fi
