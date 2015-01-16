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

### Software publishing
Part of Codekvast is published to Bintray. To be able to build you need the following lines in your `~/.gradle/gradle.properties`:

    bintrayUser=
    bintrayKey=

### IDE

**Intellij Ultimate Edition 14+** is the recommended IDE with the following plugins:

1. **Lombok Support** (required)
1. **AspectJ Support** (required)
1. AngularJS (optional)
1. Karma (optional)

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

This will download Tomcat 7 and then download and deploy Jenkins into Tomcat. Finally, Tomcat is started with Codekvast Collector attached.
Terminate with Ctrl-C.

You can access Jenkins at [http://localhost:8080/jenkins](http://localhost:8080/jenkins)

### Start codekvast-agent in terminal 2

    cd <root>/product/agent/codekvast-agent
    gradle run

This will launch **codekvast-agent**, that will process output from all collectors. The agent will try to upload the
data to **http://localhost:8090**, which is the default URL for the **codekvast-server**.

### Start codekvast-server in terminal 3

    cd <root>/product/server/codekvast-server
    gradle bootRun

This will start **codekvast-server** on [http://localhost:8090](http://localhost:8090).

### Open a browser at [http://localhost:8090](http://localhost:8090)

Log in with `user` / `0000`

