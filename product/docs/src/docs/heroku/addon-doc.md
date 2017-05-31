[Codekvast](https://elements.heroku.com/addons/codekvast) is an add-on for detecting _Truly Dead Code_ within your application.

By Truly Dead Code we mean code that is in production, but has not been used for a certain period of time.

Continuously detecting and removing dead code provides the following benefits:

* Shorter Development Cycles.
* Reduced Technical Debt.
* Reduced Risk for Security Exploits.
* Faster Developer Onboarding.

Codekvast requires minimal changes to your application, and is currently available for Java (Java 7 or higher).

## Install the Codekvast add-on

Codekvast can be attached to a Heroku application via the CLI:

> A list of all available plans can be found [here](https://elements.heroku.com/addons/codekvast).

```term
$ heroku addons:create codekvast
-----> Adding codekvast to sharp-mountain-4005... done, v18 (free)
```

Installing the add-on automatically creates a private Codekvast account integrated with Heroku, configures access for Heroku servers and adds a Codekvast link to your Add-ons list in the Heroku UI.

After you provision Codekvast, the `CODEKVAST_URL` and `CODEKVAST_LICENSE_KEY` config variables are available in your app's configuration. They contain information for accessing the newly provisioned Codekvast service instance. You can confirm this via the `heroku config:get` command:

```term
$ heroku config:get CODEKVAST_URL
https://api.codekvast.io/
```

Continue with the procedures which follows to configure the Codekvast agent.

## Configure your Codekvast javaagent

### Manual configuration

1. Download [codekvast-agent-0.20.3.zip](https://dl.bintray.com/crisp/codekvast/0.20.3/codekvast-agent-0.20.3.zip) and unzip it onto the root of your project. It will create a sample `codekvast.conf` as well as a `codekvast/` directory that contains the Java agent.

1. Edit `codekvast.conf` to suit your needs. You should not change the value of serverUrl or licenseKey, since they are injected from the Heroku environment.

1. Change the environment variable `JAVA_OPTS`:
```term
heroku config:set JAVA_OPTS="-javaagent:codekvast/codekvast-javaagent-0.20.3.jar -Xbootclasspath/a:codekvast/codekvast-javaagent-0.20.3.jar"
```

### If you use Gradle and Spring Boot

If you use the `spring-boot-gradle-plugin` to build an executable jar file, then you must edit `Procfile` so that it injects $JAVA_OPTS on the command line:
```term
web: env SERVER_PORT=$PORT java $JAVA_OPTS -jar build/libs/*.jar
```

You must also edit codekvast.conf so that
```properties
codeBase = build/libs, build/classes/main
```

or else Codekvast will not find your application's classes.

### Using Gradle dependencies

It is possible to configure Gradle to download the Codekvast agent as a regular build-time dependency instead of adding it to Git.

A sample Spring Boot application that uses Gradle for downloading codekvast-agent.jar at build time is available at [https://github.com/crispab/codekvast-spring-heroku](https://github.com/crispab/codekvast-spring-heroku).

> Note
> The Gradle build does not create codekvast.conf for you!

### Using with Java/Maven

To Be Completed.

### Testing locally

After installing the add-on and configuring the Codekvast java agent, you can test locally that everything works as expected and that data collection takes place.
 
It's necessary to locally replicate the add-on config vars so your development environment can operate against the service.

Use the Heroku Local command-line tool to configure, run and manage process types specified in your app's [Procfile](procfile). Heroku Local reads configuration variables from a `.env` file. To view all of your app's config vars, type `heroku config`. Use the following command for each value that you want to add to your `.env` file:

```term
heroku config:get CODEKVAST_URL -s  >> .env
heroku config:get CODEKVAST_LICENSE_KEY -s  >> .env
heroku config:get JAVA_OPTS -s  >> .env
```

> Warning
> Credentials and other sensitive configuration values should not be committed to source-control. In Git exclude the `.env` file with: `echo .env >> .gitignore`.

For more information, see the [Heroku Local](heroku-local) article.

### Monitoring and logging

You can monitor Codekvast activity within the Heroku log-stream by:

```term
$ heroku logs -t | grep -i codekvast
```

## Dashboard

> note
> For more information on the features available within the Codekvast dashboard, please see the docs at [docs.codekvast.io](http://docs.codekvast.io).

The Codekvast dashboard allows you to browse the collected usage data.

You can access the dashboard via the CLI:

```term
$ heroku addons:open codekvast
Opening codekvast for sharp-mountain-4005
```

or by visiting the [Heroku Dashboard](https://dashboard.heroku.com/apps) and selecting the application in question. Select Codekvast from the Add-ons menu.

## Troubleshooting

Use `heroku logs -t` for trouble shooting.

## Migrating between plans

Use the `heroku addons:upgrade` command to migrate to a new plan.

```term
$ heroku addons:upgrade codekvast:newplan
-----> Upgrading codekvast:newplan to sharp-mountain-4005... done, v18 ($49/mo)
       Your plan has been updated to: codekvast:newplan
```

> Note
> Migrating from a shared plan to a dedicated plan will destroy your collected data and cannot be undone!

## Uninstalling the Codekvast add-on

Use the following procedure for uninstalling the Codekvast add-on:
1. Remove `codekvast.conf` from your project root.
1. Revert the changes to the build system or `heroku config:unset JAVA_OPTS`
1. `git push heroku master`

Then deprovision the Codekvast service via the CLI:

> Warning
> This will destroy all collected data and cannot be undone!

```term
$ heroku addons:destroy codekvast
-----> Removing codekvast from sharp-mountain-4005... done, v20 (free)
```

## Support

All Codekvast support and runtime issues should be submitted via one of the [Heroku Support channels](support-channels).

Any non-support related issues or product feedback is welcome at [https://github.com/crispab/codekvast/issues](https://github.com/crispab/codekvast/issues).

There is also a Slack account for Codekvast. Send an email to [codekvast@hit.se](mailto:codekvast@hit.se) to get an invitation.
