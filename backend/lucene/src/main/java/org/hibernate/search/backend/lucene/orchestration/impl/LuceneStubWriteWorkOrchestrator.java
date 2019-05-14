/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;


/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
public class LuceneStubWriteWorkOrchestrator implements LuceneWriteWorkOrchestrator {

	private final LuceneStubWriteWorkExecutionContext context;

	// Protected by synchronization on updates
	private CompletableFuture<?> latestFuture = CompletableFuture.completedFuture( null );

	public LuceneStubWriteWorkOrchestrator(IndexWriter indexWriter) {
		this.context = new LuceneStubWriteWorkExecutionContext( indexWriter );
	}

	@Override
	public void close() {
		latestFuture.join();
	}

	@Override
	public synchronized <T> CompletableFuture<T> submit(LuceneWriteWork<T> work) {
		CompletableFuture<T> future = latestFuture.thenApply(
				// FIXME for now everything is blocking here, we need a non blocking wrapper on top of the IndexWriter
				ignored -> work.execute( context )
		);
		// Ignore errors from this work in future works and during close(): error handling is the client's responsibility.
		latestFuture = future.exceptionally( ignore -> null );
		return future;
	}

	@Override
	public synchronized CompletableFuture<?> submit(List<LuceneWriteWork<?>> works) {
		CompletableFuture<?> future = latestFuture;
		for ( LuceneWriteWork<?> work : works ) {
			future = future.thenApply(
					// FIXME for now everything is blocking here, we need a non blocking wrapper on top of the IndexWriter
					ignored -> work.execute( context )
			);
		}
		// Ignore errors from this work in future works and during close(): error handling is the client's responsibility.
		latestFuture = future.exceptionally( ignore -> null );
		return future;
	}
}
