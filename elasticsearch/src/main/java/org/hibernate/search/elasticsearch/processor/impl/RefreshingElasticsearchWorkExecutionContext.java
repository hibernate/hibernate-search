/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.builder.RefreshWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.util.impl.Futures;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * The execution context used in {@link ElasticsearchWorkProcessor}
 * when there's a need for indexing monitor buffering *and* for dirty index refresh.
 * <p>
 * This context is mutable and is not thread-safe.
 *
 * @author Yoann Rodiere
 */
class RefreshingElasticsearchWorkExecutionContext extends IndexMonitorBufferingElasticsearchWorkExecutionContext {

	private static final Log log = LoggerFactory.make( Log.class );

	private final ElasticsearchWorkFactory workFactory;

	private final ElasticsearchWorkProcessor workProcessor;

	private final Set<URLEncodedString> dirtyIndexes = new HashSet<>();

	public RefreshingElasticsearchWorkExecutionContext(ElasticsearchClient client,
			GsonProvider gsonProvider, ElasticsearchWorkFactory workFactory,
			ElasticsearchWorkProcessor workProcessor,
			ErrorHandler errorHandler) {
		super( client, gsonProvider, errorHandler );
		this.workFactory = workFactory;
		this.workProcessor = workProcessor;
	}

	@Override
	public void setIndexDirty(URLEncodedString indexName) {
		dirtyIndexes.add( indexName );
	}

	@Override
	public CompletableFuture<Void> flush() {
		CompletableFuture<Void> future = super.flush();

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

		return workProcessor.executeAsyncUnsafe( work )
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
