/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.mapping;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultReadAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultWriteAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.mappingWithDiscriminatorProperty;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.mappingWithoutAnyProperty;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.index.layout.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.JsonObject;

/**
 * Test the base functionality of type name mapping strategies.
 */
@RunWith(Parameterized.class)
public class ElasticsearchTypeNameMappingBaseIT {

	private static final String ID_1 = "id_1";
	private static final String ID_2 = "id_2";

	private enum IrregularIndexNameSupport {
		YES,
		NO
	}

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] configurations() {
		return new Object[][] {
				{ null, mappingWithDiscriminatorProperty( "_entity_type" ), IrregularIndexNameSupport.YES },
				{ "index-name", mappingWithoutAnyProperty(), IrregularIndexNameSupport.NO },
				{ "discriminator", mappingWithDiscriminatorProperty( "_entity_type" ), IrregularIndexNameSupport.YES }
		};
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private final StubMappedIndex index1 = StubMappedIndex.withoutFields().name( "index1" );
	private final StubMappedIndex index2 = StubMappedIndex.withoutFields().name( "index2" );

	private final String strategyName;
	private final JsonObject expectedMappingContent;
	private final IrregularIndexNameSupport irregularIndexNameSupport;

	public ElasticsearchTypeNameMappingBaseIT(String strategyName, JsonObject expectedMappingContent,
			IrregularIndexNameSupport irregularIndexNameSupport) {
		this.strategyName = strategyName;
		this.expectedMappingContent = expectedMappingContent;
		this.irregularIndexNameSupport = irregularIndexNameSupport;
	}

	@Test
	public void singleIndexScope() {
		setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_AND_DROP );
		assertThatQuery(
				index1.createScope().query().where( f -> f.matchAll() ).toQuery()
		)
				.hasDocRefHitsAnyOrder( c -> c
						.doc( index1.typeName(), ID_1 )
						.doc( index1.typeName(), ID_2 )
				);
	}

	@Test
	public void multiIndexScope() {
		setup( StubMappingSchemaManagementStrategy.DROP_AND_CREATE_AND_DROP );

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

	@Test
	public void irregularIndexName_correctNamingSchemeAndIncorrectUniqueKey_singleIndexScope() {
		createIndexesWithCorrectNamingSchemeIncorrectUniqueKeyAndCorrectAliases();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY );

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

	@Test
	public void irregularIndexName_correctNamingSchemeAndIncorrectUniqueKey_multiIndexScope() {
		createIndexesWithCorrectNamingSchemeIncorrectUniqueKeyAndCorrectAliases();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY );

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

	@Test
	public void irregularIndexName_incorrectNamingScheme_singleIndexScope() {
		createIndexesWithIncorrectNamingSchemeAndCorrectAliases();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY );

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

	@Test
	public void irregularIndexName_incorrectNamingScheme_multiIndexScope() {
		createIndexesWithIncorrectNamingSchemeAndCorrectAliases();
		setup( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY );

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

	private void createIndexesWithCorrectNamingSchemeIncorrectUniqueKeyAndCorrectAliases() {
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

	private void createIndexesWithIncorrectNamingSchemeAndCorrectAliases() {
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

	private void setup(StubMappingSchemaManagementStrategy schemaManagementStrategy) {
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
