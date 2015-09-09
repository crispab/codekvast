# Codekvast Release Notes

## 0.11.10

1. Prepared for static weaving of the Codekvast Agent

## 0.11.9

1. Upgraded to Spring Boot 1.2.5
1. Upgraded dependencies
1. Rewrote from ajc to @Aspect style

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

1. Added Redhat 6 startscript for codekvast-agent.

## 0.10.0

1. Changed format of JvmData. Breaks compatibility between collector, agent and server.

## 0.9.5

1. Bug fix in codekvast-agent: Method.millisSinceJvmStart was not correct.
1. Implemented server-side support for application statistics.

## 0.9.4

1. codekvast-server now requires JDK 8.

## 0.9.3

1. Java compiler for 1.6 & 1.7 uses correct bootclasspath.

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
