/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;

class ElasticsearchIgnoreRefreshWorkExecutionContext implements ElasticsearchRefreshableWorkExecutionContext {

	private final ElasticsearchClient client;

	private final GsonProvider gsonProvider;

	public ElasticsearchIgnoreRefreshWorkExecutionContext(ElasticsearchClient client, GsonProvider gsonProvider) {
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
	public void registerIndexToRefresh(URLEncodedString indexName) {
		// Ignored
	}

	@Override
	public CompletableFuture<Void> executePendingRefreshes() {
		return CompletableFuture.completedFuture( null );
	}
}
