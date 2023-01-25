/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.work;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultWriteAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.encodeName;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.impl.ElasticsearchBackendImplSettings;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.StubSingleIndexLayoutStrategy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Test the content of generated Elasticsearch indexing requests.
 */
@RunWith(Parameterized.class)
public class ElasticsearchIndexingIT {

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Parameterized.Parameters(name = "IndexLayoutStrategy = {0}")
	public static Object[][] configurations() {
		return new Object[][] {
				{ null, defaultWriteAlias( index.name() ) },
				{ "no-alias", encodeName( index.name() ) },
				{ new StubSingleIndexLayoutStrategy( "custom-write", "custom-read" ), encodeName( "custom-write" ) }
		};
	}

	private final ElasticsearchTestDialect dialect = ElasticsearchTestDialect.get();

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ElasticsearchClientSpy clientSpy = new ElasticsearchClientSpy();

	private final Object layoutStrategy;
	private final URLEncodedString writeName;

	public ElasticsearchIndexingIT(Object layoutStrategy, URLEncodedString writeName) {
		this.layoutStrategy = layoutStrategy;
		this.writeName = writeName;
	}

	@Before
	public void setup() {
		setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendImplSettings.CLIENT_FACTORY, clientSpy.factoryReference()
				)
				.withBackendProperty(
						ElasticsearchBackendSettings.LAYOUT_STRATEGY, layoutStrategy
				)
				.withIndex( index )
				.setup();
	}

	@Test
	public void addUpdateDelete_noRouting() {
		Gson gson = new Gson();

		IndexIndexingPlan plan = index.createIndexingPlan();

		plan.add( referenceProvider( "1" ), document -> {
			document.addValue( index.binding().string, "text1" );
		} );
		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( Paths._BULK )
						.body( gson.fromJson( "{'index':{'_index': '" + writeName + "',"
								+ dialect.getTypeNameForMappingAndBulkApi().map( name -> "'_type': '" + name + "'," ).orElse( "" )
								+ "'_id': '1'}}", JsonObject.class ) )
						.body( new JsonObject() ) // We don't care about the document
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		plan.execute( OperationSubmitter.BLOCKING ).join();
		clientSpy.verifyExpectationsMet();

		plan.addOrUpdate( referenceProvider( "1" ), document -> {
			document.addValue( index.binding().string, "text2" );
		} );
		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( Paths._BULK )
						.body( gson.fromJson( "{'index':{'_index': '" + writeName + "',"
								+ dialect.getTypeNameForMappingAndBulkApi().map( name -> "'_type': '" + name + "'," ).orElse( "" )
								+ "'_id': '1'}}", JsonObject.class ) )
						.body( new JsonObject() ) // We don't care about the document
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		plan.execute( OperationSubmitter.BLOCKING ).join();
		clientSpy.verifyExpectationsMet();

		plan.delete( referenceProvider( "1" ) );
		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( Paths._BULK )
						.body( gson.fromJson( "{'delete':{'_index': '" + writeName + "',"
								+ dialect.getTypeNameForMappingAndBulkApi().map( name -> "'_type': '" + name + "'," ).orElse( "" )
								+ "'_id': '1'}}", JsonObject.class ) )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		plan.execute( OperationSubmitter.BLOCKING ).join();
		clientSpy.verifyExpectationsMet();
	}

	@Test
	public void addUpdateDelete_routing() {
		Gson gson = new Gson();

		String routingKey = "someRoutingKey";
		IndexIndexingPlan plan = index.createIndexingPlan();

		plan.add( referenceProvider( "1", routingKey ), document -> {
			document.addValue( index.binding().string, "text1" );
		} );
		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( Paths._BULK )
						.body( gson.fromJson( "{'index':{'_index': '" + writeName + "',"
								+ dialect.getTypeNameForMappingAndBulkApi().map( name -> "'_type': '" + name + "'," ).orElse( "" )
								+ "'routing': '" + routingKey + "',"
								+ "'_id': '1'}}", JsonObject.class ) )
						.body( new JsonObject() ) // We don't care about the document
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		plan.execute( OperationSubmitter.BLOCKING ).join();
		clientSpy.verifyExpectationsMet();

		plan.addOrUpdate( referenceProvider( "1", routingKey ), document -> {
			document.addValue( index.binding().string, "text2" );
		} );
		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( Paths._BULK )
						.body( gson.fromJson( "{'index':{'_index': '" + writeName + "',"
								+ dialect.getTypeNameForMappingAndBulkApi().map( name -> "'_type': '" + name + "'," ).orElse( "" )
								+ "'routing': '" + routingKey + "',"
								+ "'_id': '1'}}", JsonObject.class ) )
						.body( new JsonObject() ) // We don't care about the document
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		plan.execute( OperationSubmitter.BLOCKING ).join();
		clientSpy.verifyExpectationsMet();

		plan.delete( referenceProvider( "1", routingKey ) );
		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( Paths._BULK )
						.body( gson.fromJson( "{'delete':{'_index': '" + writeName + "',"
								+ dialect.getTypeNameForMappingAndBulkApi().map( name -> "'_type': '" + name + "'," ).orElse( "" )
								+ "'routing': '" + routingKey + "',"
								+ "'_id': '1'}}", JsonObject.class ) )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		plan.execute( OperationSubmitter.BLOCKING ).join();
		clientSpy.verifyExpectationsMet();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString() )
					.toReference();
		}
	}


}
