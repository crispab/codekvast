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

## How To Kick The Tyres

The following procedure will download and start two different versions of Jenkins and launch them in Tomcat 7, with Codekvast Collector attached.
It will also build and start Codekvast Daemon and Codekvast Warehouse.

1. Install **JDK 8** (OpenJDK or Oracle are fine.) 

1. Install [Docker Engine 1.10.3+](https://docs.docker.com/engine/installation/) and [Docker Compose 1.6.2+](https://docs.docker.com/compose/install/)

1. Open a terminal window

1. Do `git clone https://github.com/crispab/codekvast.git && cd codekvast`

1. Open 3 more terminal windows or tabs with `codekvast` as working directory.

1. In terminal window #1 do `./gradlew :sample:jenkins1:run`

    This will download and start Jenkins inside Tomcat with Codekvast Collector attached.
    
    You can access Jenkins #1 at http://localhost:8081/jenkins

    _NOTE:_ The download of jenkins.war could take some time. Be patient!
    
1. In terminal window #2 do `./gradlew :sample:jenkins2:run`
   
    Downloads and starts another version of Jenkins inside another Tomcat also with Codekvast Collector attached.
    
    You can access Jenkins #2 at http://localhost:8082/jenkins
    
1. In terminal window #3 do `./gradlew :product:warehouse:distDocker`
 
    This will build a local Docker image for Codekvast Warehouse from the sources.
    
1. In terminal window #3 do `cd product/warehouse`

1. In terminal window #3 do `docker-compose up -d`

    This will launch two Docker containers: **warehouse_db_1** (MariaDB) and **warehouse_app_1** (the Codekvast Warehouse app).
    
    The warehouse app is configured to look for zip files in /tmp/codekvast/.warehouse and import them into the MariaDB database.
    
    Do `docker-compose port app 8080` and note the port that was allocated to warehouse_app_1 (the number after "0.0.0.0:"). Note this port number.
    
1. In terminal window #4 do `sudo chmod o+rw /tmp/codekvast/.warehouse` or else the Codekvast Daemon cannot create it's zip files there.
    
1. In terminal window #4 do `./gradlew :product:agent:daemon:run`

    This will launch **Codekvast daemon**, that will process output from the collectors attached to the two Jenkins instances.
    
    The daemon will regularly produce zip data files in /tmp/codekvast/.warehouse (where Codekvast Warehouse will find and consume them).
    
1. Open a web browser to 0.0.0.0:&lt;warehouse_app_1-port&gt; (output from the `docker-compose port` command above)

   Play around with the Swagger UI console.

1. In each terminal window press `Ctrl-C`to terminate.

### Pre-built binaries and Docker Compose recipes

Pre-built binaries, a User Manual and Docker Compose files are available for download from [Codekvast at Bintray](https://bintray.com/crisp/codekvast/distributions/view#files)

* codekvast-daemon-x.x.x.zip contains the *Codekvast Collector* and the *Codekvast Daemon*.

* codekvast-warehouse-x.x.x.zip contains *Codekvast Warehouse* as a regular Linux System-V service. Install MariaDB separately.

* codekvast-warehouse.docker-compose.yml is a Docker Compose file for running Codekvast Warehouse and MariaDB as Docker images.

* CodekvastUserManual.html is a complete User Manual for all three components. It contains installation and configuration guides.

* RELEASE-NOTES.md contains release notes.

## Development Guide

If you have read this far, you're probably eager to do some Codekvast development. Welcome!

### Technology Stack

The following stack is used when developing Codekvast (in alphabetical order):

1. Angular2
1. AspectJ (in Load-Time Weaving mode)
1. Docker 1.10.3+ and Docker Compose 1.6.2+ (For running MariaDB and Codekvast Warehouse)
1. Github
1. Gradle 
1. H2 database (disk persistent data, server embedded in Codekvast Daemon)
1. Java 8
1. Lombok
1. MariaDB 10+ (Codekvast Warehouse)
1. Node Package Manager (npm)
1. Spring Boot
1. TypeScript

### Directory structure

The product itself lives under `product/`.

Sample projects to use when testing Codekvast lives under `sample/`.

Development tools live under `tools/`.

### Development environment

#### JDK

Java 8  is required. OpenJDK is recommended.

Use the following command to install OpenJDK 8 (Ubuntu, Debian):

    sudo apt-get install openjdk-8-jdk

#### TypeScript

The Codekvast Warehouse web UI is developed with npm, TypeScript and Angular2.

npm is used for managing the frontend development environment.
 
Use the following command to install the Node Package Manager (npm), which is used to manage all JavaScript-related stuff (Ubuntu, Debian):

    sudo apt-get install nodejs npm
    
#### Docker Engine & Docker Compose

Docker Engine 1.10 or later and Docker Compose 1.6 or later is required for Codekvast Warehouse.

Install [Docker Engine 1.10.3+](https://docs.docker.com/engine/installation/) and [Docker Compose 1.6.2+](https://docs.docker.com/compose/install/) using
the official instructions.

#### Build tool

Codekvast uses **Gradle** as build tool. It uses the Gradle Wrapper, `gradlew`, which is checked in at the root of the workspace.
There is the convenience script `tools/src/script/gradle` which simplifies invocation of gradlew. Install that script in your PATH
and use `gradle` instead of `path/to/gradlew`

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

**Intellij Ultimate Edition 16+** is the recommended IDE with the following plugins:

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
