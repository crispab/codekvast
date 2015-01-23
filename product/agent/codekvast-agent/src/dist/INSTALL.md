# How to install Codekvast Agent

## For each host where your application is installed

### Download and configure Codekvast Agent

1. Download [codekvast-agent.zip] (https://bintray.com/artifact/download/crisp/foobar/agent/foobar-agent-@CODEKVAST_VERSION@.zip) to any
directory

1. Unpack the zip file to any location in the file system

1. **cd path/to/codekvast-agent-@CODEKVAST_VERSION@/conf** (substitute **path/to** with the actual path were codekvast-agent is installed).

1. **cp codekvast-agent.conf.sample codekvast-agent.conf**

1. Edit **codekvast-agent.conf** to suit your needs.

### Modify your application's start script

#### Tomcat (Linux)

1. **cd path/to/tomcat** (substitute **path/to** with the actual path were Tomcat is installed).

1. **mkdir endorsed/**.

1. **ln -s path/to/codekvast-agent-@CODEKVAST_VERSION@/javaagents/aspectjweaver-@ASPECTJ_VERSION@.jar endorsed/**

1. **cp path/to/codekvast-agent-@CODEKVAST_VERSION@/conf/codekvast-collector.conf.sample conf/codekvast.conf**

1. Edit **conf/codekvast.conf** to suit your needs. *The sample config file is self-documenting!*

1. **cp path/to/codekvast-agent-@CODEKVAST_VERSION@/tomcat/setenv.sh bin/**

1. Edit **bin/setenv.sh** so that CODEKVAST_HOME points to the folder where Codekvast Agent is installed.

#### Tomcat (Windows)

Follow the instructions for Linux but replace all '/' characters with '\'. Also replace **.sh** with **.bat**.
The **cp** command is named **copy** in Windows.
The **ln -s** command does not exist in Windows. Use **copy** instead.

#### Other applications

Use the installation guide for Tomcat as a basis.

The goal is to make **-javaagent:/path/to/codekvast-collector-@CODEKVAST_VERSION@.jar=/path/to/codekvast.config
-javaagent:/path/to/aspectjweaver-@ASPECTJ_VERSION@.jar** appear as first arguments to the **java** command.

If you get **LinkageError** on some aspectj-related type you can try this:

1. Move **aspectjweaver-@ASPECTJ_VERSION@.jar** to a separate directory (called **/path/to/endorsed** below).

1. Add **-Djava.endorsed.dir=/path/to/endorsed/** to the **java** command.
