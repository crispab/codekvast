#!/usr/bin/env bash

declare IMAGE_NAME=crisp/codekvast-warehouse
declare count=$(docker images --quiet ${IMAGE_NAME} | wc -l)
if [ ${count} -gt 0 ]; then
    echo "Attempt to remove $count $IMAGE_NAME images from the local Docker registry"
    docker rmi --force $(docker images --quiet ${IMAGE_NAME}) || true
    docker rmi --force $(docker images --quiet ${IMAGE_NAME}) || true
fi
