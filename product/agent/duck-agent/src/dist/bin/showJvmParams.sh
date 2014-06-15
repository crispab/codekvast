#!/bin/sh
cd $(dirname $0)/..
DUCK_HOME=$PWD
ENDORSED=${DUCK_HOME}/endorsed
SENSOR=$(find ${ENDORSED} -name *.jar)
CONFIG=${DUCK_HOME}/conf/duck.properties
echo -javaagent:${SENSOR}=${CONFIG} -Djava.endorsed.dirs=${ENDORSED}
