/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.exception.AssertionFailure;


/**
 * The execution context used in {@link ElasticsearchWorkProcessor}
 * when the context must be shared by multiple threads.
 * <p>
 * This context is immutable and thread-safe, but doesn't support
 * {@link #getBufferedIndexingMonitor(IndexingMonitor)} nor {@link #setIndexDirty(String)}.
 *
 * @author Yoann Rodiere
 */
class ImmutableElasticsearchWorkExecutionContext implements ElasticsearchWorkExecutionContext {

	private final ElasticsearchClient client;
	private final GsonProvider gsonProvider;

	public ImmutableElasticsearchWorkExecutionContext(ElasticsearchClient client, GsonProvider gsonProvider) {
		super();
		this.client = client;
		this.gsonProvider = gsonProvider;
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
		throw new AssertionFailure( "Unexpected dirty index with a default context."
				+ " Works that may alter index content should be executed"
				+ " through the " + ElasticsearchWorkProcessor.class.getName()
				+ ", using an appropriate context." );
	}

	@Override
	public IndexingMonitor getBufferedIndexingMonitor(IndexingMonitor indexingMonitor) {
		throw new AssertionFailure( "Unexpected indexing monitor request with a default context."
				+ " Works that may alter index content should be executed"
				+ " through the " + ElasticsearchWorkProcessor.class.getName()
				+ ", using an appropriate context." );
	}

}
