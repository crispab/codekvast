title=What is Codekvast?
authors=Olle Hallin
date=2018-05-18
tags=Overview, Benefits
type=page
status=published
~~~~~~

Codekvast detects **Truly Dead Code** in **Java-applications**.

By *Truly Dead Code* we mean code that is in production, but *has not been used by anyone for a significant period of time*.

Continuously detecting and removing dead code provides the following benefits:

* Shorter Development Cycles.
* Reduced Technical Debt.
* Reduced Risk for Security Exploits.
* Faster Developer On-boarding.

Codekvast requires minimal changes to your application, and is currently available for Java and Scala (JVM 7 or higher).

You enable Codekvast by attaching the **Codekvast Agent** to your application.

The agent inspects the application and prepares an inventory of all methods in your packages.
The agent then records the instants any of those methods are invoked.

The agent periodically uploads collected data to the Codekvast Dashboard, which offers a web interface for analyzing the collected data.

Modern IDEs like IntelliJ IDEA can detect dead code which have *package 
private* or *private* visibility. They can never know what potential callers there are to public and protected methods though.
 
This is where Codekvast can help. Codekvast *records when your methods are invoked in production* and stores the data for analysis in the *Codekvast Dashboard*.

By using the web interface offered by Codekvast Dashboard, you can find out whether a certain method, class or package is safe to remove or not.

*Codekvast collects the data. You then combine that with your domain knowledge to make informed decisions about what is truly dead code
that safely could be deleted.*

# Performance

Codekvast Agent is extremely efficient. It adds approximately *15 ns* to each method invocation. If this is unacceptable,
you can exclude certain time critical packages from collection.

# Availability

Codekvast is currently available with self-service as a [Heroku Add-on](https://devcenter.heroku.com/articles/codekvast?preview=1).

Not running in Heroku? Send an email to **codekvast-support@hit.se**, and we will be happy to help. 

# License

Codekvast Agent is released under the MIT license.

# Need more info?

Please see [Contact]({filename}/pages/80-contact.md).
