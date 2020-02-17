/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.lucene;

import org.junit.Test;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

/**
 * Test that JMH benchmarks work correctly on a very short run.
 * <p>
 * This may not work correctly when run from the IDE.
 * <p>
 * See README to know how to run the benchmark from the command line to obtain more reliable results.
 */
public class SmokeIT {

	@Test
	public void test() throws RunnerException {
		Options opts = new OptionsBuilder()
				.include( ".*" )
				.warmupIterations( 0 )
				.measurementIterations( 1 )
				.measurementTime( TimeValue.seconds( 1 ) )
				.param(
						"configuration",
						"",
						"io.commit_interval=100&io.refresh_interval=100"
				)
				.param( "initialIndexSize", "100" )
				.param( "batchSize", "10" )
				.param( "maxResults", "10" )
				.shouldFailOnError( true )
				.forks( 0 ) // To simplify debugging; Remember this implies JVM parameters via @Fork won't be applied.
				.build();

		new Runner( opts ).run();
	}

}
