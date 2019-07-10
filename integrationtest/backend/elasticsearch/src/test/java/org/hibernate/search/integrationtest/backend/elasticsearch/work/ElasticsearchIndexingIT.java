/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.work;

import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.dialect.ElasticsearchTestDialect;
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

	private final ElasticsearchTestDialect dialect = ElasticsearchTestDialect.get();

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ElasticsearchClientSpy clientSpy = new ElasticsearchClientSpy();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.start( BACKEND_NAME )
				.withBackendProperty(
						BACKEND_NAME, ElasticsearchBackendSpiSettings.CLIENT_FACTORY, clientSpy.getFactory()
				)
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	@Test
	public void addUpdateDelete_routing() {
		String routingKey = "someRoutingKey";
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();

		workPlan.add( referenceProvider( "1", routingKey ), document -> {
			document.addValue( indexMapping.string, "text1" );
		} );
		clientSpy.expectNext(
				ElasticsearchRequest.put()
						.pathComponent( URLEncodedString.fromString( INDEX_NAME ) )
						.pathComponent( dialect.getTypeKeywordForNonMappingApi() )
						.pathComponent( URLEncodedString.fromString( "1" ) )
						.body( new JsonObject() ) // We don't care about the payload
						.param( "routing", routingKey )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		workPlan.execute().join();
		clientSpy.verifyExpectationsMet();

		workPlan.update( referenceProvider( "1", routingKey ), document -> {
			document.addValue( indexMapping.string, "text2" );
		} );
		clientSpy.expectNext(
				ElasticsearchRequest.put()
						.pathComponent( URLEncodedString.fromString( INDEX_NAME ) )
						.pathComponent( dialect.getTypeKeywordForNonMappingApi() )
						.pathComponent( URLEncodedString.fromString( "1" ) )
						.body( new JsonObject() ) // We don't care about the payload
						.param( "routing", routingKey )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		workPlan.execute().join();
		clientSpy.verifyExpectationsMet();

		workPlan.delete( referenceProvider( "1", routingKey ) );
		clientSpy.expectNext(
				ElasticsearchRequest.delete()
						.pathComponent( URLEncodedString.fromString( INDEX_NAME ) )
						.pathComponent( dialect.getTypeKeywordForNonMappingApi() )
						.pathComponent( URLEncodedString.fromString( "1" ) )
						.param( "routing", routingKey )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		workPlan.execute().join();
		clientSpy.verifyExpectationsMet();
	}

	private static class IndexMapping {
		final IndexFieldReference<String> string;

		IndexMapping(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() )
					.toReference();
		}
	}


}
