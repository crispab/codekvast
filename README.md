# README for Codekvast

## Directory structure

The product itself lives under `product/`.

Sample projects to use when testing Codekvast lives under `sample/`.

Development tools live under `tools/`.

Some legacy code lives under `legacy/`.

## Development environment

### JDK

Java 8 **and** Java 6 is required. OpenJDK is recommended.

### Database

MariaDB v10.0 or later is **required** for Codekvast **Warehouse**.

Use the following command to install MariaDB (Ubuntu, Debian):

    sudo apt-get install mariadb-server
    
Then the following commands must be executed once:
    
    $ sudo mysql
    create database codekvast_warehouse;
    grant all on codekvast_warehouse.* to 'codekvast'@'localhost' identified by 'codekvast';
    Ctrl-D
    
### Build tool

Codekvast uses **Gradle** as build tool. It uses the Gradle Wrapper, `gradlew`, which is checked in at the root of the workspace.
There is the convenience script `tools/src/script/gradle` which simplifies invocation of gradlew. Install that script in your PATH
and use that script instead of `path/to/gradlew`

### Software publishing
Codekvast is published to Bintray. To be able to upload to Bintray you need the following lines in your `~/.gradle/gradle.properties`:

    bintrayUser=my-bintray-user
    bintrayKey=my-bintray-key

You also need to be member of the Crisp organisation in Bintray.

### IDE

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

## How to build the product from the commmand line
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

### Start Codekvast daemon in terminal 3

    gradle :product:agent:daemon:run

This will launch the Codekvast **daemon**, that will process output from the collectors attached to the two Jenkins instances.
The daemon will regularly produce data files in /tmp/codekvast/.export.
Terminate the daemon with `Ctrl-C`.

### Start Codekvast Warehouse in terminal 4

    gradle :product:warehouse:run

This will start Codekvast Warehouse. It will look for the data files produced by codekvast-daemon and import them to the
local MariaDB database **codekvast_warehouse** (username/password: `codekvast/codekvast`).
Terminate with `Ctrl-C`.

Log in to MariaDB with `mysql -ucodekvast -pcodekvast codekvast_warehouse`.

Examine the collected data by means of `SELECT * FROM MethodInvocations1`

## User Manual

A User Manual located in product/docs/src/asciidoc.

The source is in AsciiDoctor format.

It is built by doing `./gradlew product:docs:asciidoctor`.

The result is a self-contained HTML5 file located at [file:product/docs/build/asciidoc/html5/CodekvastUserManual.html]()
