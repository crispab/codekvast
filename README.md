# Codekvast - the Truly Dead Code Detector

## Overview

Codekvast detects **Truly Dead Code** in **Java-applications**.

By *Truly Dead Code* we mean code that is in production, but *has not been used by anyone for a certain period of time*.

Codekvast works by attaching the **Codekvast Collector** to your application.
The collector records the instant when methods belonging to any of your packages are invoked. 

Modern IDEs like IntelliJ IDEA can detect dead code which have *package 
private* or *private* visibility. They can never know what potential callers there are to public and protected methods though.
 
This is where Codekvast can help. Codekvast *records when your methods are invoked in production*.

The collector periodically sends the recorded data as well as an inventory of *all* methods to the **Codekvast Dashboard**.
 
By using the web interface offered Codekvast Dashboard, you can find out whether a certain method, class or package is safe to remove or not.

*Codekvast collects the data. You then combine that with your domain knowledge to make informed decisions about what is truly dead code
that safely could be deleted.*

## Performance

Codekvast Collector is extremely efficient. It adds approximately *15 ns* to each method invocation. If this is unacceptable,
you can exclude certain time critical packages from collection.

## License

Codekvast is released under the MIT license.

### Pre-built binaries

Pre-built binaries, and the User Manual are available for download from [downloads.codekvast.io](https://downloads.codekvast.io).

* codekvast-agent-x.x.x.zip contains the *Codekvast Agent* as well as a sample `codekvast.conf`.

* CodekvastUserManual.html is a complete User Manual. It contains installation and configuration guides.

* RELEASE-NOTES.md contains release notes.

`codekvast-agent-x.x.x.jar` is also available as a Maven artifact at [jcenter](https://jcenter.bintray.com/io/codekvast/codekvast-agent)

## Development Guide

See [DEVELOPMENT-GUIDE.md](DEVELOPMENT-GUIDE.md)
