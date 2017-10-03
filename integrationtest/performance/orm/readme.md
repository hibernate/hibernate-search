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
- `org.hibernate.search.test.performance.scenario.FileSystemReadWriteTestScenario`
- `org.hibernate.search.test.performance.scenario.FileSystemNearRealTimeReadWriteTestScenario`
- `org.hibernate.search.test.performance.scenario.FileSystemSessionMassIndexerTestScenario`
- `org.hibernate.search.test.performance.scenario.FileSystemJsr352MassIndexerTestScenario`


## Enable benchmarking

Data sets are small and metrics are disabled by default,
so that you're not forced to run lengthy test for any build.
A profile enables larger data sets and metrics recording:

    -Pperf


## Examples how to run

### Standalone accessing database directly

- run tests in standalone mode against in-memory database,
  **with a small dataset and with metrics disabled**
  (otherwise you will probably run out of memory)

        mvn clean test -Dtest=TestRunnerStandalone \
        -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemReadWriteTestScenario

Note: For the following scenarios, the test database needs to be created first with appropriate
username and password.

- run tests in standalone mode against a PostgreSQL database,
  with a large dataset and with metrics enabled

        mvn clean test -Pperf -Ppostgresql84 -Dtest=TestRunnerStandalone \
        -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemReadWriteTestScenario \
        -Djdbc.url=jdbc:postgresql://localhost:5432/hibperf \
        -Djdbc.user=foo \
        -Djdbc.pass=foo

- run tests in standalone mode against a MariaDB database,
  with a large dataset and with metrics enabled

        mvn clean test -Pperf -Pmysql51 -Dtest=TestRunnerStandalone \
        -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemReadWriteTestScenario \
        -Djdbc.url=jdbc:mysql://hostname:3306/hibperf \
        -Djdbc.user=foo \
        -Djdbc.pass=foo

### In container against a data source

The tests will create the datasource automatically.
Just provide the parameters as you would do in standalone mode.

- run tests in container mode against in-memory database,
  **with a small dataset and with metrics disabled**
  (otherwise you will probably run out of memory)

        mvn clean test -Dtest=TestRunnerArquillian \
        -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemReadWriteTestScenario

Note: For the following scenarios, the test database needs to be created first with appropriate
username and password.

- run tests in container mode against a PostgreSQL database,
  with a large dataset and with metrics enabled

        mvn clean test -Pperf -Ppostgresql84 -Dtest=TestRunnerArquillian \
        -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemReadWriteTestScenario \
        -Djdbc.url=jdbc:postgresql://localhost:5432/hibperf \
        -Djdbc.user=foo \
        -Djdbc.pass=foo

- run tests in container mode against a MariaDB database,
  with a large dataset and with metrics enabled

        mvn clean test -Pperf -Pmysql51 -Dtest=TestRunnerArquillian \
        -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemReadWriteTestScenario \
        -Djdbc.url=jdbc:mysql://hostname:3306/hibperf \
        -Djdbc.user=foo \
        -Djdbc.pass=foo
