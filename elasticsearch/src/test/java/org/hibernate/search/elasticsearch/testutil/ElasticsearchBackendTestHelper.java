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

import org.elasticsearch.client.Response;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexNameNormalizer;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.elasticsearch.processor.impl.ElasticsearchWorkProcessor;
import org.hibernate.search.elasticsearch.util.impl.gson.JsonAccessor;
import org.hibernate.search.elasticsearch.work.impl.DefaultElasticsearchRequestSuccessAssessor;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.elasticsearch.work.impl.SimpleElasticsearchWork;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.test.TestResourceManager;
import org.hibernate.search.test.util.BackendTestHelper;

import com.google.gson.JsonObject;

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
			CountWork work = new CountWork.Builder( indexNames )
					.type( entityType.getName() )
					.build();
			return processor.get().executeSyncUnsafe( work );
		}
	}

	@Override
	public int getNumberOfDocumentsInIndex(String indexName) {
		ServiceManager serviceManager = resourceManager.getExtendedSearchIntegrator().getServiceManager();

		try ( ServiceReference<ElasticsearchWorkProcessor> processor =
				serviceManager.requestReference( ElasticsearchWorkProcessor.class ) ) {
			CountWork work = new CountWork.Builder( ElasticsearchIndexNameNormalizer.getElasticsearchIndexName( indexName ) )
					.build();
			return processor.get().executeSyncUnsafe( work );
		}
	}

	@Override
	public int getNumberOfDocumentsInIndexByQuery(String indexName, String fieldName, String value) {
		ServiceManager serviceManager = resourceManager.getExtendedSearchIntegrator().getServiceManager();
		String query = value.contains( "*" ) ? "wildcard" : "term";

		try ( ServiceReference<ElasticsearchWorkProcessor> processor =
				serviceManager.requestReference( ElasticsearchWorkProcessor.class ) ) {
			CountWork work = new CountWork.Builder( ElasticsearchIndexNameNormalizer.getElasticsearchIndexName( indexName ) )
					.query( JsonBuilder.object()
							.add( "query", JsonBuilder.object()
									.add( query, JsonBuilder.object()
											.addProperty( fieldName, value )
									)
							).build()
					)
					.build();
			return processor.get().executeSyncUnsafe( work );
		}
	}

	private static class CountWork extends SimpleElasticsearchWork<Integer> {

		private static final JsonAccessor COUNT_ACCESSOR = JsonAccessor.root().property( "count" );

		protected CountWork(Builder builder) {
			super( builder );
		}

		@Override
		protected Integer generateResult(ElasticsearchWorkExecutionContext context, Response response, JsonObject parsedResponseBody) {
			return COUNT_ACCESSOR.get( parsedResponseBody ).getAsInt();
		}

		private static class Builder extends SimpleElasticsearchWork.Builder<Builder> {

			private final List<String> indexNames = new ArrayList<>();
			private final List<String> typeNames = new ArrayList<>();
			private JsonObject query;

			public Builder(String indexName) {
				this( Collections.singletonList( indexName ) );
			}

			public Builder(Collection<String> indexNames) {
				super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
				this.indexNames.addAll( indexNames );
			}

			public Builder type(String type) {
				this.typeNames.add( type );
				return this;
			}

			public Builder query(JsonObject query) {
				this.query = query;
				return this;
			}

			@Override
			protected ElasticsearchRequest buildRequest() {
				ElasticsearchRequest.Builder builder =
						ElasticsearchRequest.get()
						.multiValuedPathComponent( indexNames );

				if ( !typeNames.isEmpty() ) {
					builder.multiValuedPathComponent( typeNames );
				}

				builder.pathComponent( "_count" );

				if ( query != null ) {
					builder.body( query );
				}

				return builder.build();
			}

			@Override
			public CountWork build() {
				return new CountWork( this );
			}

		}
	}
}
