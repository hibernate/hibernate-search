Hibernate Search Performance Tests
==================================

## Description

The performance tests can be run in two modes, standalone via `TestRunnerStandalone` or in container via `TestRunnerArquillian`.

The test is controlled by class `TestScenario` or more precisely by one of its subclasses,
which should be specified via system property `-Dscenario`. 
The test scenario defines used hibernate configuration, number of threads, number of test cycles and
what tasks will be executed during one test cycle. The following phases exist: initialize database, warm up, clean up,
initialize index and own measurement.

At the end, a test report is generated in the *target* directory. It contains a test summary including environment
settings, used versions, total time per phase, total time per task, approximated memory usage before and after test,
log from index check and uncaught exceptions.


## Scenarios

- `org.hibernate.search.test.performance.scenario.SmokeTestScenario` (used by default)
- `org.hibernate.search.test.performance.scenario.FileSystemDefaultTestScenario`
- `org.hibernate.search.test.performance.scenario.FileSystemNearRealTimeTestScenario`


## Enable benchmarking

All metrics are disabled by default, so that you're not forced to run lengthy test for any build.
A system property enables them:

   > -Dorg.hibernate.search.enable_performance_tests=true
 

## Examples how to run

- run tests in standalone mode against in-memory database

    > mvn clean test -Dtest=TestRunnerStandalone -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemDefaultTestScenario -Dorg.hibernate.search.enable_performance_tests=true

- run tests in standalone mode against a PostgreSQL database (via system properties)

    > mvn clean test -Ppostgresql84 -Dtest=TestRunnerStandalone -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemDefaultTestScenario -Dhibernate.dialect=org.hibernate.dialect.PostgreSQLDialect -Dhibernate.connection.driver_class=org.postgresql.Driver -Dhibernate.connection.url=jdbc:postgresql://localhost:5432/dev -Dhibernate.connection.username=foo -Dhibernate.connection.password=foo -Dorg.hibernate.search.enable_performance_tests=true

- run tests in standalone mode against a MariaDB database (via system properties)

    > mvn clean test -Pmysql51 -Dtest=TestRunnerStandalone -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemDefaultTestScenario -Dhibernate.hikari.dataSourceClassName=com.mysql.jdbc.jdbc2.optional.MysqlDataSource -Dhibernate.dialect=org.hibernate.dialect.MySQL5InnoDBDialect -Dhibernate.hikari.dataSource.url=jdbc:mysql://hostname:3306/hibperf -Dhibernate.hikari.dataSource.user=foo -Dhibernate.hikari.dataSource.password=foo -Dorg.hibernate.search.enable_performance_tests=true
                     
- run tests in container mode against default data source (java:jboss/datasources/ExampleDS)

    > mvn clean test -Dtest=TestRunnerArquillian -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemDefaultTestScenario -Dorg.hibernate.search.enable_performance_tests=true

- run tests in container mode against specified data source (via system property)                     
    
    > mvn clean test -Dtest=TestRunnerArquillian -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemDefaultTestScenario -Ddatasource=foo -Dorg.hibernate.search.enable_performance_tests=true
