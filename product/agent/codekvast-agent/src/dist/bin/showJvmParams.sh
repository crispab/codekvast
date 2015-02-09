#!/bin/sh
cd $(dirname $0)/..
CODEKVAST_HOME=$PWD
JAVAAGENTS=${CODEKVAST_HOME}/javaagents
COLLECTOR=$(find ${JAVAAGENTS} -name '*collector*.jar')
ASPECTJWEAVER=$(find ${JAVAAGENTS} -name '*aspectjweaver*.jar')
CONFIG=${CODEKVAST_HOME}/conf/codekvast.conf
echo export CODEKVAST_CONFIG=${CONFIG}
echo -javaagent:${COLLECTOR} -javaagent:${ASPECTJWEAVER}
