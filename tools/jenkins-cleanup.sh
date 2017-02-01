#!/usr/bin/env bash

declare IMAGE_NAME=crisp/codekvast-warehouse
for pass in $(seq 3); do
    declare count=$(docker images --quiet ${IMAGE_NAME} | wc -l)
    if [ ${count} -gt 0 ]; then
        echo -e "Attempting to remove\n$(docker images ${IMAGE_NAME})"
        docker rmi --force $(docker images --quiet ${IMAGE_NAME}) || true
    fi
done
