/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import java.io.IOException;

import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;

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
	public void ensureIndexOperationsFail(String indexName) {
		client.index( indexName ).delete();
		/*
		 * Automatic index creation might get in the way and make operation succeed
		 * even though the index doesn't exist, so we disable it.
		 * We never rely on automatic index creation,
		 * so leaving this setting in place afterwards is not a problem.
		 */
		client.clusterSettings( "action.auto_create_index" ).put( "false" );
	}
}
