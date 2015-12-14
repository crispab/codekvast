#!/bin/sh
cd $(dirname $0)
CODEKVAST_HOME=$PWD
COLLECTOR=$(find ${CODEKVAST_HOME}/javaagents -name '*collector*.jar')
ASPECTJWEAVER=$(find ${CODEKVAST_HOME}/javaagents -name '*aspectjweaver*.jar')

echo JAVA_OPTS="-Dcodekvast.home=$CODEKVAST_HOME -javaagent:$CODEKVAST_HOME/javaagents/$(basename $COLLECTOR) -javaagent:$CODEKVAST_HOME/javaagents/$(basename $ASPECTJWEAVER)"
