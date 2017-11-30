# Development Guide

## Technology Stack

The following stack is used when developing Codekvast (in alphabetical order):

1. Angular 4
1. AspectJ (in Load-Time Weaving mode)
1. Docker 1.10.3+ (For running MariaDB)
1. Github
1. git-crypt
1. Gradle 
1. Inkscape (SVG graphics)
1. Java 8
1. Kotlin
1. Lombok
1. MariaDB 10+ (Codekvast Dashboard)
1. NodeJS
1. Node Package Manager (npm)
1. PhantomJS
1. Spring Boot
1. TypeScript
1. Webpack
1. Yarn

## Directory structure

The product itself lives under `product/`.

Sample projects to use when testing Codekvast lives under `sample/`.

Development tools live under `tools/`.

Provisioning scripts live under `provisioning/`.

*NOTE: the provisioning/ tree is encrypted with git-crypt since it stores sensible data like cloud provider credentials!*

Authorized developers are enabled to unlock the repo by adding their public GPG keys.
See `man git-crypt`, `git-crypt help add-gpg-user` and `git-crypt help unlock`.

## Web site

Web pages (i.e., http://www.codekvast.io) lives in the Git repo `https://github.com/crispab/codekvast-site`.

## Development environment

There is a Bash script that prepares the development environment.

It works for Ubuntu, and is called `tools/prepare-workstation/run.sh`.
It uses Ansible for setting up the workstation so that it works for Codekvast.

If you run some other OS or prefer to do it by hand, here are the requirements:

### git-crypt

The `provisioning/` directory is encrypted with `git-crypt` and GPG public keys.

You must be added as trusted developer by `git-crypt add-gpg-user` to access the provisioning directory.

### JDK and Node.js

Java 8 is required. OpenJDK is recommended.

Node.js 6, NPM 3.10+ and PhantomJS are required.

git-crypt is required for deploying to the cloud.

Use the following command to install OpenJDK 8, git-crypt, Node.js, npm, PhantomJS and Yarn (Ubuntu, Debian):

    curl -sL https://deb.nodesource.com/setup_6.x | sudo -E bash -
    sudo apt install openjdk-8-jdk openjdk-8-doc openjdk-8-source git-crypt nodejs
    sudo npm install -g phantomjs-prebuilt
    curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | sudo apt-key add -
    echo "deb https://dl.yarnpkg.com/debian/ stable main" | sudo tee /etc/apt/sources.list.d/yarn.list
    sudo apt-get update && sudo apt-get install yarn

You also must define the environment variable `PHANTOMJS_BIN` to point to the phantomjs executable.
(This is due to a bug in the karma-phantomjs-launcher, which does not use PATH.)

Put this into your `/etc/profile.d/phantomjs.sh` or your `$HOME/.profile` or similar:

    export PHANTOMJS_BIN=$(which phantomjs)
    
### TypeScript

The Codekvast Dashboard web UI is developed with TypeScript and Angular 4. Twitter Bootstrap is used as CSS framework.

npm and yarn are used for managing the frontend development environment. Webpack is used as frontend bundler.
    
### Docker Engine

Docker Engine 1.10 or later is required for Codekvast Dashboard development.

Install [Docker Engine 1.10.3+](https://docs.docker.com/engine/installation/) using
the official instructions.

### Inkscape

Graphics including the Codekvast logo is crafted in SVG format, and exported to PNG in various variants and sizes.
Inkscape is an excellent, free and cross-platform SVG editor.

### Build tool

Codekvast uses **Gradle** as build tool. It uses the Gradle Wrapper, `gradlew`, which is checked in at the root of the workspace.
There is the convenience script `tools/src/script/gradle` which simplifies invocation of gradlew. Install that script in your PATH
(e.g., `/usr/local/bin`) and simply use `gradle` instead of `path/to/gradlew`

## Continuous Integration

Codekvast is built by Jenkins at http://jenkins.crisp.se on every push, to all branches.

The pipeline is defined by `Jenkinsfile`.

To access http://jenkins.crisp.se you need to be either a Member or an Outside collaborator of https://github.com/orgs/crispab/people.

## Software publishing
Codekvast binaries are published to Bintray.

You execute the publishing to Bintray by executing `tools/ship-it.sh` in the root of the project.

Preconditions:

1. Clean workspace (no work in progress).
1. On the master branch.
1. Synced with origin (pushed and pulled).
1. Bintray credentials either in environment variables `BINTRAY_USER` and `BINTRAY_KEY` or as values in in  `~/.gradle/gradle.properties`: 
    
    `bintrayUser=my-bintray-user`
    
    `bintrayKey=my-bintray-key`
    
1. `my-bintray-user` must be member of the Crisp organisation at Bintray.

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

## How to do semi-manual end-to-end tests

All of the non-trivial code is covered with unit tests.

Some tricky integrations are covered by proper integration tests where the external part is executing in Docker containers managed by the tests. 

There is also a smoke test that launches MariaDB and Codekvast Dashboard, and executes some Web Driver tests.
This is just a smoke test though.

To assist manual e2e tests, there is a number of sample apps that are managed by Gradle. They are configured to start with the latest
Codekvast collector attached.

### How to demo

The following procedure can be used for demo purposes and also when doing development with live data flowing.

1. Launch 4 terminal windows
1. In terminal #1 do `./gradlew :product:dashboard:bootRun`.
This will start Codekvast Dashboard that will consume the data files uploaded by the instrumented apps.
1. In terminal #2 do `./gradlew :sample:jenkins1:run`. This will download and start one version of Jenkins with Codekvast attached.
1. In terminal #3 do `./gradlew :sample:jenkins2:run`. This will download and start another version of Jenkins with Codekvast attached.
1. In terminal #4 do `./gradlew :sample:sample-gradle-application:run`. This will launch the short-lived `sample.app.SampleApp` with Codekvast attached. The SampleApp is handy when
you want to correlate source code to the data that is collected by Codekvast.
1. Open a web browser at http://localhost:8080. It will show the dashboard web interface wher e you can inspect the collected data.

### How to do rapid development of the dashboard web app

In addition to the above do this:

1. Launch a terminal window
1. `cd product/dashboard/src/webapp`
1. `npm start`. It will start an embedded web server on port 8088.
It reloads changes to the webapp automatically. It will also refresh the browser automatically.
1. Open the web browser at http://localhost:8088

### Canned REST responses for off-line dashboard webapp development

When running the dashboard webapp from `npm start` there is a number of canned REST responses available.
This makes it possible to develop the webapp with nothing else than `npm start` running.

The canned responses are really handy when doing frontend development, where live data is strictly not necessary.
 
#### End-point /webapp/v1/methods

In the Methods page, the canned response is delivered from disk by searching for the signature `-----` (five dashes).
 
#### Updating the canned responses

Canned responses has to be re-captured every time the dashboard REST API has been changed.

The canned response for `/webapp/v1/methods` is captured by executing

    curl -X GET --header 'Accept: application/json' 'http://localhost:8080/webapp/v1/methods?signature=%25&maxResults=100'|jq . > product/dashboard/src/webapp/src/app/test/canned/v1/MethodData.json
    git add product/dashboard/src/webapp/src/app/test/canned/v1/MethodData.json
    
from the root directory while `./gradlew :product:dashboard:bootRun` is running.
When doing the capture, make sure that data from the three above mentioned sample apps is stored in the dashboard.

(The JSON response is piped through `jq .` to make it more pretty for the human eye.)

## File watch limits

On some Linux distros, both IntelliJ IDEA and the Node.js uses the system service `inotify` to watch directories for changed files.

If the limit is to low, `npm start` will fail.

If you happen to use Ubuntu, here is the remedy:

Create the file `/etc/sysctl.d/60-jetbrains.conf` with the following content:

    # Set inotify watch limit high enough for IntelliJ IDEA (PhpStorm, PyCharm, RubyMine, WebStorm).
    # Create this file as /etc/sysctl.d/60-jetbrains.conf (Debian, Ubuntu), and
    # run `sudo sysctl --system` or reboot.
    # Source: https://confluence.jetbrains.com/display/IDEADEV/Inotify+Watches+Limit
    # 
    # More information resources:
    # man inotify  # manpage
    # man sysctl.conf  # manpage
    # cat /proc/sys/fs/inotify/max_user_watches  # print current value in use
    
    fs.inotify.max_user_watches = 524288
    
Then do `sudo sysctl --system` to activate the changes.
