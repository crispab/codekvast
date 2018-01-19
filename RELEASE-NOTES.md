# Codekvast Release Notes

## 0.22.4
1. Upgraded Java dependencies: jslack 1.0.20, wiremock 2.13.0, phantomjsdriver 1.4.4
1. Upgraded JavaScript dependencies: angular 5.1.3, zone.js 0.8.19, rxjs 5.5.6 (+ some test deps)
1. Replaced PhantomJS with Chrome headless for web tests.
1. Dashboard: passes JWT token with cookie instead of header.

## 0.22.3
1. Dashboard: the method details table to right is now optional.
1. Dashboard: method details does not show hosts if more than 10.
1. Java Agent: downgraded third-party dependencies so that it works in Java 7 again.

## 0.22.2
1. Dashboard: added more search filters in the Methods page

## 0.22.1
1. Upgraded to Angular 5.1.1, Typescript 2.6.1, aspectjweaver 1.8.12
1. Upgraded to Spring Boot 1.5.9
1. Upgraded to Gradle 4.4
1. Fixed bug caused by renaming classes in agent-model

## 0.22.0
1. Moved codebase analysis from agent to server. Now all discovered methods are uploaded to the server, no matter the name pattern.

## 0.21.6
1. Upgraded to Angular 4.4.4, Typescript 3.5.3.
1. Upgraded to aspectjweaver 1.8.11.
1. Upgraded to Spring Boot 1.5.8
1. Renamed Codekvast Warehouse to Codekvast Dashboard.

## 0.21.5
1. Added Boot-Class-Path to codekvast-agent's MANIFEST.MF (eliminates the need for -Xbootclasspath/a:codekvast-agent-x.x.jar)
1. Improved handling of communication failures in Codekvast Warehouse.
1. Added support for trial periods.
1. Increased maxMethods to 25.000 in TEST price plan.
1. Bug fix in Warehouse when importing incomplete methods.
1. Bug fix in Warehouse: ga('send', 'pageview') after navigation events.
1. Upgraded to Spring Boot 1.5.7
1. Redesigned Warehouse /home; made it more compact.
1. Added Slack integration in CustomerServiceImpl.

## 0.21.4
1. Implemented Status page in Codekvast Warehouse.
1. Upgraded to Angular 4.3.2.
1. Upgraded to Spring Boot 1.5.6, Jackson 2.9.0.
1. Upgraded to Gradle 4.0.
1. Upgraded Gradle plugins.
1. Codekvast Agent avoids publishing empty invocation sets.

## 0.21.3
1. Upgraded to Angular 4.2.4 and Webpack 3.0.0
1. Added column users.lastActivityAt
1. Now redirects to /logged-out after webapp session has expired
1. Now redirects to /not-logged-in when an unauthenticated user clicks a link that requires login
1. Bug fix: now deprovision works.
1. Bug fix: the first invocation data is now uploaded very soon after start

## 0.21.2
1. Added support for Spring Boot executable jars
1. Upgraded to Angular 4.2.2

## 0.21.1
1. Removed support for building a Docker image for Codekvast Warehouse. 
1. Adjusted logging to make it easier do document what to look for when starting the agent.

## 0.21.0
1. Implemented price plan enforcements.
1. **NOTE:** Backwards incompatible API change in AgentController.

## 0.20.3
1. Fixed a OkHttp Response leak in java-agent

## 0.20.2
1. Made Heroku add-on shareable across apps.

## 0.20.1
1. Removed classifier 'all' from codekvast-agent-x.x.x.jar. It used to be named codekvast-agent-x.x.x-all.jar

## 0.20.0
1. Merged codekvast-agent.jar and aspectjweaver.jar

## 0.19.0
1. Warehouse: Added invocation statuses to method summary and method details page.
1. Warehouse: Added settings editor.
1. Warehouse: Implemented detection of inconsistent collector config.
1. Warehouse: Added a landing page for feature voting, to make it simpler to use Google Analytics.
1. Adopted Yarn instead of npm for installing Node dependencies.
1. Eliminated the Codekvast Daemon.
1. Renamed all packages from `se.crisp.codekvast` to `io.codekvast`.
1. Bumped minimum Java version for the agent to Java 7.
1. Renamed /api/v1/... endpoints to /webapp/v1/...
1. Agent zip distribution build.
1. Added provisioning/ containing Ansible playbooks for setting up an AWS stack per customer/environment.

## 0.18.6
1. Restructured and cleaned up README.md
1. Added /home to Warehouse.
1. Improved Warehouse's /methods. Now the scrollbars work as expected.
1. Implemented Warehouse's method details page. Ugly, but working.

## 0.18.5
1. Implemented a simple web interface to Warehouse.
1. Upgraded to Gradle 3.4.
1. Upgraded to Angular 4, Typescript 2.
1. Upgraded to Spring Boot 1.5.
1. Improved codekvast-warehouse.sh to use environment variables.
1. Switched to java:8-jre-alpine for codekvast-warehouse Docker image (reduced image size from 332 MB to 129 MB).
1. Replaced Serenity with Geb + Spock.

