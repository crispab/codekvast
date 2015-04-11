# Codekvast Agent Release Notes

## 0.8.18

- Improved the server web interface.
- codekvast-agent now uses a private H2 database for storing not yet uploaded data.

## 0.8.17

- Added /etc/init.d scripts for agent and server.
- Documented installation of agent and server (work in progress).

## 0.8.16

- Simplified installation of codekvast-server.
- Renamed CollectorConfig.methodExecutionPointcut to CollectorConfig.methodVisibility. Same for JvmData. Breaks compatibility to 0.8.15.
- Made it possible to track not only public methods.
- Added UserManual.adoc and doc/UserManual.html

## 0.8.15

- Implemented backup and restore in codekvast-server.

## 0.8.14

- codekvast-collector.jar is now completely silent unless -Dcodekvast.options=verbose=true

## 0.8.13

- codekvast-collector.jar is less verbose on System.out

## 0.8.12

- codekvast-collector.jar will remain passive (a no-op) when no codekvast.conf is found.
 
## 0.8.11

- codekvast.conf is found automactically if placed in /etc/codekvast or /etc.

## 0.8.10

- Added possibility to configure collector with -Dcodekvast.options=key=value;key=value;... to override values in codekvast.conf

## 0.8.9

- Added column SIGNATURE.MILLIS_SINCE_JVM_START. Breaks compatibility to 0.8.8.
- Added table APPLICATION_SETTINGS

## 0.8.8

- Improved API between server and client: now uses a mix of HTTP requests and WebSocket messages

## 0.8.4

- Added columns JVM_INFO.COLLECTOR_RESOLUTION_SECONDS and JVM_INFO.METHOD_EXECUTION_POINTCUT. Breaks compatibility to 0.8.3
- Added DatabaseScavenger which keeps SIGNATURES from growing.

## 0.8.1

- Added even mode stuff to JvmData. 0.8.1 is incompatible with 0.8.0

## 0.8.0

- Added JvmData.computerID. Agent 0.8 is now incompatible with server 0.7.x
- Implemented chunking in AgentApi.uploadSignatureData() and AgentApi.uploadInvocationsData()

## 0.7.11

- Removed parameter `customerName` from **codekvast-collector.conf**
- Implemented a first version of Codekvast Invocation Data page, with live display of collected data. Still very rough, not entirely snappy ;)

## 0.7.10

- New strategy in **codekvast-collector.conf**

        appVersion: filename my-app-(.*).jar

    See **conf/codekvast-collector.conf.sample** for a description of how it works.

- New field in **codekvast-collector.conf**

        tags: tag1, tag2, ...

    Tags are arbitrary text values that are transported to the database along with the invocation data.
    Can be used for filtering in the presentation.

## 0.7.9

- Added support for expansions in **codekvast-collector.conf** and **codekvast-agent.conf**

    Now one can use Ant-style expansions in the right-hand side values in all config files.
    Environment variables and Java system properties (-Dkey=value) are supported.
    Nested expansions are **not** supported.

    See **conf/codekvast-collector.conf.sample** for a description of how it works.

## 0.7.8

- Made it work on a Tomcat app that uses Aspectj.
