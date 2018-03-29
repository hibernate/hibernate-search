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
public class StubLuceneQueryWorkOrchestrator implements LuceneQueryWorkOrchestrator {

	private final StubLuceneQueryWorkExecutionContext context;

	// Protected by synchronization on updates
	private CompletableFuture<?> latestFuture = CompletableFuture.completedFuture( null );

	public StubLuceneQueryWorkOrchestrator() {
		this.context = new StubLuceneQueryWorkExecutionContext();
	}

	@Override
	public void close() {
		latestFuture.join();
	}

	@Override
	public synchronized <T> CompletableFuture<T> submit(LuceneQueryWork<T> work) {
		// Ignore errors in unrelated changesets
		latestFuture = latestFuture.exceptionally( ignore -> null );
		CompletableFuture<T> future = latestFuture.thenCompose( Futures.safeComposer(
				ignored -> work.execute( context )
		) );
		latestFuture = future;
		return future;
	}

	@Override
	public synchronized CompletableFuture<?> submit(List<LuceneQueryWork<?>> works) {
		// Ignore errors in unrelated changesets
		latestFuture = latestFuture.exceptionally( ignore -> null );
		for ( LuceneQueryWork<?> work : works ) {
			latestFuture = latestFuture.thenCompose( Futures.safeComposer(
					ignored -> work.execute( context )
			) );
		}
		return latestFuture;
	}
}
