/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.elasticsearch.work.impl.builder.RefreshWorkBuilder;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.util.impl.Futures;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * The execution context used in {@link ElasticsearchWorkProcessor}
 * when multiple works are executed one after another.
 * <p>
 * This context is mutable and is not thread-safe.
 *
 * @author Yoann Rodiere
 */
class SequentialWorkExecutionContext implements ElasticsearchWorkExecutionContext {

	private static final Log log = LoggerFactory.make( Log.class );

	private final ElasticsearchClient client;

	private final GsonProvider gsonProvider;

	private final ElasticsearchWorkFactory workFactory;

	private final ElasticsearchWorkProcessor workProcessor;

	private final ErrorHandler errorHandler;

	/*
	 * We use buffers to avoid too many calls to the actual index monitor, which is potentially synchronized and hence
	 * may be a contention point.
	 */
	private final Map<IndexingMonitor, BufferedIndexingMonitor> bufferedIndexMonitors = new HashMap<>();

	private final Set<URLEncodedString> dirtyIndexes = new HashSet<>();

	public SequentialWorkExecutionContext(ElasticsearchClient client,
			GsonProvider gsonProvider, ElasticsearchWorkFactory workFactory,
			ElasticsearchWorkProcessor workProcessor,
			ErrorHandler errorHandler) {
		super();
		this.client = client;
		this.gsonProvider = gsonProvider;
		this.workFactory = workFactory;
		this.workProcessor = workProcessor;
		this.errorHandler = errorHandler;
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
	public IndexingMonitor getBufferedIndexingMonitor(IndexingMonitor originalMonitor) {
		return bufferedIndexMonitors.computeIfAbsent( originalMonitor, BufferedIndexingMonitor::new );
	}

	public void flush() {
		CompletableFuture<?> future = CompletableFuture.completedFuture( null );

		// Refresh dirty indexes
		if ( !dirtyIndexes.isEmpty() ) {
			future = future.thenCompose( ignored -> refreshDirtyIndexes() )
					.thenRun( () -> dirtyIndexes.clear() );
		}

		// Flush the indexing monitors
		future = future.thenRun( () -> {
				for ( BufferedIndexingMonitor buffer : bufferedIndexMonitors.values() ) {
					try {
						buffer.flush();
					}
					catch (RuntimeException e) {
						errorHandler.handleException( "Flushing an indexing monitor failed", e );
					}
				}
				bufferedIndexMonitors.clear();
		} );

		// Note: timeout is handled by the client, so this "join" will not last forever
		future.join();
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

	private static final class BufferedIndexingMonitor implements IndexingMonitor {

		private final IndexingMonitor delegate;

		private long documentsAdded = 0L;

		public BufferedIndexingMonitor(IndexingMonitor delegate) {
			super();
			this.delegate = delegate;
		}

		@Override
		public void documentsAdded(long increment) {
			documentsAdded += increment;
		}

		private void flush() {
			delegate.documentsAdded( documentsAdded );
			documentsAdded = 0L;
		}
	}

}
