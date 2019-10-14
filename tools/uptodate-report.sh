#!/usr/bin/env bash

cd $(dirname $0)/..
declare PROJECT_ROOT=$(pwd)
declare REPORT_FILE=${PROJECT_ROOT}/build/reports/outdated-dependencies.txt
mkdir -p $(dirname ${REPORT_FILE})

echo "Java:" | tee ${REPORT_FILE}
cd ${PROJECT_ROOT}/product
# Exclude dependencies with latest versions that does not work in Java7
../gradlew uptodate --max-workers=1 | egrep "^'" | sort -u | tr -d "'" | egrep -v "com.google.guava:guava:|javax.validation:validation-api:" | tee -a ${REPORT_FILE}

echo -e "\nDashboard JavaScript webapp:" | tee -a ${REPORT_FILE}
cd ${PROJECT_ROOT}/product/server/dashboard/src/webapp
yarn outdated --depth=0 | tee -a ${REPORT_FILE}

echo -e "\nDashboard JavaScript webapp2:" | tee -a ${REPORT_FILE}
cd ${PROJECT_ROOT}/product/server/dashboard/src/webapp2
yarn outdated --depth=0 | tee -a ${REPORT_FILE}
