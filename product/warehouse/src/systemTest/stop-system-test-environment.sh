#!/usr/bin/env bash

pkill -f codekvast-warehouse-

docker ps | grep codekvast_systest_ | cut -d' ' -f1 | xargs --no-run-if-empty docker stop

docker ps -a | grep codekvast_systest_ | cut -d' ' -f1 | xargs --no-run-if-empty docker rm
