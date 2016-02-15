/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.testutil;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.client.impl.JestClient;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.impl.IndexNameNormalizer;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.TestResourceManager;
import org.hibernate.search.test.util.BackendTestHelper;

import io.searchbox.core.Count;
import io.searchbox.core.CountResult;

/**
 * {@link BackendTestHelper} implementation based on Elasticsearch.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchBackendTestHelper extends BackendTestHelper {

	private TestResourceManager resourceManager;

	public ElasticsearchBackendTestHelper(TestResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}

	@Override
	public int getNumberOfDocumentsInIndex(Class<?> entityType) {
		ServiceManager serviceManager = resourceManager.getExtendedSearchIntegrator().getServiceManager();

		IndexManager[] indexManagers = resourceManager.getExtendedSearchIntegrator()
				.getIndexBinding( entityType )
				.getIndexManagers();

		List<String> indexNames = new ArrayList<>( indexManagers.length );

		for ( IndexManager indexManager : indexManagers ) {
			indexNames.add( ( (ElasticsearchIndexManager)indexManager ).getActualIndexName() );
		}

		try ( ServiceReference<JestClient> client = serviceManager.requestReference( JestClient.class ) ) {
			Count request = new Count.Builder()
					.addIndex( indexNames )
					.addType( entityType.getName() )
					.build();

			CountResult response = client.get().executeRequest( request );

			return response.getCount().intValue();
		}
	}

	@Override
	public int getNumberOfDocumentsInIndex(String indexName) {
		ServiceManager serviceManager = resourceManager.getExtendedSearchIntegrator().getServiceManager();

		try ( ServiceReference<JestClient> client = serviceManager.requestReference( JestClient.class ) ) {
			Count request = new Count.Builder()
					.addIndex( IndexNameNormalizer.getElasticsearchIndexName( indexName ) )
					.build();

			CountResult response = client.get().executeRequest( request );

			return response.getCount().intValue();
		}
	}

	@Override
	public int getNumberOfDocumentsInIndexByQuery(String indexName, String fieldName, String value) {
		ServiceManager serviceManager = resourceManager.getExtendedSearchIntegrator().getServiceManager();
		String query = value.contains( "*" ) ? "wildcard" : "term";

		try ( ServiceReference<JestClient> client = serviceManager.requestReference( JestClient.class ) ) {
			Count request = new Count.Builder()
					.addIndex( IndexNameNormalizer.getElasticsearchIndexName( indexName ) )
					.query( "{ \"query\" : { \"" + query + "\" : { \"" + fieldName + "\" : \"" + value + "\" } } }" )
					.build();

			CountResult response = client.get().executeRequest( request );

			return response.getCount().intValue();
		}
	}
}
