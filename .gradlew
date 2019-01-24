#!/usr/bin/env bash
#---------------------------------------------------------------------------------------------------------------
# This is a wrapper for ./gradlew that sets JAVA_HOME to the value of sdkmanJavaDefault in gradle.properties
# It then invokes ./gradlew with --console=plain followed by all passed parameters.
#
# It is used from Jenkinsfile.
#---------------------------------------------------------------------------------------------------------------

declare javaVersion=$(awk '/sdkmanJavaDefault*/ {print $3}' gradle.properties)

env JAVA_HOME=$HOME/.sdkman/candidates/java/$javaVersion ./gradlew --console=plain $*
