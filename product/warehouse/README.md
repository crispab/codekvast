# How to develop Codekvast Warehouse

## Backend

Backend development requires a running MariaDB database with correct configuration.

To test the REST API against a running app do 

1. `gradle bootRun`. This will build Codekvast Warehouse, start a Docker container with MariaDB and the launch the app.
1. Open an API docs console at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Use the console for exploring the API, and curl for testing it.

**Hint:** pipe results from curl through `| jq .` for better readability! (Note the trailing dot!)

Install jq with `sudo apt-get install jq`

## Frontend

The frontend is developed with a running backend (gradle bootRun) and with a NodeJS lite server for the frontend stuff.
 
Start the frontend development environment with

`gradle runLiteServer`

It starts the TypeScript compiler tsc in watch mode, and then launches a Lite server on ports 3000 (the webapp) and 3001 ([Browser Sync](https://www.browsersync.io)).

When finished, stop the Lite server with Ctrl-C.

The next time `gradle assemble` is done, the result of the TypeScript compilation is copied to the Spring-boot jar.

**NOTE:** runLiteServer also starts [Browser Sync](https://www.browsersync.io), which means that you could test the webapp simultaneously in more than one browser
 (e.g., Chrome and Firefox).

## File watch limits
Both IntelliJ IDEA and the Node.js uses the system service `inotify` to watch directories for changed files.

If the limit is to low, `gradle runLiteServer` will fail.

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
