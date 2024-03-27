/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessor;
import org.hibernate.search.backend.lucene.work.impl.IndexingWork;
import org.hibernate.search.engine.backend.orchestration.spi.BatchedWorkProcessor;
import org.hibernate.search.util.common.reporting.EventContext;

/**
 * A thread-safe component responsible for applying write works to an index writer.
 * <p>
 * Ported from Search 5's LuceneBackendQueueTask, in particular.
 */
public class LuceneBatchedWorkProcessor implements BatchedWorkProcessor {

	private final IndexAccessor indexAccessor;
	private final IndexAccessorWorkExecutionContext context;

	public LuceneBatchedWorkProcessor(EventContext eventContext,
			IndexAccessor indexAccessor) {
		this.indexAccessor = indexAccessor;
		this.context = new IndexAccessorWorkExecutionContext( eventContext, indexAccessor );
	}

	@Override
	public void beginBatch() {
		// Nothing to do
	}

	@Override
	public CompletableFuture<?> endBatch() {
		try {
			indexAccessor.commitOrDelay();
		}
		catch (RuntimeException e) {
			indexAccessor.cleanUpAfterFailure( e, "Commit after a batch of index works" );
			// The exception was reported to the failure handler, no need to propagate it.
		}
		// Everything was already executed, so just return a completed future.
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public void complete() {
		try {
			indexAccessor.commitOrDelay();
		}
		catch (RuntimeException e) {
			indexAccessor.cleanUpAfterFailure( e, "Commit after completion of all remaining index works" );
			// The exception was reported to the failure handler, no need to propagate it.
		}
	}

	public <T> T submit(IndexingWork<T> work) {
		try {
			return work.execute( context );
		}
		catch (RuntimeException e) {
			indexAccessor.cleanUpAfterFailure( e, work.getInfo() );
			throw e;
		}
	}

	// Note this may be called outside of a batch
	public void forceCommit() {
		try {
			indexAccessor.commit();
		}
		catch (RuntimeException e) {
			indexAccessor.cleanUpAfterFailure( e, "Commit after a set of index works" );
			throw e;
		}
	}

	// Note this may be called outside of a batch
	public void forceRefresh() {
		// In case of failure, just propagate the exception:
		// we don't expect a refresh failure to affect the writer and require a cleanup.
		indexAccessor.refresh();
	}

}
