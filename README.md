# Codekvast - the Truly Dead Code Detector

## Overview

Codekvast detects **Truly Dead Code** in **Java-applications**.

By *Truly Dead Code* we mean code that is in production, but *has not been used by anyone for a certain period of time*.

Codekvast works by attaching the **Codekvast Collector** to your application.
The collector records the instant when methods belonging to any of your packages are invoked. 

By default Codekvast tracks *public* and *protected* methods. Modern IDEs like IntelliJ IDEA can detect dead code which have *package 
private* or *private* visibility. They can never know what callers there are to public and protected methods though.
 
This is where Codekvast can help. Codekvast *records when your methods are invoked in production*.

The collector periodically sends the recorded data to the **Codekvast Daemon**, which combines the invocation data
with an inventory of *all* methods in your application. (It's the methods that *not* have been invoked recently that
constitutes Truly Dead Code, remember?)
 
The Codekvast Daemon is installed in the same host as your application, since it needs access to your application's binaries
 (jar files) to make the inventory.

The Codekvast Daemon periodically produces a zip file containing both the method inventory and the collected invocation data.

The zip files from all daemons should be transferred to a central **Codekvast Warehouse**, which aggregates the data, and makes
it available to users in a web interface.

The Codekvast Daemon can be configured to automatically upload the zip files to the warehouse by means of SCP. If SCP is not an option,
the zip files must be transferred manually somehow.

By using the Codekvast Warehouse, you can find out whether a certain method, class or package is safe to remove or not.

*Codekvast collects the data. You then combine that with your domain knowledge to make informed decisions about what is truly dead code
that safely could be deleted.*

### Performance

Codekvast Collector is extremely efficient. It adds approximately *15 ns* to each method invocation. If this is unacceptable,
you can exclude certain time critical packages from collection.

The collected data volumes are quite moderate. When tested on a telecom equipment vendors' fairly large network management application,
the local database maintained by Codekvast Daemon occupies less than 200 MB disk space.

The zip file for that case weighs less than 3 MB. 

## License

Codekvast is released under the MIT license.

## Development Status

* The Codekvast Collector is fairly complete and stable.

    Tentative road-map:
    
    1. Mechanism for delivering collected data to the daemon over a TCP socket instead of text files in the local file system. This will
    make it possible to use Codekvast in e.g., Heroku and Google App Engine. 
    
* The Codekvast Daemon is fairly stable.

    Tentative Road-map:

    1. Add a mechanism for pruning the local database once data has been delivered to the warehouse.
    1. Add a mechanism for receiving collected data from the collectors using TCP sockets.
    1. Add other mechanisms than scp for delivering data to the central warehouse.
    1. Make it possible to run as a Docker container.
    1. Make it possible to analyze unexploded WAR and EAR archives.
    1. Add devops stuff. (ping, health checks, JMX, metrics, ...)
    
*NOTE:* the collector and the daemon communicates via the local file system. This means that Codekvast for the moment is unusable in e.g., Heroku and
Google App Engine.

* The Codekvast Warehouse is **very** rudimentary. It aggregates and persists data alright, but the user interface is absent. (For the moment
the only functionality offered is a REST API for getting information about a method).

    Tentative road-map:
    
     1. Add a REST API.
     1. Add a web UI.
     1. Add a mechanism for informing daemons that they safely can prune data.
     1. Add more mechanisms for receiving data from daemons (currently supported: zip files, optionally pushed by scp).  

### Pre-built binaries and Docker Compose recipes

Pre-built binaries, a User Manual and Docker Compose files are available for download from [Codekvast at Bintray](https://bintray.com/crisp/codekvast/distributions/view#files)

* codekvast-daemon-x.x.x.zip contains the *Codekvast Collector* and the *Codekvast Daemon*.

* codekvast-warehouse-x.x.x.zip contains *Codekvast Warehouse* as a regular Linux System-V service. Install MariaDB separately.

* codekvast-warehouse.sh is a Docker Compose script that runs Codekvast Warehouse and MariaDB as Docker images.

* CodekvastUserManual.html is a complete User Manual for all three components. It contains installation and configuration guides.

* RELEASE-NOTES.md contains release notes.

## Development Guide

If you have read this far, you're probably eager to do some Codekvast development. Welcome!

### Technology Stack

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

### Directory structure

The product itself lives under `product/`.

Sample projects to use when testing Codekvast lives under `sample/`.

GitHub pages (i.e., http://codekvast.crisp.se) lives under `docs/`

Development tools live under `tools/`.

### Development environment

There is a Bash script that prepares the development environment.

It works for Ubuntu, and is called `tools/prepare-workstation/run.sh`.
It uses Ansible for setting up the workstation so that it works for Codekvast.

If you run some other OS or prefer to do it by hand, here are the requirements:

#### JDK and Node.js

Java 8 is required. OpenJDK is recommended.

Node.js 6, NPM 3.10+ and PhantomJS are required.

Use the following command to install OpenJDK 8, Node.js, npm and PhantomJS (Ubuntu, Debian):

    curl -sL https://deb.nodesource.com/setup_6.x | sudo -E bash -
    sudo apt-get install openjdk-8-jdk openjdk-8-doc openjdk-8-source nodejs
    sudo npm install -g phantomjs-prebuilt

You also must define the environment variable `PHANTOMJS_BIN` to point to the phantomjs executable.
Put this into your `/etc/profile.d/phantomjs.sh` or your `$HOME/.profile` or similar:

    export PHANTOMJS_BIN=$(which phantomjs)
    
#### TypeScript

The Codekvast Warehouse web UI is developed with TypeScript and Angular 4. Twitter Bootstrap is used as design framework.

npm is used for managing the frontend development environment. Webpack is used as frontend bundler.
    
#### Docker Engine & Docker Compose

Docker Engine 1.10 or later and Docker Compose 1.6 or later is required for Codekvast Warehouse.

Install [Docker Engine 1.10.3+](https://docs.docker.com/engine/installation/) and [Docker Compose 1.6.2+](https://docs.docker.com/compose/install/) using
the official instructions.

#### Inkscape

Graphics including the Codekvast logo is crafted in SVG format, and exported to PNG in various variants and sizes.
Inkscape is an excellent, free and cross-platform SVG editor.

#### Build tool

Codekvast uses **Gradle** as build tool. It uses the Gradle Wrapper, `gradlew`, which is checked in at the root of the workspace.
There is the convenience script `tools/src/script/gradle` which simplifies invocation of gradlew. Install that script in your PATH
(e.g., /usr/local/bin) and use `gradle` instead of `path/to/gradlew`

#### Software publishing
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

#### IDE

**Intellij Ultimate Edition 2016+** is the recommended IDE with the following plugins:

1. **Lombok Support** (required)
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
