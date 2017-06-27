# Search Engine Performance tests

This module is designed to verify throughput of the object/document transformation and
of the index writing backends.
This module is decoupled from Hibernate ORM to allow running performance diagnostics
and find regressions in isolation from the other projects.
Some tests will use the 'blackhole' backend to further narrow the analysis to only
the object transformation, or other tests will test the backends exclusively.


## Build

To build the performance tests:

    $ mvn clean install


## Run it from command line with standard jars

To run the benchmarks:

    $ java -cp `cat target/classpath.txt`:target/engine-benchmark.jar org.openjdk.jmh.Main


## Run it from command line using an "uber jar"

There is an alternative way to run it which might be handy when needing to copy the
performance test to a dedicated server and for some reason you're not liking rsync:

    $ java -jar target/benchmarks.jar


## Run it from your IDE

Open 'org.hibernate.search.engineperformance.Launcher' from your IDE.


## Run flight recorder: long runs on a specific test

   $ java -jar target/benchmarks.jar -jvmArgsPrepend "-XX:+UnlockCommercialFeatures -XX:+FlightRecorder" -i 30000 -p backend=blackhole

 - jvmArgsPrepend: allows to add additional JVM properties to the forked process which runs the benchmark
 - p: set testing parameters to select a specific configuration
 - e: excludes running all tests matching this name
 - i: sets the number of iterations. Setting it very high here to have time to play with Flight Recorder before it shuts down


## Produce GC logs suited for tools

   $ java -jar target/benchmarks.jar -jvmArgsPrepend "-Xloggc:lognameHere.log -XX:+PrintGCDetails -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCCause" -i 30000


# Notes

For best results disable features such as power management, dynamic CPU scaling,
and run it on a dedicated box which has no other significant services running.
So the "run it from your IDE" approach is just meant for development of new tests.


## TODO

- add more tests, especially those focusing on backend performance
- add search tests
- include Infinispan Directory test, also in clustered modes
- cleanup the on-disk stored indexes automatically?
- use the word producer utility to feed larger, semi-random but reproducible text
- find out how to use JMH within a WildFly deployment without including the overhead from Arquillian in the measurements
- gradually replace the other performance tests to use JMH

