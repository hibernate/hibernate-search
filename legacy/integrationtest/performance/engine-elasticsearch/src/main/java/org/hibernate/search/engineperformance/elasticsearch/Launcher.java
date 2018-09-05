/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Launches all the JMH benchmarks within this project.
 * <p>
 * In order to run the benchmarks, generate the JMH benchmark classes by running
 * {@code mvn compile -pl :hibernate-search-performance-engine-elasticsearch} from the root dir.
 * <p>
 * Refer to the <a href="http://openjdk.java.net/projects/code-tools/jmh/">JMH documentation</a> to learn more about the
 * Java Micro-benchmark Harness in general.
 *
 * Typically you'll want to run this from a commandline; this Launcher is not meant
 * to take measurements but rather to simplify debugging and developing.
 *
 * @author Gunnar Morling
 * @author Sanne Grinovero
 */
public class Launcher {

	public static void main(String... args) throws Exception {
		Options opts = new OptionsBuilder()
			.include( ".*" )
			.warmupIterations( 1 )
			.measurementIterations( 10 )
			.param( "indexSize", "100" )
			.param( "maxResults", "10" )
			.param( "worksPerChangeset", "2;4" )
			.param( "changesetsPerFlush", "50" )
			.param( "streamedAddsPerFlush", "300" )
			.forks( 0 ) //To simplify debugging; Remember this implies JVM parameters via @Fork won't be applied.
			.build();

		new Runner( opts ).run();
	}

	private Launcher() {
		//Do not construct
	}

}
