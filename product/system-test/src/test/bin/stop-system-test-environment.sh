#!/usr/bin/env bash

pkill -f codekvast-dashboard- && echo "codekvast-dashboard killed"
pkill -f codekvast-login- && echo "codekvast-login killed"
docker ps | grep codekvast_systest_ | cut -d' ' -f1 | xargs --no-run-if-empty docker stop
docker ps -a | grep codekvast_systest_ | cut -d' ' -f1 | xargs --no-run-if-empty docker rm
