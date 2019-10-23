#!/usr/bin/env bash
cd $(dirname $0)
docker-compose --project-name=codekvast $*
