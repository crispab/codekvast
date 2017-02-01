#!/usr/bin/env bash

cd $(dirname $0)/..
declare PROJECT_ROOT=$(pwd)
cd product
../gradlew uptodate
