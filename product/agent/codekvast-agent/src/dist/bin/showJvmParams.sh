#!/bin/sh
cd $(dirname $0)/..
CODEKVAST_HOME=$PWD
ENDORSED=${CODEKVAST_HOME}/endorsed
SENSOR=$(find ${ENDORSED} -name *.jar)
CONFIG=${CODEKVAST_HOME}/conf/codekvast.properties
echo -javaagent:${SENSOR}=${CONFIG} -Djava.endorsed.dirs=${ENDORSED}
