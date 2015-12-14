@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  codekvast-collector startup script for Tomcat
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

@rem Modify this to match your actual installation location
set CODEKVAST_HOME=C:\Program Files\codekvast-daemon-@gradle.version@

@rem Don't touch below this line!
set COLLECTOR=%CATALINA_HOME%\endorsed\codekvast-collector-@gradle.version@.jar
set WEAVER=%CATALINA_HOME%\endorsed\aspectjweaver-@aspectjVersion@.jar

set CATALINA_OPTS=-javaagent:%COLLECTOR% -javaagent:%WEAVER%

