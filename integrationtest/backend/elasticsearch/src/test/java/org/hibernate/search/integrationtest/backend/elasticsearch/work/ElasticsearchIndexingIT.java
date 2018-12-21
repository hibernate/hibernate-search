/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.work;

import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.backend.elasticsearch.cfg.spi.SearchBackendElasticsearchSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.index.impl.ElasticsearchIndexManagerBuilder;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientMock;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.gson.JsonObject;

/**
 * Test the content of generated Elasticsearch indexing requests.
 */
public class ElasticsearchIndexingIT {

	private static final String BACKEND_NAME = "myElasticsearchBackend";
	private static final String INDEX_NAME = "indexname";
	private static final String TYPE_NAME = ElasticsearchIndexManagerBuilder.TYPE_NAME;

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ElasticsearchClientMock clientMock = new ElasticsearchClientMock();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexAccessors indexAccessors;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration( BACKEND_NAME )
				.withBackendProperty(
						BACKEND_NAME, SearchBackendElasticsearchSpiSettings.CLIENT_FACTORY, clientMock.getFactory()
				)
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	@Test
	public void addUpdateDelete_routing() {
		String routingKey = "someRoutingKey";
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();

		workPlan.add( referenceProvider( "1", routingKey ), document -> {
			indexAccessors.string.write( document, "text1" );
		} );
		clientMock.expectNext(
				ElasticsearchRequest.put()
						.pathComponent( URLEncodedString.fromString( INDEX_NAME ) )
						.pathComponent( URLEncodedString.fromString( TYPE_NAME ) )
						.pathComponent( URLEncodedString.fromString( "1" ) )
						.body( new JsonObject() ) // We don't care about the payload
						.param( "routing", routingKey )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		workPlan.execute().join();
		clientMock.verifyExpectationsMet();

		workPlan.update( referenceProvider( "1", routingKey ), document -> {
			indexAccessors.string.write( document, "text2" );
		} );
		clientMock.expectNext(
				ElasticsearchRequest.put()
						.pathComponent( URLEncodedString.fromString( INDEX_NAME ) )
						.pathComponent( URLEncodedString.fromString( TYPE_NAME ) )
						.pathComponent( URLEncodedString.fromString( "1" ) )
						.body( new JsonObject() ) // We don't care about the payload
						.param( "routing", routingKey )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		workPlan.execute().join();
		clientMock.verifyExpectationsMet();

		workPlan.delete( referenceProvider( "1", routingKey ) );
		clientMock.expectNext(
				ElasticsearchRequest.delete()
						.pathComponent( URLEncodedString.fromString( INDEX_NAME ) )
						.pathComponent( URLEncodedString.fromString( TYPE_NAME ) )
						.pathComponent( URLEncodedString.fromString( "1" ) )
						.param( "routing", routingKey )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		workPlan.execute().join();
		clientMock.verifyExpectationsMet();
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> string;

		IndexAccessors(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().toIndexFieldType() )
					.createAccessor();
		}
	}


}
