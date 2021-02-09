#!/usr/bin/env bash

pkill -f codekvast-backoffice- && echo "codekvast-backoffice killed"
pkill -f codekvast-dashboard- && echo "codekvast-dashboard killed"
pkill -f codekvast-intake- && echo "codekvast-intake killed"
pkill -f codekvast-login- && echo "codekvast-login killed"

docker ps | grep codekvast_systest_mariadb_ | cut -d' ' -f1 | xargs --no-run-if-empty docker stop
docker ps -a | grep codekvast_systest_mariadb_ | cut -d' ' -f1 | xargs --no-run-if-empty docker rm

docker ps | grep codekvast_systest_rabbitmq_ | cut -d' ' -f1 | xargs --no-run-if-empty docker stop
docker ps -a | grep codekvast_systest_rabbitmq_ | cut -d' ' -f1 | xargs --no-run-if-empty docker rm
