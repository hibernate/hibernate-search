/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engineperformance.elasticsearch;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.hibernate.search.engineperformance.elasticsearch.datasets.Dataset;
import org.hibernate.search.engineperformance.elasticsearch.model.AbstractBookEntity;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.ThreadParams;

/**
 * Generate a continuous stream of entities to add,
 * whose IDs are guaranteed not to conflict with any other added document in the same trial.
 */
@State(Scope.Thread)
public class StreamAddEntityGenerator {

	private int streamedAddsPerFlush;
	private int threadCount;

	private Dataset<? extends AbstractBookEntity> dataset;

	private int nextId;

	@Setup(Level.Trial)
	public void setup(StreamWriteEngineHolder eh, StreamDatasetHolder dh, ThreadParams threadParams) {
		streamedAddsPerFlush = eh.getStreamedAddsPerFlush();
		threadCount = threadParams.getThreadCount();

		int threadIndex = threadParams.getThreadIndex();

		dataset = dh.getDataset( threadIndex );

		/*
		 * Make sure each threads has a different starting point,
		 * so that no two threads can use the same ID.
		 */
		int initialIndexSize = dh.getInitialIndexSize();
		nextId = initialIndexSize + threadIndex;
	}

	public Stream<AbstractBookEntity> stream() {
		return IntStream.generate( this::next ).limit( streamedAddsPerFlush )
				.mapToObj( dataset::create );
	}

	public IndexedTypeIdentifier getTypeId() {
		return dataset.getTypeId();
	}

	private int next() {
		int returned = nextId;
		nextId += threadCount;
		return returned;
	}

}
