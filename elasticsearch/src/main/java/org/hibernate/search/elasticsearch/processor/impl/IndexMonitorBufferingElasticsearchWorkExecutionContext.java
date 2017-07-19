/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.exception.ErrorHandler;

/**
 * The execution context used in {@link ElasticsearchWorkProcessor}
 * when there's a need for indexing monitor buffering but not for dirty index refresh.
 * <p>
 * This context is mutable and is not thread-safe.
 *
 * @author Yoann Rodiere
 */
class IndexMonitorBufferingElasticsearchWorkExecutionContext implements FlushableElasticsearchWorkExecutionContext {

	private final ElasticsearchClient client;

	private final GsonProvider gsonProvider;

	protected final ErrorHandler errorHandler;

	/*
	 * We use buffers to avoid too many calls to the actual index monitor, which is potentially synchronized and hence
	 * may be a contention point.
	 */
	private final Map<IndexingMonitor, BufferedIndexingMonitor> bufferedIndexMonitors = new HashMap<>();

	public IndexMonitorBufferingElasticsearchWorkExecutionContext(ElasticsearchClient client,
			GsonProvider gsonProvider, ErrorHandler errorHandler) {
		super();
		this.client = client;
		this.gsonProvider = gsonProvider;
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
		// Ignored
	}

	@Override
	public IndexingMonitor getBufferedIndexingMonitor(IndexingMonitor originalMonitor) {
		return bufferedIndexMonitors.computeIfAbsent( originalMonitor, BufferedIndexingMonitor::new );
	}

	@Override
	public CompletableFuture<Void> flush() {
		CompletableFuture<?> future = CompletableFuture.completedFuture( null );

		// Flush the indexing monitors
		return future.thenRun( () -> {
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
