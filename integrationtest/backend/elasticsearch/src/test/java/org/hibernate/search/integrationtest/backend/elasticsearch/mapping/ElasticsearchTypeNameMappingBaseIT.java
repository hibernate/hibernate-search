/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.mapping;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultReadAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultWriteAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.mappingWithDiscriminatorProperty;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.mappingWithoutAnyProperty;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.JsonObject;

/**
 * Test the base functionality of type name mapping strategies.
 */
class ElasticsearchTypeNameMappingBaseIT {

	private static final String ID_1 = "id_1";
	private static final String ID_2 = "id_2";

	private enum IrregularIndexNameSupport {
		YES,
		NO
	}

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of( null, mappingWithDiscriminatorProperty( "_entity_type" ), IrregularIndexNameSupport.YES ),
				Arguments.of( "index-name", mappingWithoutAnyProperty(), IrregularIndexNameSupport.NO ),
				Arguments.of( "discriminator", mappingWithDiscriminatorProperty( "_entity_type" ),
						IrregularIndexNameSupport.YES )
		);
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticsearchClient = TestElasticsearchClient.create();

	private final StubMappedIndex index1 = StubMappedIndex.withoutFields().name( "index1" );
	private final StubMappedIndex index2 = StubMappedIndex.withoutFields().name( "index2" );

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void singleIndexScope(String strategyName, JsonObject expectedMappingContent,
			IrregularIndexNameSupport irregularIndexNameSupport) {
		setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_AND_DROP, strategyName );
		assertThatQuery(
				index1.createScope().query().where( f -> f.matchAll() ).toQuery()
		)
				.hasDocRefHitsAnyOrder( c -> c
						.doc( index1.typeName(), ID_1 )
						.doc( index1.typeName(), ID_2 )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void multiIndexScope(String strategyName, JsonObject expectedMappingContent,
			IrregularIndexNameSupport irregularIndexNameSupport) {
		setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_AND_DROP, strategyName );

		assertThatQuery(
				index1.createScope( index2 ).query().where( f -> f.matchAll() ).toQuery()
		)
				.hasDocRefHitsAnyOrder( c -> c
						.doc( index1.typeName(), ID_1 )
						.doc( index1.typeName(), ID_2 )
						.doc( index2.typeName(), ID_1 )
						.doc( index2.typeName(), ID_2 )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void irregularIndexName_correctNamingSchemeAndIncorrectUniqueKey_singleIndexScope(String strategyName,
			JsonObject expectedMappingContent,
			IrregularIndexNameSupport irregularIndexNameSupport) {
		createIndexesWithCorrectNamingSchemeIncorrectUniqueKeyAndCorrectAliases( expectedMappingContent );
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY, strategyName );

		SearchQuery<DocumentReference> query = index1.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();

		// Should work even if the index has an irregular name: the selected type-name mapping strategy is not actually used.
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( c -> c
						.doc( index1.typeName(), ID_1 )
						.doc( index1.typeName(), ID_2 )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void irregularIndexName_correctNamingSchemeAndIncorrectUniqueKey_multiIndexScope(String strategyName,
			JsonObject expectedMappingContent,
			IrregularIndexNameSupport irregularIndexNameSupport) {
		createIndexesWithCorrectNamingSchemeIncorrectUniqueKeyAndCorrectAliases( expectedMappingContent );
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY, strategyName );

		SearchQuery<DocumentReference> query = index1.createScope( index2 ).query()
				.where( f -> f.matchAll() )
				.toQuery();

		if ( IrregularIndexNameSupport.YES.equals( irregularIndexNameSupport ) ) {
			assertThatQuery( query )
					.hasDocRefHitsAnyOrder( c -> c
							.doc( index1.typeName(), ID_1 )
							.doc( index1.typeName(), ID_2 )
							.doc( index2.typeName(), ID_1 )
							.doc( index2.typeName(), ID_2 )
					);
		}
		else {
			assertThatThrownBy( () -> query.fetch( 20 ) )
					.isInstanceOf( SearchException.class );
		}
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void irregularIndexName_incorrectNamingScheme_singleIndexScope(String strategyName,
			JsonObject expectedMappingContent,
			IrregularIndexNameSupport irregularIndexNameSupport) {
		createIndexesWithIncorrectNamingSchemeAndCorrectAliases( expectedMappingContent );
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY, strategyName );

		SearchQuery<DocumentReference> query = index1.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();

		// Should work even if the index has an irregular name: the selected type-name mapping strategy is not actually used.
		assertThatQuery( query )
				.hasDocRefHitsAnyOrder( c -> c
						.doc( index1.typeName(), ID_1 )
						.doc( index1.typeName(), ID_2 )
				);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void irregularIndexName_incorrectNamingScheme_multiIndexScope(String strategyName, JsonObject expectedMappingContent,
			IrregularIndexNameSupport irregularIndexNameSupport) {
		createIndexesWithIncorrectNamingSchemeAndCorrectAliases( expectedMappingContent );
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY, strategyName );

		SearchQuery<DocumentReference> query = index1.createScope( index2 ).query()
				.where( f -> f.matchAll() )
				.toQuery();

		if ( IrregularIndexNameSupport.YES.equals( irregularIndexNameSupport ) ) {
			assertThatQuery( query )
					.hasDocRefHitsAnyOrder( c -> c
							.doc( index1.typeName(), ID_1 )
							.doc( index1.typeName(), ID_2 )
							.doc( index2.typeName(), ID_1 )
							.doc( index2.typeName(), ID_2 )
					);
		}
		else {
			assertThatThrownBy( () -> query.fetch( 20 ) )
					.isInstanceOf( SearchException.class );
		}
	}

	private void createIndexesWithCorrectNamingSchemeIncorrectUniqueKeyAndCorrectAliases(
			JsonObject expectedMappingContent) {
		URLEncodedString index1PrimaryName = IndexNames.encodeName( index1.name() + "-000001-somesuffix-000001" );
		URLEncodedString index1WriteAlias = defaultWriteAlias( index1.name() );
		URLEncodedString index1ReadAlias = defaultReadAlias( index1.name() );
		elasticsearchClient.index( index1PrimaryName, index1WriteAlias, index1ReadAlias )
				.deleteAndCreate()
				.type().putMapping( expectedMappingContent );
		URLEncodedString index2PrimaryName = IndexNames.encodeName( index2.name() + "-000001-somesuffix-000001" );
		URLEncodedString index2WriteAlias = defaultWriteAlias( index2.name() );
		URLEncodedString index2ReadAlias = defaultReadAlias( index2.name() );
		elasticsearchClient.index( index2PrimaryName, index2WriteAlias, index2ReadAlias )
				.deleteAndCreate()
				.type().putMapping( expectedMappingContent );
	}

	private void createIndexesWithIncorrectNamingSchemeAndCorrectAliases(JsonObject expectedMappingContent) {
		URLEncodedString index1PrimaryName = IndexNames.encodeName( index1.name() + "-somesuffix" );
		URLEncodedString index1WriteAlias = defaultWriteAlias( index1.name() );
		URLEncodedString index1ReadAlias = defaultReadAlias( index1.name() );
		elasticsearchClient.index( index1PrimaryName, index1WriteAlias, index1ReadAlias )
				.deleteAndCreate()
				.type().putMapping( expectedMappingContent );
		URLEncodedString index2PrimaryName = IndexNames.encodeName( index2.name() + "-somesuffix" );
		URLEncodedString index2WriteAlias = defaultWriteAlias( index2.name() );
		URLEncodedString index2ReadAlias = defaultReadAlias( index2.name() );
		elasticsearchClient.index( index2PrimaryName, index2WriteAlias, index2ReadAlias )
				.deleteAndCreate()
				.type().putMapping( expectedMappingContent );
	}

	private void setup(StubMappingSchemaManagementStrategy schemaManagementStrategy, String strategyName) {
		setupHelper.start()
				.withSchemaManagement( schemaManagementStrategy )
				.withBackendProperty(
						ElasticsearchBackendSettings.MAPPING_TYPE_NAME_STRATEGY, strategyName
				)
				.withIndexes( index1, index2 )
				.setup();

		initData();
	}

	private void initData() {
		BulkIndexer indexer1 = index1.bulkIndexer()
				.add( ID_1, document -> {} )
				.add( ID_2, document -> {} );
		BulkIndexer indexer2 = index2.bulkIndexer()
				.add( ID_1, document -> {} )
				.add( ID_2, document -> {} );
		indexer1.join( indexer2 );
	}

}
