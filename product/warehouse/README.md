# How to develop Codekvast Warehouse

Besides automatic tests on different levels one may want to test the warehouse interactively. This is how you do it.

## Backend

To get some data to play with, do like this:

1. In one window, start the daemon: `gradle :product:agent:daemon:bootRun`.
1. In a second window, start Jenkins: `gradle :sample:jenkins1:run`.
1. In the third window, do: `cd product/warehouse; gradle startMariadb`
    
This will produce collection data that will be consumed by the warehouse once it starts. You can stop Jenkins and the daemon after a few minutes.

To test the warehouse REST API: 

1. `gradle bootRun`. This will build and the launch the warehouse app.
1. Open the Swagger UI at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

Use the console for exploring the API, and the Swagger UI or `curl` for testing it.

**Hint:** pipe results from curl through `| jq .` for better readability! (Note the trailing dot!)

Install `jq` with `sudo apt-get install jq`

## Frontend

The frontend is developed with TypeScript, Angular2, npm and Webpack.

It is located in `src/webapp`.

The frontend can be developed with a running backend (gradle bootRun) and with a webpack-dev-server for the frontend stuff.
 
Start the frontend development environment with

    cd src/webapp
    npm start
    
This will start Webpack watch mode, and then launches a webpack-dev-server accessible
at [http://localhost:8081](http://localhost:8081) (the webapp).

When finished, stop with Ctrl-C.

You execute the frontend unit tests by executing

    npm test

This will start Karma that locates and executes all Jasmine test specs.

The next time `gradle assemble` is executed, the result of the Webpack bundling is copied into the Spring-boot jar, and is served from the
embedded web server.

## File watch limits

On some Linux distros, both IntelliJ IDEA and the Node.js uses the system service `inotify` to watch directories for changed files.

If the limit is to low, `npm start` will fail.

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
