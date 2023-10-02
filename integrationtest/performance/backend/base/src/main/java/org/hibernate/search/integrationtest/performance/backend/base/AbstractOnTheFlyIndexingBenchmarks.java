/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.performance.backend.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset.Dataset;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset.DatasetHolder;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.AbstractBackendHolder;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.MappedIndex;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.index.PerThreadIndexPartition;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.GroupThreads;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Abstract class for JMH benchmarks related to on-the-fly indexing,
 * which is primarily used when doing CRUD operations on the database
 * in the ORM integration.
 * <p>
 * This benchmark generates and executes indexing plans from a single thread,
 * guaranteeing not to conflict with any other thread in the same trial.
 * <p>
 * Internally, this object keeps tab of documents currently present in the index
 * and generates works accordingly.
 */
@Fork(1)
@State(Scope.Thread)
public abstract class AbstractOnTheFlyIndexingBenchmarks extends AbstractBackendBenchmarks {

	@Param({ "NONE" })
	private DocumentRefreshStrategy refreshStrategy;

	/**
	 * The number of works of each type (add/update/delete)
	 * to put in each write plan.
	 */
	@Param({ "20" })
	private int worksPerTypePerWritePlan;

	/*
	 * We just want a sequence of numbers that spreads uniformly over a large interval,
	 * but we don't need cryptographically secure randomness,
	 * and we want the sequence to be the same from one test run to another.
	 * That's why we simply use {@link Random} and that's why we set the seed to
	 * a hard-coded value.
	 * Also, we use one random generator per thread to avoid contention.
	 */
	private final Random idShufflingRandom = new Random( 3210140441369L );

	private Dataset dataset;

	private long invocationCount;

	private List<Long> idsToAdd;
	private List<Long> idsToUpdate;
	private List<Long> idsToDelete;

	@Setup(Level.Iteration)
	public void prepareIteration(DatasetHolder datasetHolder) {
		this.dataset = datasetHolder.getDataset();
		this.invocationCount = 0L;

		int threadIdIntervalSize = 3 * worksPerTypePerWritePlan;

		// Initialize the ID lists
		idsToAdd = new ArrayList<>();
		idsToUpdate = new ArrayList<>();
		idsToDelete = new ArrayList<>();
		List<Long> shuffledIds = createShuffledIndexList( threadIdIntervalSize );
		int offset = 0;
		for ( int i = 0; i < worksPerTypePerWritePlan; ++i ) {
			idsToAdd.add( shuffledIds.get( i ) );
		}
		offset += worksPerTypePerWritePlan;
		for ( int i = 0; i < worksPerTypePerWritePlan; ++i ) {
			idsToDelete.add( shuffledIds.get( offset + i ) );
		}
		offset += worksPerTypePerWritePlan;
		for ( int i = 0; i < worksPerTypePerWritePlan; ++i ) {
			idsToUpdate.add( shuffledIds.get( offset + i ) );
		}

		getIndexInitializer().addToIndex(
				getIndexPartition().getIndex(),
				Stream.concat( idsToUpdate.stream(), idsToDelete.stream() )
						.mapToLong( getIndexPartition()::toDocumentId )
		);
	}

	@Benchmark
	@Threads(10 * AbstractBackendHolder.INDEX_COUNT)
	public void indexingPlan(WriteCounters counters) {
		PerThreadIndexPartition partition = getIndexPartition();
		MappedIndex index = partition.getIndex();
		IndexIndexingPlan indexingPlan =
				index.createIndexingPlan( getCommitStrategyParam(), refreshStrategy );

		for ( Long documentIdInThread : idsToAdd ) {
			long documentId = partition.toDocumentId( documentIdInThread );
			indexingPlan.add(
					StubMapperUtils.referenceProvider( String.valueOf( documentId ) ),
					document -> dataset.populate( index, document, documentId, invocationCount )
			);
		}
		for ( Long documentIdInThread : idsToUpdate ) {
			long documentId = partition.toDocumentId( documentIdInThread );
			indexingPlan.addOrUpdate(
					StubMapperUtils.referenceProvider( String.valueOf( documentId ) ),
					document -> dataset.populate( index, document, documentId, invocationCount )
			);
		}
		for ( Long documentIdInThread : idsToDelete ) {
			long documentId = partition.toDocumentId( documentIdInThread );
			indexingPlan.delete(
					StubMapperUtils.referenceProvider( String.valueOf( documentId ) )
			);
		}

		// Do not return until works are *actually* executed
		Futures.unwrappedExceptionJoin( indexingPlan.execute( OperationSubmitter.blocking() ) );

		counters.write += 3 * worksPerTypePerWritePlan;

		++invocationCount;

		List<Long> nextToDelete = idsToUpdate;
		idsToUpdate = idsToAdd;
		idsToAdd = idsToDelete;
		idsToDelete = nextToDelete;
	}

	@Benchmark
	@GroupThreads(10 * AbstractBackendHolder.INDEX_COUNT)
	@Group("concurrentReadWrite")
	public void concurrentIndexingPlan(WriteCounters counters) {
		indexingPlan( counters );
	}

	@Benchmark
	@GroupThreads(2 * AbstractBackendHolder.INDEX_COUNT)
	@Group("concurrentReadWrite")
	public void concurrentQuery(QueryParams params, Blackhole blackhole) {
		PerThreadIndexPartition partition = getIndexPartition();
		MappedIndex index = partition.getIndex();

		SearchResult<DocumentReference> results = index.createScope().query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( MappedIndex.SHORT_TEXT_FIELD_NAME ) )
				.fetch( params.getQueryMaxResults() );

		blackhole.consume( results.total().hitCount() );
		for ( DocumentReference hit : results.hits() ) {
			blackhole.consume( hit );
		}
	}

	protected abstract DocumentCommitStrategy getCommitStrategyParam();

	private List<Long> createShuffledIndexList(int size) {
		List<Long> result = new ArrayList<>( size );
		for ( int i = 0; i < size; ++i ) {
			result.add( (long) i );
		}
		Collections.shuffle( result, idShufflingRandom );
		return result;
	}

}
