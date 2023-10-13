/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import java.io.IOException;

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexSettingsTestUtils;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;

public class ElasticsearchTckBackendAccessor implements TckBackendAccessor {
	private final TestElasticsearchClient client;

	ElasticsearchTckBackendAccessor(TestElasticsearchClient client) {
		this.client = client;
	}

	@Override
	public void close() throws IOException {
		client.close();
	}

	@Override
	public void ensureIndexingOperationsFail(String indexName) {
		// There are many ways we could implement this method,
		// but the one below is the only one that:
		// 1. Will make even deletion operations fail (unlike deleting the index).
		// 2. Will work on AWS Elasticsearch Service (unlike _close).

		// Block read and write operations
		client.index( indexName )
				.settings().putDynamic( ElasticsearchIndexSettingsTestUtils.settingsEnableReadWrite( false ) );
	}

	@Override
	public void ensureFlushMergeRefreshOperationsFail(String indexName) {
		client.index( indexName ).delete();
	}
}
