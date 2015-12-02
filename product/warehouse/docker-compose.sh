#!/usr/bin/env bash
export CODEKVAST_VERSION=$(grep codekvastVersion ../../gradle.properties | tr -d [:space:] | cut -d= -f2)
docker-compose $*
