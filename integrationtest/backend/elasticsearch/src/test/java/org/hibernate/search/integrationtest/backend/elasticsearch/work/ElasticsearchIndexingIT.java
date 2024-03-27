/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.work;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultWriteAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.encodeName;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.impl.ElasticsearchBackendImplSettings;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.StubSingleIndexLayoutStrategy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Test the content of generated Elasticsearch indexing requests.
 */
class ElasticsearchIndexingIT {

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of( null, defaultWriteAlias( index.name() ) ),
				Arguments.of( "no-alias", encodeName( index.name() ) ),
				Arguments.of( new StubSingleIndexLayoutStrategy( "custom-write", "custom-read" ), encodeName( "custom-write" ) )
		);
	}

	private final ElasticsearchTestDialect dialect = ElasticsearchTestDialect.get();

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public ElasticsearchClientSpy clientSpy = ElasticsearchClientSpy.create();

	public void init(Object layoutStrategy, URLEncodedString writeName) {
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

	@ParameterizedTest(name = "IndexLayoutStrategy = {0}")
	@MethodSource("params")
	void addUpdateDelete_noRouting(Object layoutStrategy, URLEncodedString writeName) {
		init( layoutStrategy, writeName );
		Gson gson = new Gson();

		IndexIndexingPlan plan = index.createIndexingPlan();

		plan.add( referenceProvider( "1" ), document -> {
			document.addValue( index.binding().string, "text1" );
		} );
		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( Paths._BULK )
						.body( gson.fromJson( "{'index':{'_index': '" + writeName + "','_id': '1'}}", JsonObject.class ) )
						.body( new JsonObject() ) // We don't care about the document
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		plan.execute( OperationSubmitter.blocking() ).join();
		clientSpy.verifyExpectationsMet();

		plan.addOrUpdate( referenceProvider( "1" ), document -> {
			document.addValue( index.binding().string, "text2" );
		} );
		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( Paths._BULK )
						.body( gson.fromJson( "{'index':{'_index': '" + writeName + "','_id': '1'}}", JsonObject.class ) )
						.body( new JsonObject() ) // We don't care about the document
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		plan.execute( OperationSubmitter.blocking() ).join();
		clientSpy.verifyExpectationsMet();

		plan.delete( referenceProvider( "1" ) );
		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( Paths._BULK )
						.body( gson.fromJson( "{'delete':{'_index': '" + writeName + "','_id': '1'}}", JsonObject.class ) )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		plan.execute( OperationSubmitter.blocking() ).join();
		clientSpy.verifyExpectationsMet();
	}

	@ParameterizedTest(name = "IndexLayoutStrategy = {0}")
	@MethodSource("params")
	void addUpdateDelete_routing(Object layoutStrategy, URLEncodedString writeName) {
		init( layoutStrategy, writeName );
		Gson gson = new Gson();

		String routingKey = "someRoutingKey";
		IndexIndexingPlan plan = index.createIndexingPlan();

		plan.add( referenceProvider( "1", routingKey ), document -> {
			document.addValue( index.binding().string, "text1" );
		} );
		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( Paths._BULK )
						.body( gson.fromJson( "{'index':{'_index': '" + writeName + "','routing': '" + routingKey + "',"
								+ "'_id': '1'}}", JsonObject.class ) )
						.body( new JsonObject() ) // We don't care about the document
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		plan.execute( OperationSubmitter.blocking() ).join();
		clientSpy.verifyExpectationsMet();

		plan.addOrUpdate( referenceProvider( "1", routingKey ), document -> {
			document.addValue( index.binding().string, "text2" );
		} );
		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( Paths._BULK )
						.body( gson.fromJson( "{'index':{'_index': '" + writeName + "','routing': '" + routingKey + "',"
								+ "'_id': '1'}}", JsonObject.class ) )
						.body( new JsonObject() ) // We don't care about the document
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		plan.execute( OperationSubmitter.blocking() ).join();
		clientSpy.verifyExpectationsMet();

		plan.delete( referenceProvider( "1", routingKey ) );
		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( Paths._BULK )
						.body( gson.fromJson( "{'delete':{'_index': '" + writeName + "','routing': '" + routingKey + "',"
								+ "'_id': '1'}}", JsonObject.class ) )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		plan.execute( OperationSubmitter.blocking() ).join();
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
