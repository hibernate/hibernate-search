Hibernate Search Performance Tests
==================================

## Description

The performance tests can be run in two modes, standalone via `TestRunnerStandalone` or in container
via `TestRunnerArquillian`.

The test is controlled by the class `TestScenario` or more precisely by one of its subclasses,
which need to be specified via system property `-Dscenario`.
The test scenario defines the used hibernate configuration, number of threads and test cycles and
tasks to execute.

The following phases exist in the test:
* initialize database
* warm up
* initialize index
* measurement
* clean up

A test report is generated in the *target* directory. It contains a test summary including environment
settings, used versions, total time per phase, total time per task, approximated memory usage before
and after test, log from index check and uncaught exceptions.


## Scenarios

- `org.hibernate.search.test.performance.scenario.SmokeTestScenario` (used by default)
- `org.hibernate.search.test.performance.scenario.FileSystemDefaultTestScenario`
- `org.hibernate.search.test.performance.scenario.FileSystemNearRealTimeTestScenario`


## Enable benchmarking

All metrics are disabled by default, so that you're not forced to run lengthy test for any build.
A profile enables them:

    -Pperf


## Examples how to run

### Standalone accessing database directly

- run tests in standalone mode against in-memory database

        mvn clean test -Pperf -Dtest=TestRunnerStandalone \
        -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemDefaultTestScenario

Note: For the following scenarios, the test database needs to be created first with appropriate
username and password.

- run tests in standalone mode against a PostgreSQL database (via system properties)

        mvn clean test -Pperf -Ppostgresql84 -Dtest=TestRunnerStandalone \
        -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemDefaultTestScenario \
        -Dhibernate.dialect=org.hibernate.dialect.PostgreSQLDialect \
        -Dhibernate.connection.driver_class=org.postgresql.Driver \
        -Dhibernate.connection.url=jdbc:postgresql://localhost:5432/hibperf \
        -Dhibernate.connection.username=foo \
        -Dhibernate.connection.password=foo

- run tests in standalone mode against a MariaDB database (via system properties)

        mvn clean test -Pperf -Pmysql51 -Dtest=TestRunnerStandalone \
        -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemDefaultTestScenario \
        -Dhibernate.dialect=org.hibernate.dialect.MySQL5InnoDBDialect \
        -Dhibernate.connection.driver_class=com.mysql.jdbc.Driver \
        -Dhibernate.connection.url=jdbc:mysql://hostname:3306/hibperf \
        -Dhibernate.connection.username=foo \
        -Dhibernate.connection.password=foo

### In container against a data source

The tests will create the datasource automatically.
Just provide the parameters as you would do in standalone mode. 

- run tests in container mode against in-memory database

        mvn clean test -Pperf -Dtest=TestRunnerArquillian \
        -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemDefaultTestScenario

Note: For the following scenarios, the test database needs to be created first with appropriate
username and password.

- run tests in container mode against a PostgreSQL database (via system properties)

        mvn clean test -Pperf -Ppostgresql84 -Dtest=TestRunnerArquillian \
        -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemDefaultTestScenario \
        -Dhibernate.dialect=org.hibernate.dialect.PostgreSQLDialect \
        -Dhibernate.connection.driver_class=org.postgresql.Driver \
        -Dhibernate.connection.url=jdbc:postgresql://localhost:5432/hibperf \
        -Dhibernate.connection.username=foo \
        -Dhibernate.connection.password=foo

- run tests in container mode against a MariaDB database (via system properties)

        mvn clean test -Pperf -Pmysql51 -Dtest=TestRunnerArquillian \
        -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemDefaultTestScenario \
        -Dhibernate.dialect=org.hibernate.dialect.MySQL5InnoDBDialect \
        -Dhibernate.connection.driver_class=com.mysql.jdbc.Driver \
        -Dhibernate.connection.url=jdbc:mysql://hostname:3306/hibperf \
        -Dhibernate.connection.username=foo \
        -Dhibernate.connection.password=foo
