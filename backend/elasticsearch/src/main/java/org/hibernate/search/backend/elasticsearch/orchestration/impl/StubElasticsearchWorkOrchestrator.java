/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.util.impl.common.Futures;


/**
 * A simplistic, badly performing orchestrator that just executes works one after another.
 *
 * @author Yoann Rodiere
 */
public class StubElasticsearchWorkOrchestrator implements ElasticsearchWorkOrchestrator {

	private final StubElasticsearchWorkExecutionContext context;

	// Protected by synchronization on updates
	private CompletableFuture<?> latestFuture = CompletableFuture.completedFuture( null );

	public StubElasticsearchWorkOrchestrator(ElasticsearchClient client) {
		this.context = new StubElasticsearchWorkExecutionContext( client );
	}

	@Override
	public void close() {
		latestFuture.join();
	}

	@Override
	public synchronized <T> CompletableFuture<T> submit(ElasticsearchWork<T> work) {
		CompletableFuture<T> future = latestFuture.thenCompose( Futures.safeComposer(
				ignored -> work.execute( context )
		) );
		// Ignore errors in future changesets and during close(): error handling is the client's responsibility.
		latestFuture = future.exceptionally( ignore -> null );
		return future;
	}

	@Override
	public synchronized CompletableFuture<?> submit(List<ElasticsearchWork<?>> works) {
		CompletableFuture<?> future = latestFuture;
		for ( ElasticsearchWork<?> work : works ) {
			future = future.thenCompose( Futures.safeComposer(
					ignored -> work.execute( context )
			) );
		}
		// Ignore errors in future changesets and during close(): error handling is the client's responsibility.
		latestFuture = future.exceptionally( ignore -> null );
		return future;
	}

}
