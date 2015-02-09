#!/bin/sh
##########################################################################
#
#  codekvast-collector startup script for Tomcat
#
##########################################################################

# Modify this to match your actual installation path
CODEKVAST_HOME=/path/to/codekvast-agent-@CODEKVAST_VERSION@

# Don't touch these unless you know what you are doing!

COLLECTOR=$(find $CODEKVAST_HOME/javaagents -name codekvast-collector*.jar )
WEAVER=$(find $CATALINA_HOME/endorsed -name aspectjweaver*.jar )
if [ "$WEAVER" = "" ]; then
    mkdir $CATALINA_HOME/endorsed
    ln -s $(find $CODEKVAST_HOME/javaagents -name aspectjweaver*.jar ) $CATALINA_HOME/endorsed
    WEAVER=$(find $CATALINA_HOME/endorsed -name aspectjweaver*.jar )
fi

CATALINA_OPTS="-javaagent:$COLLECTOR -javaagent:$WEAVER"
