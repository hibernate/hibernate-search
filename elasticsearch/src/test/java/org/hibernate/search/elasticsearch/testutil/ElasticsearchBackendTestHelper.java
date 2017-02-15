/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.testutil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexNameNormalizer;
import org.hibernate.search.elasticsearch.processor.impl.ElasticsearchWorkProcessor;
import org.hibernate.search.elasticsearch.work.impl.DefaultElasticsearchRequestResultAssessor;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.NoopElasticsearchWorkSuccessReporter;
import org.hibernate.search.elasticsearch.work.impl.SimpleElasticsearchWork;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.TestResourceManager;
import org.hibernate.search.test.util.BackendTestHelper;

import io.searchbox.action.Action;
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

		try ( ServiceReference<ElasticsearchWorkProcessor> processor =
				serviceManager.requestReference( ElasticsearchWorkProcessor.class ) ) {
			ElasticsearchWork<CountResult> work = new CountWork.Builder( indexNames )
					.type( entityType.getName() )
					.build();
			CountResult response = processor.get().executeSyncUnsafe( work );

			return response.getCount().intValue();
		}
	}

	@Override
	public int getNumberOfDocumentsInIndex(String indexName) {
		ServiceManager serviceManager = resourceManager.getExtendedSearchIntegrator().getServiceManager();

		try ( ServiceReference<ElasticsearchWorkProcessor> processor =
				serviceManager.requestReference( ElasticsearchWorkProcessor.class ) ) {
			ElasticsearchWork<CountResult> work = new CountWork.Builder( ElasticsearchIndexNameNormalizer.getElasticsearchIndexName( indexName ) )
					.build();
			CountResult response = processor.get().executeSyncUnsafe( work );

			return response.getCount().intValue();
		}
	}

	@Override
	public int getNumberOfDocumentsInIndexByQuery(String indexName, String fieldName, String value) {
		ServiceManager serviceManager = resourceManager.getExtendedSearchIntegrator().getServiceManager();
		String query = value.contains( "*" ) ? "wildcard" : "term";

		try ( ServiceReference<ElasticsearchWorkProcessor> processor =
				serviceManager.requestReference( ElasticsearchWorkProcessor.class ) ) {
			ElasticsearchWork<CountResult> work = new CountWork.Builder( ElasticsearchIndexNameNormalizer.getElasticsearchIndexName( indexName ) )
					.query( "{ \"query\" : { \"" + query + "\" : { \"" + fieldName + "\" : \"" + value + "\" } } }" )
					.build();
			CountResult response = processor.get().executeSyncUnsafe( work );

			return response.getCount().intValue();
		}
	}

	private static class CountWork extends SimpleElasticsearchWork<CountResult> {

		protected CountWork(Builder builder) {
			super( builder );
		}

		private static class Builder extends SimpleElasticsearchWork.Builder<Builder, CountResult> {

			private final Count.Builder jestBuilder;

			public Builder(String indexName) {
				this( Collections.singletonList( indexName ) );
			}

			public Builder(Collection<String> indexNames) {
				super( null, DefaultElasticsearchRequestResultAssessor.INSTANCE, NoopElasticsearchWorkSuccessReporter.INSTANCE );
				this.jestBuilder = new Count.Builder().addIndex( indexNames );
			}

			public Builder type(String type) {
				this.jestBuilder.addType( type );
				return this;
			}

			public Builder query(String query) {
				this.jestBuilder.query( query );
				return this;
			}

			@Override
			protected Action<CountResult> buildAction() {
				return jestBuilder.build();
			}

			@Override
			public CountWork build() {
				return new CountWork( this );
			}

		}
	}
}
