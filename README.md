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
with an *inventory* of all methods in your application.
 
The Codekvast Daemon is installed in the same host as your application, since it needs access to your application's binaries
 (jar files) to make the inventory.

The Codekvast Daemon periodically produces a zip file containing both the method inventory and the collected invocation data.

The zip files from all daemons should be transferred to a central **Codekvast Warehouse**, which aggregates the data, and makes
it available to users in a web interface.

The Codekvast Daemon can be configured to automatically upload the zip files to the warehouse by means of SCP. If SCP is not an option,
the zip files must be transferred manually somehow.

By using the Codekvast Warehouse, you can find out whether a certain method, class or package is safe to remove or not.

*Codekvast collects the data, you combine that with your domain knowledge to make informed decisions about what is truly dead code
that safely could be deleted.*

### Performance

Codekvast Collector is extremely efficient. It adds approximately *15 ns* to each method invocation. If this is unacceptable,
you can exclude certain time critical packages from collection.

The collected data volumes are quite moderate. When tested on a telecom equipment vendors' fairly large network management application,
the local database maintained by Codekvast Daemon occupies less than 10 MB disk space.

The zip file for that case weighs less than 1 MB. 

## License

Codekvast is released under the MIT license.

## Development Status

* The Codekvast Collector is fairly complete and stable.

* The Codekvast Daemon is fairly stable.

* The Codekvast Warehouse is quite rudimentary. It aggregates and persists data alright, but the user interface is quite limited. (For the moment
the only functionality offered is a view in the database schema).

*NOTE:* the collector and the daemon communicates via the local file system. This means that Codekvast for the moment is unusable in e.g., Heroku and
Google App Engine.

## How To Kick The Tyres

The following procedure will download and start two different versions of Jenkins and launch them in Tomcat 7, with Codekvast Collector attached.
It will also build and start Codekvast Daemon and Codekvast Warehouse.

1. Install **JDK 8** (OpenJDK or Oracle are fine.) 

1. Install [Docker Compose](https://docs.docker.com/compose/install/)

1. Open a terminal window

1. Do `git clone https://github.com/crispab/codekvast.git && cd codekvast`

1. Open 4 more terminal windows with `codekvast` as working directory.

1. In terminal window #1 do `./gradlew :sample:jenkins1:run`

    This will download and start Jenkins inside Tomcat with Codekvast Collector attached.
    
    You can access Jenkins at http://localhost:8081/jenkins
    
    **NOTE:** Should the command fail, it is probably due to an old version of Tomcat in sample/jenkins1/build.gradle.
     If this happens, edit build.gradle, step ext.tomcatVersion and try again.
    
1. In terminal window #2 do `./gradlew :sample:jenkins2:run`
   
    Downloads and starts another version of Jenkins inside another Tomcat also with Codekvast Collector attached.
    
    You can access it at http://localhost:8082/jenkins
    
1. In terminal window #3 do `./gradlew :product:warehouse:distDocker`.
 
    This will build a local Docker image for Codekvast Warehouse from the sources.
    
1. In terminal window #3 do `docker-compose -f product/warehouse/docker-compose.yml up`

    This will launch two Docker containers: **codekvast-database** (MariaDB) and **codekvast-warehouse** (the Codekvast Warehouse app).
    
    The warehouse app is configured to look for zip files in /tmp/codekvast/import and import them into the MariaDB database.
    
1. In terminal window #4 do `sudo chmod o+rw /tmp/codekvast/import` or else the Codekvast Daemon cannot create it's zip files there.
    
1. In terminal window #4 do `./gradlew :product:agent:daemon:run`

    This will launch **Codekvast daemon**, that will process output from the collectors attached to the two Jenkins instances.
    
    The daemon will regularly produce zip data files in /tmp/codekvast/import (where Codekvast Warehouse will find and consume them).
    
1. In terminal window #5 do `docker exec -ti codekvast-database mysql -ucodekvast -pcodekvast codekvast_warehouse`

    Examine the collected data by means of `SELECT * FROM MethodInvocations1;`
    
    Examine the view with `DESCRIBE MethodInvocations1;`

1. In each terminal window press `Ctrl-C`to terminate.

### Pre-built binaries and Docker Compose recipes

Pre-built binaries, a User Manual and Docker Compose files are available for download from [Codekvast at Bintray](https://bintray.com/crisp/codekvast/distributions/view#files)

* codekvast-daemon-x.x.x.zip contains the *Codekvast Collector* and the *Codekvast Daemon*.

* codekvast-warehouse-x.x.x.zip contains *Codekvast Warehouse* as a regular Linux System-V service. Install MariaDB separately.

* docker-compose-x.x.x.yml is a Docker Compose file for running Codekvast Warehouse and MariaDB as Docker images.

* CodekvastUserManual-x.x.x.html is a complete User Manual for all three components. It contains installation and configuration guides.

* RELEASE-NOTES-x.x.x.md contains release notes.

## Development Guide

If you have read this far, you're probably eager to do some Codekvast development. Welcome!

### Technology Stack

The following stack is used when developing Codekvast:

1. Github
1. Java 8 (the collector is built with Java 6)
1. AspectJ (in Load-Time Weaving mode)
1. Spring Boot
1. H2 database (disk persistent, embedded in Codekvast Daemon)
1. MariaDB 10+ (Codekvast Warehouse)
1. Gradle 
1. Docker + Docker Compose (optional mode of running Codekvast Warehouse)

### Directory structure

The product itself lives under `product/`.

Sample projects to use when testing Codekvast lives under `sample/`.

Development tools live under `tools/`.

Some experimental code lives under `playground/`.

### Development environment

#### JDK

Java 8 **and** Java 6 is required. OpenJDK is recommended.

#### Database

MariaDB v10.0 or later is required for Codekvast Warehouse.

Use the following command to install MariaDB (Ubuntu, Debian):

    sudo apt-get install mariadb-server
    
Then the following commands must be executed once:
    
    $ sudo mysql -e "create database codekvast_warehouse; grant all on codekvast_warehouse.* to 'codekvast'@'localhost' identified by 'codekvast';"
    
#### Build tool

Codekvast uses **Gradle** as build tool. It uses the Gradle Wrapper, `gradlew`, which is checked in at the root of the workspace.
There is the convenience script `tools/src/script/gradle` which simplifies invocation of gradlew. Install that script in your PATH
and use that script instead of `path/to/gradlew`

#### Software publishing
Codekvast is published to Bintray. To be able to upload to Bintray you need the following lines in your `~/.gradle/gradle.properties`:

    bintrayUser=my-bintray-user
    bintrayKey=my-bintray-key

You also need to be member of the Crisp organisation in Bintray.

#### IDE

**Intellij Ultimate Edition 14+** is the recommended IDE with the following plugins:

1. **Lombok Support** (required)
1. **Git** (required)
1. Github (optional)
1. AspectJ Support (optional)
1. AngularJS (optional)
1. Karma (optional)

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

Then a couple of module settings must be changed:

1. File > Project Structure

1. Platform Settings > SDKs
     You need **both** a **1.6** SDK **and** a **1.8** SDK.

1. Project Settings > Project
    Project SDK should be **1.8** and
    Project language level should be **8 - Lambdas, type annotations etc**

1. Project Settings > Modules
    The modules **agent-lib** and **collector** shall have
    Language level **6 - @Override in interfaces** and 
    Module SDK: **1.6** (in the Dependencies tab)

