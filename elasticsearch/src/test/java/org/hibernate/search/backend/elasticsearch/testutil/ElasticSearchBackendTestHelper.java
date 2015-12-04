/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.testutil;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.client.impl.JestClientReference;
import org.hibernate.search.backend.elasticsearch.impl.ElasticSearchIndexManager;
import org.hibernate.search.backend.elasticsearch.impl.IndexNameNormalizer;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.TestResourceManager;
import org.hibernate.search.test.util.BackendTestHelper;

import io.searchbox.core.Count;
import io.searchbox.core.CountResult;

/**
 * {@link BackendTestHelper} implementation based on ElasticSearch.
 *
 * @author Gunnar Morling
 */
public class ElasticSearchBackendTestHelper extends BackendTestHelper {

	private TestResourceManager resourceManager;

	public ElasticSearchBackendTestHelper(TestResourceManager resourceManager) {
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
			indexNames.add( ( (ElasticSearchIndexManager)indexManager ).getActualIndexName() );
		}

		try (JestClientReference client = new JestClientReference( serviceManager ) ) {
			Count request = new Count.Builder()
					.addIndex( indexNames )
					.addType( entityType.getName() )
					.build();

			CountResult response = client.executeRequest( request );

			return response.getCount().intValue();
		}
	}

	@Override
	public int getNumberOfDocumentsInIndex(String indexName) {
		ServiceManager serviceManager = resourceManager.getExtendedSearchIntegrator().getServiceManager();

		try (JestClientReference client = new JestClientReference( serviceManager ) ) {
			Count request = new Count.Builder()
					.addIndex( IndexNameNormalizer.getElasticSearchIndexName( indexName ) )
					.build();

			CountResult response = client.executeRequest( request );

			return response.getCount().intValue();
		}
	}

	@Override
	public int getNumberOfDocumentsInIndexByQuery(String indexName, String fieldName, String value) {
		ServiceManager serviceManager = resourceManager.getExtendedSearchIntegrator().getServiceManager();
		String query = value.contains( "*" ) ? "wildcard" : "term";

		try (JestClientReference client = new JestClientReference( serviceManager ) ) {
			Count request = new Count.Builder()
					.addIndex( IndexNameNormalizer.getElasticSearchIndexName( indexName ) )
					.query( "{ \"query\" : { \"" + query + "\" : { \"" + fieldName + "\" : \"" + value + "\" } } }" )
					.build();

			CountResult response = client.executeRequest( request );

			return response.getCount().intValue();
		}
	}
}
