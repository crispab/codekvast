#!/bin/sh
##########################################################################
#
#  codekvast-collector startup script for Tomcat
#
##########################################################################

# Modify this to match your actual installation path
CODEKVAST_HOME=${CODEKVAST_HOME:-/opt/codekvast-daemon-*}

mkdir -p $CATALINA_BASE/endorsed
COLLECTOR=$(find $CATALINA_BASE/endorsed -name codekvast-collector*.jar)
if [ "$COLLECTOR" = "" ]; then
    cp $(find $CODEKVAST_HOME/javaagents -name codekvast-collector*.jar) $CATALINA_BASE/endorsed
    cp $(find $CODEKVAST_HOME/javaagents -name aspectjweaver*.jar) $CATALINA_BASE/endorsed
fi
COLLECTOR=$(find $CATALINA_BASE/endorsed -name codekvast-collector*.jar)
WEAVER=$(find $CATALINA_BASE/endorsed -name aspectjweaver*.jar)

CATALINA_OPTS="-javaagent:$COLLECTOR -javaagent:$WEAVER"
