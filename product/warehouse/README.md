# How to develop Codekvast Warehouse

## Backend

Backend development requires a running MariaDB database. `gradle startMariadb` will start MariaDB in a Docker container, unless port 3306 already is
open.

To test the REST API do 

1. `gradle bootRun`. This will build and the launch the app.
1. Open an API docs console at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Use the console for exploring the API, and `curl` for testing it.

**Hint:** pipe results from curl through `| jq .` for better readability! (Note the trailing dot!)

Install jq with `sudo apt-get install jq`

## Frontend

The frontend is developed with TypeScript and Angular2.

It is located in `src/main/webapp`. This is a vanilla Angular2 project, except for that the output from tsc is stored in `src/main/webapp/app` to make
it easier to integrate with Gradle.

There are a number of Gradle tasks that wrap npm commands, to make it simpler to use.

The IDEA TypeScript compiler is disabled, since it interferes with the one managed by NPM.

The frontend can be developed with a running backend (gradle bootRun) and with a NodeJS Lite Server for the frontend stuff.
 
Start the frontend development environment with `gradle npmStart`

This will start the TypeScript compiler tsc in watch mode, and then launches a Lite Server accessible
at [http://localhost:3000](http://localhost:3000) (the webapp) and 3001 ([Browser Sync](https://www.browsersync.io)).

When finished, stop the Lite Server with Ctrl-C.

The next time `gradle assemble` is executed, the result of the TypeScript compilation is copied to the Spring-boot jar.

**NOTE:** npmStart also starts [Browser Sync](https://www.browsersync.io), which means that you could test the webapp simultaneously in more than one browser
 (e.g., Chrome and Firefox).

## File watch limits
On some Linux distros, Both IntelliJ IDEA and the Node.js uses the system service `inotify` to watch directories for changed files.

If the limit is to low, `gradle npmStart` will fail.

If you happen to use Ubuntu, here is the remedy:

Create the file `/etc/sysctl.d/60-jetbrains.conf` with the following content:

    # Set inotify watch limit high enough for IntelliJ IDEA (PhpStorm, PyCharm, RubyMine, WebStorm).
    # Create this file as /etc/sysctl.d/60-jetbrains.conf (Debian, Ubuntu), and
    # run `sudo sysctl --system` or reboot.
    # Source: https://confluence.jetbrains.com/display/IDEADEV/Inotify+Watches+Limit
    # 
    # More information resources:
    # man inotify  # manpage
    # man sysctl.conf  # manpage
    # cat /proc/sys/fs/inotify/max_user_watches  # print current value in use
    
    fs.inotify.max_user_watches = 524288
    
Then do `sudo sysctl --system` to activate the changes.
