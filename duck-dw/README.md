# Introduction

The DUCK Data Warehouse REST API Application is meant to store data from the DUCK Agent and provide data to the DUCK Consumers.
The application is based on the dropwizard-example: https://github.com/dropwizard/dropwizard/tree/master/dropwizard-example


# Overview

Included with this application is an example of the optional db API module. The examples provided illustrate a few of
the features available in [JDBI](http://jdbi.org), along with demonstrating how these are used from within dropwizard.

This database example is comprised of the following classes.

* The `PersonDAO` illustrates using the [SQL Object Queries](http://jdbi.org/sql_object_api_queries/) and string template
features in JDBI.

* The `PeopleDAO.sql.stg` stores all the SQL statements for use in the `PersonDAO`, note this is located in the
src/resources under the same path as the `PersonDAO` class file.

* `migrations.xml` illustrates the usage of `dropwizard-migrations` which can create your database prior to running
your application for the first time.

* The `PersonResource` and `PeopleResource` are the REST resource which use the PersonDAO to retrieve data from the database, note the injection
of the PersonDAO in their constructors.

As with all the modules the db example is wired up in the `initialize` function of the `HelloWorldApplication`.

# Running The Application

To test the example application run the following commands.

* To package the example run.

        mvn package

* To setup the h2 database run.

        java -jar target/duck-dw-0.7.1-SNAPSHOT.jar db migrate example.yml

* To run the server run.

        java -jar target/duck-dw-0.7.1-SNAPSHOT.jar server example.yml

* To hit the Hello World example (hit refresh a few times).

	http://localhost:8080/hello-world

* To post data into the application.

	curl -H "Content-Type: application/json" -X POST -d '{"fullName":"Other Person","jobTitle":"Other Title"}' http://localhost:8080/people
	
	open http://localhost:8080/people
