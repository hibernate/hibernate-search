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
import java.util.Set;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.elasticsearch.client.impl.Paths;
import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexManager;
import org.hibernate.search.elasticsearch.impl.ElasticsearchIndexNameNormalizer;
import org.hibernate.search.elasticsearch.impl.ElasticsearchService;
import org.hibernate.search.elasticsearch.impl.JsonBuilder;
import org.hibernate.search.elasticsearch.work.impl.DefaultElasticsearchRequestSuccessAssessor;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.elasticsearch.work.impl.SimpleElasticsearchWork;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.ServiceReference;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.IndexedTypeIdentifier;
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
	public int getNumberOfDocumentsInIndex(IndexedTypeIdentifier entityType) {
		ServiceManager serviceManager = resourceManager.getExtendedSearchIntegrator().getServiceManager();

		Set<IndexManager> indexManagers = resourceManager.getExtendedSearchIntegrator()
				.getIndexBinding( entityType )
				.getIndexManagerSelector().all();

		List<URLEncodedString> indexNames = new ArrayList<>( indexManagers.size() );

		for ( IndexManager indexManager : indexManagers ) {
			indexNames.add( URLEncodedString.fromString( ( (ElasticsearchIndexManager)indexManager ).getActualIndexName() ) );
		}

		try ( ServiceReference<ElasticsearchService> esService =
				serviceManager.requestReference( ElasticsearchService.class ) ) {
			CountWork work = new CountWork.Builder( indexNames )
					.type( URLEncodedString.fromString( entityType.getName() ) )
					.build();
			return esService.get().getWorkProcessor().executeSyncUnsafe( work );
		}
	}

	@Override
	public int getNumberOfDocumentsInIndex(String indexName) {
		ServiceManager serviceManager = resourceManager.getExtendedSearchIntegrator().getServiceManager();

		try ( ServiceReference<ElasticsearchService> esService =
				serviceManager.requestReference( ElasticsearchService.class ) ) {
			CountWork work = new CountWork.Builder( ElasticsearchIndexNameNormalizer.getElasticsearchIndexName( indexName ) )
					.build();
			return esService.get().getWorkProcessor().executeSyncUnsafe( work );
		}
	}

	@Override
	public int getNumberOfDocumentsInIndexByQuery(String indexName, String fieldName, String value) {
		ServiceManager serviceManager = resourceManager.getExtendedSearchIntegrator().getServiceManager();
		String query = value.contains( "*" ) ? "wildcard" : "term";

		try ( ServiceReference<ElasticsearchService> esService =
				serviceManager.requestReference( ElasticsearchService.class ) ) {
			CountWork work = new CountWork.Builder( ElasticsearchIndexNameNormalizer.getElasticsearchIndexName( indexName ) )
					.query( JsonBuilder.object()
							.add( "query", JsonBuilder.object()
									.add( query, JsonBuilder.object()
											.addProperty( fieldName, value )
									)
							).build()
					)
					.build();
			return esService.get().getWorkProcessor().executeSyncUnsafe( work );
		}
	}

	private static class CountWork extends SimpleElasticsearchWork<Integer> {

		private static final JsonAccessor<Integer> COUNT_ACCESSOR = JsonAccessor.root().property( "count" ).asInteger();

		protected CountWork(Builder builder) {
			super( builder );
		}

		@Override
		protected Integer generateResult(ElasticsearchWorkExecutionContext context, ElasticsearchResponse response) {
			JsonObject body = response.getBody();
			return COUNT_ACCESSOR.get( body ).get();
		}

		private static class Builder extends SimpleElasticsearchWork.Builder<Builder> {

			private final List<URLEncodedString> indexNames = new ArrayList<>();
			private final List<URLEncodedString> typeNames = new ArrayList<>();
			private JsonObject query;

			public Builder(URLEncodedString indexName) {
				this( Collections.singletonList( indexName ) );
			}

			public Builder(Collection<URLEncodedString> indexNames) {
				super( null, DefaultElasticsearchRequestSuccessAssessor.INSTANCE );
				this.indexNames.addAll( indexNames );
			}

			public Builder type(URLEncodedString type) {
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

				builder.pathComponent( Paths._COUNT );

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
