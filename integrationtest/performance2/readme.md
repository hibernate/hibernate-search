Hibernate Search Performance Tests
==================================

## Description

Performance tests are disabled by default, they are not part of normal build.

Test can be run in two modes, standalone via `TestRunnerStandalone` or in container via `TestRunnerArquillain`.

Test is controled by class `TestScenario` or more precisely by one of its subclass, 
which should be specified via system property `-Dscenario`. 
Test scenario defines used hibernate configuration, number of threads, number of test cycles and what tasks will be executed during one test cycle. 
It has following phases: initialize database, warm up, clean up, initialize index and own measurement.

At the end is generated test report. It contains summary about test environment, used versions, 
total time per each phase, total time per each task, approximately memory usage before and after test, 
log from index check and log about uncaught execptions.


## Scenarios

- `org.hibernate.search.test.performance.scenario.SmokeTestScenario` (used by default)
- `org.hibernate.search.test.performance.scenario.FileSystemDefaultTestScenario`
- `org.hibernate.search.test.performance.scenario.FileSystemNearRealTimeTestScenario`
 

## Examples how to run

    > mvn clean test -Ppostgresql84
                     -Dtest=TestRunnerStandalone 
                     -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemDefaultTestScenario
                     -Dhibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
                     -Dhibernate.connection.driver_class=org.postgresql.Driver
                     -Dhibernate.connection.url=jdbc:postgresql://localhost:5432/dev
                     -Dhibernate.connection.username=foo
                     -Dhibernate.connection.password=foo
    
    > mvn clean test -Dtest=TestRunnerArquillian
                     -Dscenario=org.hibernate.search.test.performance.scenario.FileSystemNearRealTimeTestScenario 
                     -Ddatasource=foo
