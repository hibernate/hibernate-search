/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.search.query;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultReadAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.encodeName;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.impl.ElasticsearchBackendImplSettings;
import org.hibernate.search.backend.elasticsearch.client.impl.Paths;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.StubSingleIndexLayoutStrategy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Test the content of generated Elasticsearch search queries.
 */
class ElasticsearchSearchQueryIT {

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of( null, defaultReadAlias( index.name() ) ),
				Arguments.of( "no-alias", encodeName( index.name() ) ),
				Arguments.of( new StubSingleIndexLayoutStrategy( "custom-write", "custom-read" ), encodeName( "custom-read" ) )
		);
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public ElasticsearchClientSpy clientSpy = ElasticsearchClientSpy.create();

	public void init(Object layoutStrategy, URLEncodedString readName) {
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
	void simple(Object layoutStrategy, URLEncodedString readName) {
		init( layoutStrategy, readName );
		StubMappingScope scope = index.createScope();

		SearchQuery<?> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();

		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( readName )
						.pathComponent( Paths._SEARCH )
						.body( new JsonObject() ) // We don't care about the payload
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		query.fetchAll();
	}

	@ParameterizedTest(name = "IndexLayoutStrategy = {0}")
	@MethodSource("params")
	void defaultSourceFiltering(Object layoutStrategy, URLEncodedString readName) {
		init( layoutStrategy, readName );
		StubMappingScope scope = index.createScope();

		SearchQuery<?> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();

		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( readName )
						.pathComponent( Paths._SEARCH )
						.body( new Gson().fromJson( "{'_source':false}", JsonObject.class ) )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		query.fetchAll();
	}

	@ParameterizedTest(name = "IndexLayoutStrategy = {0}")
	@MethodSource("params")
	void projection_sourceFiltering(Object layoutStrategy, URLEncodedString readName) {
		init( layoutStrategy, readName );
		StubMappingScope scope = index.createScope();

		SearchQuery<Object> query = scope.query()
				.select( f -> f.field( "string" ) )
				.where( f -> f.matchAll() )
				.toQuery();

		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( readName )
						.pathComponent( Paths._SEARCH )
						.body( new Gson().fromJson( "{'_source':['string']}", JsonObject.class ) )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		query.fetchAll();
	}

	@ParameterizedTest(name = "IndexLayoutStrategy = {0}")
	@MethodSource("params")
	void routing(Object layoutStrategy, URLEncodedString readName) {
		init( layoutStrategy, readName );
		StubMappingScope scope = index.createScope();

		String routingKey = "someRoutingKey";

		SearchQuery<?> query = scope.query()
				.where( f -> f.matchAll() )
				.routing( routingKey )
				.toQuery();

		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.pathComponent( readName )
						.pathComponent( Paths._SEARCH )
						.body( new JsonObject() ) // We don't care about the payload
						.param( "routing", routingKey )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		query.fetchAll();
	}

	@ParameterizedTest(name = "IndexLayoutStrategy = {0}")
	@MethodSource("params")
	void trackTotalHits_fetch(Object layoutStrategy, URLEncodedString readName) {
		init( layoutStrategy, readName );

		StubMappingScope scope = index.createScope();

		SearchQuery<?> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();

		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.param( "track_total_hits", true )
						.pathComponent( readName )
						.pathComponent( Paths._SEARCH )
						.body( new JsonObject() ) // We don't care about the payload
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		query.fetch( 30 );
	}

	@ParameterizedTest(name = "IndexLayoutStrategy = {0}")
	@MethodSource("params")
	void trackTotalHits_fetchHits(Object layoutStrategy, URLEncodedString readName) {
		init( layoutStrategy, readName );

		StubMappingScope scope = index.createScope();

		SearchQuery<?> query = scope.query()
				.where( f -> f.matchAll() )
				.toQuery();

		clientSpy.expectNext(
				ElasticsearchRequest.post()
						.param( "track_total_hits", false )
						.pathComponent( readName )
						.pathComponent( Paths._SEARCH )
						.body( new JsonObject() ) // We don't care about the payload
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		query.fetchHits( 30 );
	}

	@SuppressWarnings("unused")
	private static class IndexBinding {
		final IndexFieldReference<Integer> integer;
		final IndexFieldReference<String> string;

		IndexBinding(IndexSchemaElement root) {
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
