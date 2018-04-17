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
import org.hibernate.search.backend.lucene.work.impl.LuceneIndexWork;
import org.hibernate.search.util.impl.common.Futures;


/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
public class StubLuceneIndexWorkOrchestrator implements LuceneIndexWorkOrchestrator {

	private final StubLuceneIndexWorkExecutionContext context;

	// Protected by synchronization on updates
	private CompletableFuture<?> latestFuture = CompletableFuture.completedFuture( null );

	public StubLuceneIndexWorkOrchestrator(IndexWriter indexWriter) {
		this.context = new StubLuceneIndexWorkExecutionContext( indexWriter );
	}

	@Override
	public void close() {
		latestFuture.join();
	}

	@Override
	public synchronized <T> CompletableFuture<T> submit(LuceneIndexWork<T> work) {
		CompletableFuture<T> future = latestFuture.thenCompose( Futures.safeComposer(
				ignored -> work.execute( context )
		) );
		// Ignore errors in future changesets and during close(): error handling is the client's responsibility.
		latestFuture = future.exceptionally( ignore -> null );
		return future;
	}

	@Override
	public synchronized CompletableFuture<?> submit(List<LuceneIndexWork<?>> works) {
		CompletableFuture<?> future = latestFuture;
		for ( LuceneIndexWork<?> work : works ) {
			future = future.thenCompose( Futures.safeComposer(
					ignored -> work.execute( context )
			) );
		}
		// Ignore errors in future changesets and during close(): error handling is the client's responsibility.
		latestFuture = future.exceptionally( ignore -> null );
		return future;
	}
}
