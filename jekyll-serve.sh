#!/usr/bin/env bash
docker-compose -p codekvast -f ./.docker-compose.yml ${@:-up}
