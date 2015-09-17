#!/bin/sh
##########################################################################
#
#  codekvast-collector startup script for Tomcat
#
##########################################################################

# Modify this to match your actual installation path
CODEKVAST_HOME=${CODEKVAST_HOME:-/opt/codekvast-daemon}

# Don't touch these unless you know what you are doing!
COLLECTOR=$(find $CATALINA_BASE/endorsed -name codekvast-collector*.jar)
if [ "$COLLECTOR" = "" ]; then
    mkdir -p $CATALINA_BASE/endorsed
    ln -s $(find $CODEKVAST_HOME/javaagents -name codekvast-collector*.jar ) $CATALINA_BASE/endorsed
    ln -s $(find $CODEKVAST_HOME/javaagents -name aspectjweaver*.jar ) $CATALINA_BASE/endorsed
fi
WEAVER=$(find $CATALINA_BASE/endorsed -name aspectjweaver*.jar )
COLLECTOR=$(find $CATALINA_BASE/endorsed -name codekvast-collector*.jar)

CATALINA_OPTS="-javaagent:$COLLECTOR -javaagent:$WEAVER"
