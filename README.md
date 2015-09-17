# README for Codekvast

## Directory structure

The product itself lives under `product/`.

Sample projects to use when testing Codekvast lives under `sample/`.

Development tools live under `tools/`.

## Development environment

### JDK

Java 8 is required. OpenJDK is recommended.

### Build tool

Codekvast uses **Gradle** as build tool. It uses the Gradle Wrapper, `gradlew`, which is checked in at the root of the workspace.
There is the convenience script `tools/src/script/gradle` which simplifies invocation of gradlew. Install that script in your PATH
and use that script instead of `path/to/gradlew`

### Software publishing
The daemon part of Codekvast is published to Bintray. To be able to upload to Bintray you need the following lines in your `~/.gradle/gradle
.properties`:

    bintrayUser=my-bintray-user
    bintrayKey=my-bintray-key

You also need to be member of the Crisp organisation in Bintray.

### IDE

**Intellij Ultimate Edition 14+** is the recommended IDE with the following plugins:

1. **Lombok Support** (required)
1. Github (optional)
1. AspectJ Support (optional)
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

### Start Jenkins 1 in terminal 1

    gradle :sample:jenkins1:run

This will download Tomcat 7 and then download and deploy one version of Jenkins into Tomcat. Finally, Tomcat is started on port 8081 with 
Codekvast Collector attached.
Terminate with `Ctrl-C`.

You can access Jenkins at [http://localhost:8081/jenkins](http://localhost:8081/jenkins)

### Start Jenkins 2 in terminal 2

    gradle :sample:jenkins2:run

This will download Tomcat 7 and then download and deploy another version of Jenkins into Tomcat. Finally, Tomcat is started on port 8082 
with 
Codekvast Collector attached.
Terminate with `Ctrl-C`.

You can access Jenkins at [http://localhost:8082/jenkins](http://localhost:8082/jenkins)

### Start codekvast-daemon in terminal 3

    gradle :product:agent:codekvast-daemon:run

This will launch **codekvast-daemon**, that will process output from all collectors. The daemon will try to upload the
data to http://localhost:8090, which is the default URL for the **codekvast-server**.
Terminate with `Ctrl-C`.

### Start codekvast-server in terminal 4

    gradle :product:server:codekvast-server:run

This will start **codekvast-server** on [http://localhost:8090](http://localhost:8090).
Terminate with `Ctrl-C`.

### Open a browser at [http://localhost:8090](http://localhost:8090)

Log in with `user` / `0000`

## User Manual

A User Manual located in product/docs/src/asciidoc.

The source is in AsciiDoctor format.

It is built by doing `gradle product:docs:build`.

The result is a self-contained HTML5 file located at [file:product/docs/build/asciidoc/html5/CodekvastUserManual.html]()
