# Backend Performance tests

This module is designed to verify throughput of the document creation and indexing.

This module is decoupled from any mapper to allow running performance diagnostics
and find regressions in isolation from the various mappers.

## Build

To build the performance tests:

```
mvn clean install -pl integrationtest/performance/backend/elasticsearch -am -DskipTests -DskipITs
# OR
mvn clean install -pl integrationtest/performance/backend/lucene -am -DskipTests -DskipITs
```

## Run it from command line

```
java -jar integrationtest/performance/backend/elasticsearch/target/benchmarks.jar
# OR
java -jar integrationtest/performance/backend/lucene/target/benchmarks.jar
```

You may pass arguments, which will be interpreted as regexps. Any benchmark matching a regexp will be run.
By default all benchmarks get run, one after the other.

You may set parameters:

```
java -jar integrationtest/performance/backend/elasticsearch/benchmarks.jar \
    -jvmArgsPrepend -Duris=http://es1.mycompany.com \
    -jvmArgsPrepend -Dprotocol=https \
    -i 10 -p batchSize=50
```

* `jvmArgsPrepend`: add additional JVM properties to the forked process which runs the benchmark.
Note that this can be used to override backend properties, such as the Elasticsearch hosts in this example.
* `e`: excludes running all benchmarks matching this regexp pattern.
* `wi`: sets the number of warm-up iterations.
* `w`: sets the minimum time to spend at each warmup iteration.
* `i`: sets the number of measurement iterations.
* `r`: sets the minimum time to spend at each measurement iteration.
* `p`: set testing parameters (`@Param` in the code).

## Run it from your IDE

Within your IDE, run the test `SmokeIT` located in the project you're interested in.

## Run a profiler

Use the built-in profiler integrations, in particular the [AsyncProfiler integration](https://github.com/openjdk/jmh/blob/6d6ce6315dc39d1d3abd0e3ac9eca9c38f767112/jmh-core/src/main/java/org/openjdk/jmh/profile/AsyncProfiler.java)

You will need to [download/install AsyncProfiler](https://github.com/async-profiler/async-profiler?tab=readme-ov-file#download) first.
The following examples assume you set the async profiler home this way:

```shell
ASYNC_PROFILER_HOME=...
```

To profile GC, perfnorm, CPU, wall-clock and memory allocation (on Linux):

```shell
BACKEND=lucene
java -jar integrationtest/performance/backend/$BACKEND/target/benchmarks.jar \
    -prof gc -prof perfnorm \
    -prof="async:rawCommand=alloc,wall;event=cpu;output=jfr;dir=/tmp;libPath=${ASYNC_PROFILER_HOME}/lib/libasyncProfiler.so"
```

This will create directories with JFR recordings (`jfr-cpu.jfr`) in `/tmp`.

To convert the JFR recording to a wall clock flamegraph (most useful if you want to consider I/O):

```
JFR_RECORDING_PATH=...
java -cp $ASYNC_PROFILER_HOME/lib/converter.jar jfr2flame --state runnable,sleeping $JFR_RECORDING_PATH wall.html
```

To convert the JFR recording to a CPU flamegraph (only useful if you want to ignore I/O):

```
JFR_RECORDING_PATH=...
java -cp $ASYNC_PROFILER_HOME/lib/converter.jar jfr2flame --state default $JFR_RECORDING_PATH cpu.html
```

See https://github.com/async-profiler/async-profiler/issues/740#issue-1650739133 for information on why we're using `--state`.

To convert the JFR recording to a memory allocation flamegraph:

```
JFR_RECORDING_PATH=...
java -cp $ASYNC_PROFILER_HOME/lib/converter.jar jfr2flame --alloc --total $JFR_RECORDING_PATH alloc.html
```

# Notes

For best results disable features such as power management, dynamic CPU scaling,
and run it on a dedicated box which has no other significant services running.

In particular the "run it from your IDE" approach is just meant for development of new tests.
