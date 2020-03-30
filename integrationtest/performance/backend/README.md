# Backend Performance tests

This module is designed to verify throughput of the document creation and indexing.

This module is decoupled from any mapper to allow running performance diagnostics
and find regressions in isolation from the various mappers.

## Build

To build the performance tests:

```
mvn clean install -pl integrationtest/performance/backend/elasticsearch -am -DskipTests
# OR
mvn clean install -pl integrationtest/performance/backend/lucene -am -DskipTests
```

## Run it from command line

```
java -jar integrationtest/performance/backend/elasticsearch/target/benchmarks.jar
# OR
java -jar integrationtest/performance/backend/lucene/target/benchmarks.jar
```

You may set parameters:

```
java -jar integrationtest/performance/backend/elasticsearch/benchmarks.jar \
    -jvmArgsPrepend -Dhosts=es1.mycompany.com \
    -jvmArgsPrepend -Dprotocol=https \
    -i 10 -p batchSize=50
```

* `jvmArgsPrepend`: add additional JVM properties to the forked process which runs the benchmark.
Note that this can be used to override backend properties, such as the Elasticsearch hosts in this example.
* `e`: excludes running all benchmarks matching this name.
* `w`: sets the number of warm-up iterations.
* `i`: sets the number of measurement iterations.
* `p`: set testing parameters (`@Param` in the code).

## Run it from your IDE

Within your IDE, run the test `SmokeIT` located in the project you're interested in.

## Run flight recorder: long runs on a specific test

```
java -jar integrationtest/performance/backend/elasticsearch/benchmarks.jar \
    -jvmArgsPrepend -XX:+FlightRecorder \
    -i 30000
```

We're setting it a very high number of iterations here to have time to play with
Flight Recorder before it shuts down.

You can also dump the result to a .jfr file and study it later (useful to run it on a remote server):

```
java -jar integrationtest/performance/backend/elasticsearch/benchmarks.jar \
    -jvmArgsPrepend -XX:+FlightRecorder \
    -jvmArgsPrepend -XX:StartFlightRecording=filename=output/profile.jfr,settings=profile
```

## Produce GC logs suited for tools

```
java -jar target/benchmarks.jar \
    -jvmArgsPrepend "-Xloggc:lognameHere.log -XX:+PrintGCDetails -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCCause" \
    -i 30000
```

# Notes

For best results disable features such as power management, dynamic CPU scaling,
and run it on a dedicated box which has no other significant services running.

In particular the "run it from your IDE" approach is just meant for development of new tests.

## TODO

- add more tests, especially those focusing on backend performance
- add search tests
