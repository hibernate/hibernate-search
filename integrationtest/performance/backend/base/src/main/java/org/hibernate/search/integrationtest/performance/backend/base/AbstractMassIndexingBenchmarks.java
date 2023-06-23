/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.performance.backend.base;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset.Dataset;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset.DatasetHolder;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.AbstractBackendHolder;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.MappedIndex;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;

/**
 * Abstract class for JMH benchmarks related to mass indexing,
 * i.e. document adds with few commits.
 * <p>
 * This benchmark generates and executes batches of documents to add to the index from a single thread,
 * guaranteeing not to conflict with any other thread in the same trial.
 */
@Fork(1)
@State(Scope.Thread)
public abstract class AbstractMassIndexingBenchmarks extends AbstractBackendBenchmarks {

	@Param({ "NONE" })
	private DocumentCommitStrategy commitStrategy;

	/**
	 * Equivalent to the MassIndexer's "batchSizeToLoadObjects".
	 */
	@Param({ "200" })
	private int batchSize;

	private MappedIndex index;
	private IndexIndexer indexer;
	private Dataset dataset;

	private long currentDocumentIdInThread = 0L;

	@Setup(Level.Iteration)
	public void prepareIteration(DatasetHolder datasetHolder) {
		index = getIndexPartition().getIndex();
		indexer = index.createIndexer();
		dataset = datasetHolder.getDataset();
	}

	@Benchmark
	@Threads(10 * AbstractBackendHolder.INDEX_COUNT)
	public void writeBatch(WriteCounters counters) throws InterruptedException {
		CompletableFuture<?>[] futures = new CompletableFuture<?>[batchSize];

		for ( int i = 0; i < batchSize; ++i ) {
			long documentId = getIndexPartition().toDocumentId( currentDocumentIdInThread++ );
			futures[i] = indexer.add(
					StubMapperUtils.referenceProvider( String.valueOf( documentId ) ),
					document -> dataset.populate( index, document, documentId, 0L ),
					commitStrategy, DocumentRefreshStrategy.NONE, OperationSubmitter.blocking()
			);
		}

		// Do not return until works are *actually* executed
		Futures.unwrappedExceptionGet( CompletableFuture.allOf( futures ) );

		counters.write += batchSize;
	}

}