## 0.18.4

1. Warehouse: Switched from embedded Tomcat to Undertow.
1. Warehouse: Switched from tomcat-jdbc to HikariCp.
1. Warehouse: Runs Karma tests against PhantomJS, Firefox and Chrome unless headless environment.
1. Bug fixes in build scripts.
1. Upgraded to Gradle 2.14.

## 0.18.3

1. Made the Ansible playbook for codekvast-daemon work with Ansible 1.5.4
1. Implemented rudimentary web UI for showing warehouse data.
1. Renamed codekvast-warehouse.docker-compose.yml to codekvast-warehouse.sh
1. Switched to building frontend with Webpack

## 0.18.2

1. New download URLs in Bintray
1. Added support for unit testing TypeScript with Karma.

## 0.18.1

1. Added support for developing Codekvast Warehouse web UI with Angular2 and TypeScript.
1. Added a Serenity BDD- and Docker-based function test for Codekvast Warehouse.
1. Added proper integration test for Codekvast Collector.
1. Now Codekvast also tracks constructors.

## 0.18.0

1. Refactored daemon; renamed many classes and packages.
1. Made all @Scheduled parameters configurable.
1. Fixed auto-commit bug in warehouse zip file import.
1. Added Docker-based integration test for ScpFileUploader.
1. Added ready-checker support in testsupport's DockerContainer.
1. Warehouse integration tests now use MariaDB in a Docker container.
1. Improved test coverage.
1. Implemented simple REST API in Codekvast Warehouse: /api/v1/methods/
1. Added Swagger UI in Codekvast Warehouse.

## 0.17.5

1. Fixed bug in warehouse; now it can insert into import_file_info.
1. Upgraded dependencies.

## 0.17.4

1. Upgraded to Gradle 2.12
1. Upgraded to Spring Boot 1.3.3.RELEASE

## 0.17.3

1. Upgraded bintrayPlugin, added github info to Bintray metadata.

## 0.17.2

1. Fixed ignored Flyway migrations (caused by misspelled file names).
1. Tomcat and Jenkins now downloaded from their archive sites.
1. Moved the playground stuff to the branch playground and removed it from master.
1. Added Github Pages at http://crispab.github.io/codekvast/
1. Now uploads codekvast-collector-x.x.x-*.jar to bintray

## 0.17.1

1. Upgraded to Gradle 2.11
1. Upgraded some dependencies: aspectj 1.8.8, h2database 1.4.191, opencsv 3.7, mariadb-java-client 1.3.5

## 0.17.0

1. Now the warehouse contains *all* methods, even those that are excluded from collection. The confidence column tells why a certain
    signature was excluded.
    
## 0.16.1

1. CollectorConfig now backward compatible with 0.15.6.
1. Collector now excludes synthetic methods.

## 0.16.0

1. Codekvast Daemon is now distributed as a Spring Boot executable jar (was: a Gradle application).
1. Renamed CollectorConfig.packagePrefixes to CollectorConfig.packages.
1. Renamed CollectorConfig.excludePackagePrefixes to CollectorConfig.excludePackages.
 
## 0.15.6

1. Simplified config of SCP upload.
1. Added fail-fast validation of DaemonConfig.uploadToHost. Now the daemon logs an ERROR if it fails to touch a file in the upload target.
1. Changed default collector and daemon dataPath to /tmp/codekvast/.collector.

## 0.15.5

1. Added Overview and Kick The Tyres sections to README.md.

## 0.15.4

1. Corrected codekvast-daemon installation guide.
1. Enabled color in console logging.
1. Simplified default config.

## 0.15.3

1. Upgraded to Spring Boot 1.3.0
1. Refactored to use sprint-boot-starter-logging instead of home-rolled LoggingConfig.

## 0.15.2

1. Default methodVisibility is now protected.

## 0.15.1

1. Tripled the performance of codekvast-collector.

## 0.15.0

1. Added CollectorConfig.excludePackagePrefixes that excludes (time-critical) code from weaving

## 0.14.6

1. Simplified logging config. -Dcodekvast.logPath has highest precedence.
1. The codekvast-database container now stores the MariaDB data in the host's /var/lib/codekvast-database.
1. The codekvast-warehouse container logs in /var/log/codekvast.
1. Made it possible to exec mysql in a running codekvast-database container.

## 0.14.5

1. Switched to a smaller Docker base image for codekvast-warehouse
1. Made codekvast-warehouse exit immediately if Docker container linking is misconfigured.
1. `./gradlew :product:warehouse:build` now also builds distDocker and tags the image with "latest" and "$version-$gitHash"

## 0.14.4

1. Added docker-compose.sh

## 0.14.3

1. Dockerized codekvast-warehouse.

## 0.14.2

1. Added MIT license header to all source files.
1. Fixed NPE in CollectorConfigLocator.

## 0.14.1

1. Added support for SCP upload of daemon export files.

