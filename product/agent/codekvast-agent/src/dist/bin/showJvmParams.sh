#!/bin/sh
cd $(dirname $0)/..
CODEKVAST_HOME=$PWD
ENDORSED=${CODEKVAST_HOME}/endorsed
COLLECTOR=$(find ${ENDORSED} -name '*collector*.jar')
CONFIG=${CODEKVAST_HOME}/conf/codekvast.conf
echo -javaagent:${COLLECTOR}=${CONFIG} -Djava.endorsed.dirs=${ENDORSED}
