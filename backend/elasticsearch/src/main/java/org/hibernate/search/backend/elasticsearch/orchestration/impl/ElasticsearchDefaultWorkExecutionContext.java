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
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.spi.FailureContextImpl;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * The execution context used in orchestrators when we need to refresh indexes after a sequence of works.
 * <p>
 * This context is mutable and is not thread-safe.
 */
class ElasticsearchDefaultWorkExecutionContext implements ElasticsearchRefreshableWorkExecutionContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ElasticsearchClient client;

	private final GsonProvider gsonProvider;

	private final FailureHandler failureHandler;

	private final ElasticsearchWorkBuilderFactory workFactory;

	private final Set<URLEncodedString> indexesToRefresh = new HashSet<>();

	private final ElasticsearchWorkExecutionContext refreshExecutionContext;

	public ElasticsearchDefaultWorkExecutionContext(ElasticsearchClient client,
			GsonProvider gsonProvider, ElasticsearchWorkBuilderFactory workFactory,
			FailureHandler failureHandler) {
		this.client = client;
		this.gsonProvider = gsonProvider;
		this.failureHandler = failureHandler;
		this.workFactory = workFactory;
		this.refreshExecutionContext = new ElasticsearchImmutableWorkExecutionContext( client, gsonProvider );
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
	public void registerIndexToRefresh(URLEncodedString indexName) {
		indexesToRefresh.add( indexName );
	}

	@Override
	public CompletableFuture<Void> executePendingRefreshes() {
		CompletableFuture<Void> future = CompletableFuture.completedFuture( null );

		if ( !indexesToRefresh.isEmpty() ) {
			future = future.thenCompose( ignored -> refreshIndexes() )
					.thenRun( indexesToRefresh::clear );
		}

		return future;
	}

	private CompletableFuture<?> refreshIndexes() {
		if ( log.isTraceEnabled() ) {
			log.tracef( "Refreshing index(es) %s", indexesToRefresh );
		}

		RefreshWorkBuilder builder = workFactory.refresh();
		for ( URLEncodedString index : indexesToRefresh ) {
			builder.index( index );
		}
		ElasticsearchWork<?> work = builder.build();

		return work.execute( refreshExecutionContext )
				.handle( Futures.handler( (result, throwable) -> {
					if ( throwable != null ) {
						FailureContextImpl.Builder contextBuilder = new FailureContextImpl.Builder();
						contextBuilder.throwable( throwable );
						contextBuilder.failingOperation( work );
						failureHandler.handle( contextBuilder.build() );
					}
					return null;
				} ) );
	}

}