## 0.14.0

1. Moved playground server and daemon parts to new playground folder.

## 0.13.8

1. Corrected bug in CodeBase.readByteCodePatternsFrom()

## 0.13.7

1. Corrected warehouse's application.properties: added missing "codekvast.importPathPollIntervalSeconds = 30"

## 0.13.6

1. Corrected the names of the generated start scripts for daemon and warehouse.

## 0.13.5

1. codekvast-warehouse-x.x.x.zip now uploads to bintray.

## 0.13.4

1. Renamed warehouse table file_meta_info to import_file_info.

## 0.13.3

1. Applied some Intellij inspections.

## 0.13.2

1. Changed Central warehouse schema: replaced jvms.jvmDataJson with discrete fields.
1. Added jvms.environment to central warehouse schema.
1. Improved import performance.
 
## 0.13.1

1. Improved logging.
1. Made deletion of imported zip file optional.

## 0.13.0

1. Created module product/warehouse, which has a dependency on mariadb-server. See README for installation
instructions.

## 0.12.4

1. Renamed modules and packages below product/agent

## 0.12.3

1. Implemented daemon data export

## 0.12.2

1. Now all except codekvast-collector requires JDK 8
1. Upgraded angular-ui-bootstrap.js to 0.14.3

## 0.12.1

1. Upgraded to Spring Boot 1.2.7
1. Upgraded to Gradle 2.8
1. Now requires JDK 1.7 for codekvast-daemon
1. Improved logging

## 0.12.0

1. Renamed codekvast-agent to codekvast-daemon

## 0.11.11

1. Preparations for making it possible to use Codekvast for Java Web Started applications (incubating).
1. Upgraded to Gradle 2.7

## 0.11.10

1. Enabled static weaving of the Codekvast Daemon
1. Codekvast Collector is now possible to initialize from a non-javaagent context.

## 0.11.9

1. Upgraded to Spring Boot 1.2.5
1. Upgraded dependencies
1. Rewrote from ajc to @Aspect style (so that all modules can be compiled with a plain Java compiler)

## 0.11.8

1. Implemented Delete collectors

## 0.11.7

1. Upgraded dependencies

## 0.11.6

1. Bug fix in Agent: now it handles more Guice-manipulated method names

## 0.11.5

1. Capacity: Application statistics calculations are delayed by 5s (configurable).
   Requests for the same app during the delay are ignored.
1. Capacity: Now Save Settings only recalculates actually changed applications.
1. Bug: Clicking I icons in table header in collectors.html resorted the column
1. Improvement: Now logging also shows the thread name
1. Improvement: Better logging when backing up and restoring database.
1. Improvement: Logs pending Flyway migrations at app startup.
1. Improvement: Avoids backing up database immediately after restoring from restore-me.zip
1. Bug: Redundant statistics recalculations for same app in different JVMs.
1. Improvement: added timing logging to report generator.

## 0.11.4

1. Replaced tooltips with popovers in application-statistics.html.
1. Connected code-usage-report.html with live version data.
1. Added filtering and sorting to collectors.html
1. Now the code-usage-report form behaves correctly. Still no report though.
1. Worked on code-usage-report. Still random fake data though.

## 0.11.3

1. Added filter and column sorting to application-usage-statistics.html

## 0.11.2

1. UI and database schema change; renamed application_statistics.num_probably_dead to num_possibly_dead. Database is migrated by Flyway.

## 0.11.1

1. Simplified logback.xml
1. Improved collectors.html so it now shows warning color for dead collectors.

## 0.11.0

1. Keeps track of agent clock skew. Breaks compatibility of agent-API.

## 0.10.3

1. Server now tracks uptime.

## 0.10.2

1. Added Data Age to collectors.html.
1. Added version info to codekvast-server web page footer.
1. Added progress bar to statistics Usage Cycle column.

## 0.10.1

1. Added Redhat 6 start script for codekvast-agent.

## 0.10.0

1. Changed format of JvmData. Breaks compatibility between collector, agent and server.

## 0.9.5

1. Bug fix in codekvast-agent: Method.millisSinceJvmStart was not correct.
1. Implemented server-side support for application statistics.

## 0.9.4

1. codekvast-server now requires JDK 8.

## 0.9.3

1. Java compiler for 1.6 & 1.7 uses correct boot classpath.

## 0.9.2

1. Now excludes trivial methods from tracking (equals(), hashCode(), toString(), compareTo(), getters, setters).

## 0.9.1

1. Added a Gentoo start script for codekvast-server and codekvast-agent.

## 0.9.0

1. Improved the installation procedures in CodekvastUserManual.
1. Now CodekvastUserManual.html is self-contained. No more external images.
1. codekvast-agent now runs on Java 6.

## 0.8.18

1. Improved the server web interface.
1. codekvast-agent now uses a private H2 database for storing not yet uploaded data.

## 0.8.17

1. Added /etc/init.d scripts for agent and server.
1. Documented installation of agent and server (work in progress).
