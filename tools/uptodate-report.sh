#!/usr/bin/env bash

cd $(dirname $0)/..
declare PROJECT_ROOT=$(pwd)
declare REPORT_FILE=${PROJECT_ROOT}/build/reports/outdated-dependencies.txt
mkdir -p $(dirname ${REPORT_FILE})
cd product

echo "Java:" | tee ${REPORT_FILE}
../gradlew uptodate --max-workers=1 | egrep "^'" | sort -u | tr -d "'" | egrep -v "com.google.guava:guava:|javax.validation:validation-api:" | tee -a ${REPORT_FILE}

echo -e "\nJavaScript:" | tee -a ${REPORT_FILE}
cd dashboard/src/webapp
npm outdated --depth=0 | tee -a ${REPORT_FILE}
