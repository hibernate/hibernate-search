/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.work.impl.LuceneQueryWork;
import org.hibernate.search.util.impl.common.Futures;


/**
 * @author Yoann Rodiere
 * @author Guillaume Smet
 */
public class LuceneStubQueryWorkOrchestrator implements LuceneQueryWorkOrchestrator {

	private final LuceneStubQueryWorkExecutionContext context;

	// Protected by synchronization on updates
	private CompletableFuture<?> latestFuture = CompletableFuture.completedFuture( null );

	public LuceneStubQueryWorkOrchestrator() {
		this.context = new LuceneStubQueryWorkExecutionContext();
	}

	@Override
	public void close() {
		latestFuture.join();
	}

	@Override
	public synchronized <T> CompletableFuture<T> submit(LuceneQueryWork<T> work) {
		CompletableFuture<T> future = latestFuture.thenCompose( Futures.safeComposer(
				ignored -> work.execute( context )
		) );
		// Ignore errors from this work in future works and during close(): error handling is the client's responsibility.
		latestFuture = future.exceptionally( ignore -> null );
		return future;
	}

	@Override
	public synchronized CompletableFuture<?> submit(List<LuceneQueryWork<?>> works) {
		CompletableFuture<?> future = latestFuture;
		for ( LuceneQueryWork<?> work : works ) {
			future = future.thenCompose( Futures.safeComposer(
					ignored -> work.execute( context )
			) );
		}
		// Ignore errors from this work in future works and during close(): error handling is the client's responsibility.
		latestFuture = future.exceptionally( ignore -> null );
		return future;
	}
}
