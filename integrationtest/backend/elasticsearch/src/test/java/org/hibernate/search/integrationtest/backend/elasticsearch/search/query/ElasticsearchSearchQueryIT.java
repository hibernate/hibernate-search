/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.search.query;

import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingScope;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Test the content of generated Elasticsearch search queries.
 */
public class ElasticsearchSearchQueryIT {

	private static final String BACKEND_NAME = "myElasticsearchBackend";
	private static final String INDEX_NAME = "indexname";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ElasticsearchClientSpy clientSpy = new ElasticsearchClientSpy();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration( BACKEND_NAME )
				.withBackendProperty(
						BACKEND_NAME, ElasticsearchBackendSpiSettings.CLIENT_FACTORY, clientSpy.getFactory()
				)
				.withIndex(
						INDEX_NAME,
						ctx -> new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	@Test
	public void projection_sourceFiltering() {
		StubMappingScope scope = indexManager.createScope();

		SearchQuery<Object> query = scope.query()
				.asProjection( f -> f.field( "string" ) )
				.predicate( f -> f.matchAll() )
				.toQuery();

		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( URLEncodedString.fromString( INDEX_NAME ) )
						.pathComponent( Paths._SEARCH )
						.body( new Gson().fromJson( "{'_source':['string']}", JsonObject.class ) )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		query.fetch();
	}

	@Test
	public void routing() {
		StubMappingScope scope = indexManager.createScope();

		String routingKey = "someRoutingKey";

		SearchQuery<?> query = scope.query()
				.predicate( f -> f.matchAll() )
				.routing( routingKey )
				.toQuery();

		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( URLEncodedString.fromString( INDEX_NAME ) )
						.pathComponent( Paths._SEARCH )
						.body( new JsonObject() ) // We don't care about the payload
						.param( "routing", routingKey )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		query.fetch();
	}

	@SuppressWarnings("unused")
	private static class IndexMapping {
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<String> string;

		IndexMapping(IndexSchemaElement root) {
			integer = root.field(
					"integer",
					f -> f.asInteger().projectable( Projectable.YES )
			)
					.toReference();
			string = root.field(
					"string",
					f -> f.asString().projectable( Projectable.YES )
			)
					.toReference();
		}
	}


}
