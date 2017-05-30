[Codekvast](https://elements.heroku.com/addons/codekvast) is an add-on for detecting Truly Dead Code within your application.

Continuously detecting and removing dead code provides the following benefits:
* Shorter Development Cycles.
* Reduced Technical Debt.
* Reduced Risk for Security Exploits.
* Faster Developer Onboarding.

Codekvast requires minimal changes to your application, and is currently available for Java.

## Provisioning the add-on

Codekvast can be attached to a Heroku application via the CLI:

> callout
> A list of all plans available can be found [here](https://elements.heroku.com/addons/codekvast).

```term
$ heroku addons:create codekvast
-----> Adding codekvast to sharp-mountain-4005... done, v18 (free)
```

After you provision Codekvast, the `CODEKVAST_URL` and `CODEKVAST_LICENSE_KEY` config variables are available in your app's configuration. It contains information for accessing the newly provisioned Codekvast service instance. You can confirm this via the `heroku config:get` command:

```term
$ heroku config:get CODEKVAST_URL
https://api.codekvast.io/
```

After you install Codekvast, your application should be configured to fully integrate with the add-on.

## Local setup

### Environment setup

After you provision the add-on, it's necessary to locally replicate its config vars so your development environment can operate against the service.

Use the Heroku Local command-line tool to configure, run and manage process types specified in your app's [Procfile](procfile). Heroku Local reads configuration variables from a `.env` file. To view all of your app's config vars, type `heroku config`. Use the following command for each value that you want to add to your `.env` file:

```term
$ heroku config:get CODEKVAST_URL -s  >> .env
$ heroku config:get CODEKVAST_LICENSE_KEY -s  >> .env
```

> warning
> Credentials and other sensitive configuration values should not be committed to source-control. In Git exclude the `.env` file with: `echo .env >> .gitignore`.

For more information, see the [Heroku Local](heroku-local) article.

## Using with Java/Gradle

## Using with Java/Maven

## Monitoring and logging

You can monitor Codekvast activity within the Heroku log-stream by:

```term
$ heroku logs -t | grep -i codekvast
```

## Dashboard

> callout
> For more information on the features available within the Codekvast dashboard, please see the docs at [docs.codekvast.io](docs.codekvast.io).

The Codekvast dashboard allows you to browse the collected usage data.

You can access the dashboard via the CLI:

```term
$ heroku addons:open codekvast
Opening codekvast for sharp-mountain-4005
```

or by visiting the [Heroku Dashboard](https://dashboard.heroku.com/apps) and selecting the application in question. Select Codekvast from the Add-ons menu.

## Troubleshooting

## Migrating between plans

> note
> Application owners should carefully manage the migration timing to ensure proper application function during the migration process.

Use the `heroku addons:upgrade` command to migrate to a new plan.

```term
$ heroku addons:upgrade codekvast:newplan
-----> Upgrading codekvast:newplan to sharp-mountain-4005... done, v18 ($49/mo)
       Your plan has been updated to: codekvast:newplan
```

## Removing the add-on

You can remove Codekvast via the CLI:

> warning
> This will destroy all associated data and cannot be undone!

```term
$ heroku addons:destroy codekvast
-----> Removing codekvast from sharp-mountain-4005... done, v20 (free)
```

## Support

All Codekvast support and runtime issues should be submitted via one of the [Heroku Support channels](support-channels). Any non-support related issues or product feedback is welcome at [codekvast-support@hit.se](mailto:codekvast@hit.se).