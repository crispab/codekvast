# Codekvast Agent Release Notes

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
