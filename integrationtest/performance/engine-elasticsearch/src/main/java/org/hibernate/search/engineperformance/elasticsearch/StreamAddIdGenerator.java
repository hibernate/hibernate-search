/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch;

import java.util.stream.IntStream;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.ThreadParams;

/**
 * Generate a continuous stream of add works,
 * whose IDs are guaranteed not to conflict with any other added document in the same trial.
 */
@State(Scope.Thread)
public class StreamAddIdGenerator {

	private int streamedAddsPerFlush;
	private int threadCount;

	private int nextId;

	@Setup(Level.Trial)
	public void setup(StreamWriteEngineHolder eh, ThreadParams threadParams) {
		streamedAddsPerFlush = eh.getStreamedAddsPerFlush();
		threadCount = threadParams.getThreadCount();

		/*
		 * Make sure each threads has a different starting point,
		 * so that no two threads can use the same ID.
		 */
		int initialIndexSize = eh.getInitialIndexSize();
		int threadIndex = threadParams.getThreadIndex();
		nextId = initialIndexSize + threadIndex;
	}

	public IntStream stream() {
		return IntStream.generate( this::next ).limit( streamedAddsPerFlush );
	}

	private int next() {
		int returned = nextId;
		nextId += threadCount;
		return returned;
	}

}
