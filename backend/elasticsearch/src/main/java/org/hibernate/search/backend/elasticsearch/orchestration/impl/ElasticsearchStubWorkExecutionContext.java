/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClient;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchStubWorkExecutionContext implements ElasticsearchWorkExecutionContext {

	private final ElasticsearchClient client;
	private final GsonProvider gsonProvider;

	public ElasticsearchStubWorkExecutionContext(ElasticsearchClient client, GsonProvider gsonProvider) {
		this.client = client;
		this.gsonProvider = gsonProvider;
	}

	@Override
	public ElasticsearchClient getClient() {
		return client;
	}

	@Override
	public void setIndexDirty(URLEncodedString indexName) {
		// TODO scope execution context per request
	}

	@Override
	public GsonProvider getGsonProvider() {
		return gsonProvider;
	}
}
