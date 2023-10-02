/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.performance.backend.base.testsupport.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.LongStream;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset.Dataset;
import org.hibernate.search.integrationtest.performance.backend.base.testsupport.dataset.DatasetHolder;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils;

import org.jboss.logging.Logger;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@State(Scope.Benchmark)
public class IndexInitializer {

	private static final Logger log = Logger.getLogger( "initialization" );

	@Param({ "10000" })
	private long initialIndexSize;

	private Dataset dataset;

	@Setup(Level.Trial)
	public void setup(DatasetHolder datasetHolder) {
		dataset = datasetHolder.getDataset();
	}

	public long getInitialIndexSize() {
		return initialIndexSize;
	}

	public void intializeIndexes(List<MappedIndex> indexes) {
		ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
		List<ForkJoinTask<?>> tasks = new ArrayList<>();
		for ( MappedIndex index : indexes ) {
			ForkJoinTask<?> task = forkJoinPool.submit(
					() -> initializeIndex( index, LongStream.range( 0, initialIndexSize ) )
			);
			tasks.add( task );
		}
		for ( ForkJoinTask<?> task : tasks ) {
			task.join();
		}
	}

	public void addToIndex(MappedIndex index, LongStream idStream) {
		log( index, "Adding documents to index..." );

		IndexWorkspace workspace = index.createWorkspace();
		IndexIndexer indexer = index.createIndexer();
		List<CompletableFuture<?>> futures = new ArrayList<>();
		idStream.forEach( id -> {
			CompletableFuture<?> future = indexer.add(
					StubMapperUtils.referenceProvider( String.valueOf( id ) ),
					document -> dataset.populate( index, document, id, 0L ),
					DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE,
					OperationSubmitter.blocking()
			);
			futures.add( future );
		} );
		CompletableFuture.allOf( futures.toArray( new CompletableFuture[0] ) ).join();
		workspace.flush( OperationSubmitter.blocking(), UnsupportedOperationBehavior.IGNORE ).join();

		log( index, " ... added " + futures.size() + " documents to the index." );
	}

	private void initializeIndex(MappedIndex index, LongStream idStream) {
		log( index, "Starting index initialization..." );
		log( index, "Purging..." );
		IndexWorkspace workspace = index.createWorkspace();
		workspace.purge( Collections.emptySet(), OperationSubmitter.blocking(), UnsupportedOperationBehavior.FAIL ).join();
		log( index, "Finished purge." );

		addToIndex( index, idStream );

		log( index, "Finished index initialization." );
	}

	private static void log(MappedIndex index, String message) {
		log.infof( "[%s] %s", index.name(), message );
	}

}
