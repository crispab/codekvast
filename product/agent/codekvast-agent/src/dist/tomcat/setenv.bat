@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  codekvast-collector startup script for Tomcat
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Modify this to match your actual installation location
set CODEKVAST_HOME=C:\Program Files\codekvast-agent-@CODEKVAST_VERSION@

set COLLECTOR=%CODEKVAST_HOME%\javaagents\codekvast-collector-@CODEKVAST_VERSION@.jar
set WEAVER=%CATALINA_HOME%\endorsed\aspectjweaver-@ASPECTJ_VERSION@.jar

set CATALINA_OPTS=-javaagent:%COLLECTOR% -javaagent:%WEAVER%

