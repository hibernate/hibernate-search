/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.builder.impl.RefreshWorkBuilder;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.engine.common.spi.ErrorHandler;
import org.hibernate.search.util.impl.common.Futures;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * The execution context used in {@link ElasticsearchWorkOrchestratorFactory}
 * when there's a need for indexing monitor buffering *and* for dirty index refresh.
 * <p>
 * This context is mutable and is not thread-safe.
 *
 * @author Yoann Rodiere
 */
class ElasticsearchRefreshingWorkExecutionContext implements ElasticsearchFlushableWorkExecutionContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchClient client;

	private final GsonProvider gsonProvider;

	private final ErrorHandler errorHandler;

	private final ElasticsearchWorkBuilderFactory workFactory;

	private final Set<URLEncodedString> dirtyIndexes = new HashSet<>();

	private final ElasticsearchWorkExecutionContext flushExecutionContext;

	public ElasticsearchRefreshingWorkExecutionContext(ElasticsearchClient client,
			GsonProvider gsonProvider, ElasticsearchWorkBuilderFactory workFactory,
			ErrorHandler errorHandler) {
		this.client = client;
		this.gsonProvider = gsonProvider;
		this.errorHandler = errorHandler;
		this.workFactory = workFactory;
		this.flushExecutionContext = new ElasticsearchImmutableWorkExecutionContext( client, gsonProvider );
	}

	@Override
	public ElasticsearchClient getClient() {
		return client;
	}

	@Override
	public GsonProvider getGsonProvider() {
		return gsonProvider;
	}

	@Override
	public void setIndexDirty(URLEncodedString indexName) {
		dirtyIndexes.add( indexName );
	}

	@Override
	public CompletableFuture<Void> flush() {
		CompletableFuture<Void> future = CompletableFuture.completedFuture( null );

		// Refresh dirty indexes
		if ( !dirtyIndexes.isEmpty() ) {
			future = future.thenCompose( ignored -> refreshDirtyIndexes() )
					.thenRun( () -> dirtyIndexes.clear() );
		}

		return future;
	}

	private CompletableFuture<?> refreshDirtyIndexes() {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Refreshing index(es) %s", dirtyIndexes );
		}

		RefreshWorkBuilder builder = workFactory.refresh();
		for ( URLEncodedString index : dirtyIndexes ) {
			builder.index( index );
		}
		ElasticsearchWork<?> work = builder.build();

		return work.execute( flushExecutionContext )
				.handle( Futures.handler(
						(result, throwable) -> {
							if ( throwable != null ) {
								errorHandler.handleException( "Refresh failed", throwable );
							}
							return null;
						}
				) );
	}

}
