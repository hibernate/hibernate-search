/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;


/**
 * @author Yoann Rodiere
 */
public class ForwardingElasticsearchWorkExecutionContext implements ElasticsearchWorkExecutionContext {

	private final ElasticsearchWorkExecutionContext delegate;

	public ForwardingElasticsearchWorkExecutionContext(ElasticsearchWorkExecutionContext delegate) {
		super();
		this.delegate = delegate;
	}

	@Override
	public ElasticsearchClient getClient() {
		return delegate.getClient();
	}

	@Override
	public GsonProvider getGsonProvider() {
		return delegate.getGsonProvider();
	}

	@Override
	public void setIndexDirty(URLEncodedString indexName) {
		delegate.setIndexDirty( indexName );
	}

	@Override
	public IndexingMonitor getBufferedIndexingMonitor(IndexingMonitor indexingMonitor) {
		return delegate.getBufferedIndexingMonitor( indexingMonitor );
	}

}
