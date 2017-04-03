# Development Guide

## Technology Stack

The following stack is used when developing Codekvast (in alphabetical order):

1. Angular 4
1. AspectJ (in Load-Time Weaving mode)
1. Docker 1.10.3+ and Docker Compose 1.6.2+ (For running MariaDB and Codekvast Warehouse)
1. Github
1. Gradle 
1. H2 database (disk persistent data, server embedded in Codekvast Daemon)
1. Inkscape (SVG graphics)
1. Java 8
1. Lombok
1. MariaDB 10+ (Codekvast Warehouse)
1. NodeJS
1. Node Package Manager (npm)
1. PhantomJS
1. Spring Boot
1. TypeScript
1. Webpack

## Directory structure

The product itself lives under `product/`.

Sample projects to use when testing Codekvast lives under `sample/`.

GitHub pages (i.e., http://codekvast.crisp.se) lives under the Git branch `gh-pages`.

Development tools live under `tools/`.

## Development environment

There is a Bash script that prepares the development environment.

It works for Ubuntu, and is called `tools/prepare-workstation/run.sh`.
It uses Ansible for setting up the workstation so that it works for Codekvast.

If you run some other OS or prefer to do it by hand, here are the requirements:

### JDK and Node.js

Java 8 is required. OpenJDK is recommended.

Node.js 6, NPM 3.10+ and PhantomJS are required.

Use the following command to install OpenJDK 8, Node.js, npm and PhantomJS (Ubuntu, Debian):

    curl -sL https://deb.nodesource.com/setup_6.x | sudo -E bash -
    sudo apt-get install openjdk-8-jdk openjdk-8-doc openjdk-8-source nodejs
    sudo npm install -g phantomjs-prebuilt

You also must define the environment variable `PHANTOMJS_BIN` to point to the phantomjs executable.
(This is due to a bug in the karma-phantomjs-launcher, which does not use PATH.)

Put this into your `/etc/profile.d/phantomjs.sh` or your `$HOME/.profile` or similar:

    export PHANTOMJS_BIN=$(which phantomjs)
    
### TypeScript

The Codekvast Warehouse web UI is developed with TypeScript and Angular 4. Twitter Bootstrap is used as CSS framework.

npm is used for managing the frontend development environment. Webpack is used as frontend bundler.
    
### Docker Engine & Docker Compose

Docker Engine 1.10 or later and Docker Compose 1.6 or later is required for Codekvast Warehouse.

Install [Docker Engine 1.10.3+](https://docs.docker.com/engine/installation/) and [Docker Compose 1.6.2+](https://docs.docker.com/compose/install/) using
the official instructions.

### Inkscape

Graphics including the Codekvast logo is crafted in SVG format, and exported to PNG in various variants and sizes.
Inkscape is an excellent, free and cross-platform SVG editor.

### Build tool

Codekvast uses **Gradle** as build tool. It uses the Gradle Wrapper, `gradlew`, which is checked in at the root of the workspace.
There is the convenience script `tools/src/script/gradle` which simplifies invocation of gradlew. Install that script in your PATH
(e.g., /usr/local/bin) and use `gradle` instead of `path/to/gradlew`

## Continuous Integration

Codekvast is built by Jenkins at http://jenkins.crisp.se on every push, to all branches.

The pipeline is defined by `Jenkinsfile`.

To access http://jenkins.crisp.se you need to be either a Member or an Outside collaborator of https://github.com/orgs/crispab/people.

## Software publishing
Codekvast binaries are published to Bintray and to Docker Hub.

You execute the publishing to both Bintray and Docker Hub by executing `tools/ship-it.sh` in the root of the project.

Preconditions:

1. Clean workspace (no work in progress).
1. On the master branch.
1. Synced with origin (pushed and pulled).
1. Bintray credentials either in environment variables `BINTRAY_USER` and `BINTRAY_KEY` or as values in in  `~/.gradle/gradle.properties`: 
    
    `bintrayUser=my-bintray-user`
    
    `bintrayKey=my-bintray-key`
    
1. `my-bintray-user` must be member of the Crisp organisation at Bintray.
1. Logged in to Docker Hub and member of the crisp organisation.

### IDE

**Intellij Ultimate Edition 2017+** is the recommended IDE with the following plugins:

1. **Lombok Support** (required)
1. Angular 2 TypeScript Live Templates (optional)
1. JavaScript Support (optional)
1. Karma (optional)
1. Git (optional)
1. Github (optional)
1. Docker (optional)

Do like this to open Codekvast in Intellij the first time:

1. File -> New -> Project from Existing Sources...
1. Navigate to the project root
1. Import project from external model...
1. Select Gradle
1. Click Next
1. Accept the defaults (use the project's Gradle wrapper)
1. Click Finish

After the import, some settings must be changed:

1. File > Settings...
1. Build, Execution, Deployment > Compiler > Annotation Processing
1. Check **Enable annotation processing**
1. Click OK

## Code Style

### Java

The general editor config for IDEA is stored in `.editorconfig`.
If you use some other IDE, please make sure to format the code in format as close to this as possible.

Most important rules:

1. **INDENT WITH SPACES**!
1. Indentation: 4 spaces
1. Line length: 140
1. Charset: UTF-8

### TypeScript

The formatting of TypeScript is described and enforced by tslint.js files in the projects that use TypeScript.
IDEA will automatically pick up and apply these settings when found.
