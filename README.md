# README for Codekvast

## Directory structure

The product itself lives under `product/`.

Sample projects to use when testing Codekvast lives under `sample/`.

Development tools live under `tools/`.

## Development environment

### Build tool

Codekvast uses **Gradle** as build tool. It uses the Gradle Wrapper, `gradlew`, which is checked in at the root of the workspace.
There is the convenience script `tools/src/script/gradle` which simplifies invocation of gradlew. Install that script in your PATH
and use that script instead of `path/to/gradlew`

### IDE

**Intellij Ultimate Edition 14+** is the recommended IDE with the following required plugins:

1. Lombok Support
1. AspectJ Support

_TODO: fill out this section together with Per_

## How to build the product
    cd <root>/product
    ../gradlew build

Or if using the convenience gradle script:

    cd <root>/product
    gradle build

The rest of this README assumes you use the convenience script.

## How to test with Tomcat+Jenkins

### Start Jenkins in terminal 1

    cd <root>/sample/jenkins
    gradle run

This will download Tomcat 8 and then download and deploy Jenkins into Tomcat. Finally, Tomcat is started with Codekvast Collector attached.
Terminate with Ctrl-C.

### Start codekvast-agent in terminal 2

    cd <root>/product/agent/codekvast-agent
    gradle bootRun

This will launch `codekvast-agent`, that will process the output from the collector attached to Tomcat. The agent will try to upload the
data to http://localhost:8090, which is the default URL for the `codekvast-server`.

The bootRun command is a standard Gradle run command, but with support for hot-deploy of changed classes and resources.

### Start codekvast-server in terminal 3

    cd <root>/product/server/codekvast-server
    gradle bootRun

This will start `codekvast-server` on http://localhost:8090.

### Open a browser at http://localhost:8090

Log in with `user` / `0000`

