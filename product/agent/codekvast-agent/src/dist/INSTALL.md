# How to install Codekvast Agent

## For each host where your application is installed

### Download and configure Codekvast Agent

1. Download [codekvast-agent.zip] (https://bintray.com/artifact/download/crisp/foobar/agent/foobar-agent-@CODEKVAST_VERSION@.zip) to any
directory

1. Unpack the zip file to any location in the file system

1. `cd path/to/codekvast-agent-@CODEKVAST_VERSION@/conf`

1. Copy `codekvast-agent.conf.sample` to `codekvast-agent.conf`

1. Copy `codekvast-collector.conf.sample` to `codekvast-collector.conf`

1. Edit codekvast-agent.conf to suit your needs

1. Edit codekvast-collector.conf to suit your needs

### Modify your application's start script

#### Tomcat

1. Copy `path/to/codekvast-agent-@CODEKVAST_VERSION@/bin/setenv.sh` to Tomcat's `bin/` folder.

1. Edit `setenv.sh` so that CODEKVAST_HOME points to the folder where Codekvast Agent is installed.

#### Other applications

1. Copy the output from  `path/to/codekvast-agent-@CODEKVAST_VERSION@/bin/showJvmParams.sh`

1. Modify your application's start script, so that the output from the command above appears first in the command line that launches `java`.


