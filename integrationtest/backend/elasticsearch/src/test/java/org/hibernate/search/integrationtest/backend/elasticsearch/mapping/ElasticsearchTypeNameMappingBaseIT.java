/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.mapping;

import static org.hibernate.search.integrationtest.backend.elasticsearch.mapping.ElasticsearchTypeNameMappingTestUtils.mappingWithDiscriminatorProperty;
import static org.hibernate.search.integrationtest.backend.elasticsearch.mapping.ElasticsearchTypeNameMappingTestUtils.mappingWithoutAnyProperty;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.SubTest;

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

	private static final String BACKEND_NAME = "backendname";
	private static final String TYPE1_NAME = "type1_name";
	private static final String INDEX1_NAME = "index1_name";
	private static final String TYPE2_NAME = "type2_name";
	private static final String INDEX2_NAME = "index2_name";

	private static final String INDEX_NAME_SUFFIX_WHEN_ALIASES = "_actual";

	private static final String ID_1 = "id_1";
	private static final String ID_2 = "id_2";

	private enum AliasSupport {
		YES,
		NO
	}

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] configurations() {
		return new Object[][] {
				{ null, mappingWithDiscriminatorProperty( "_entity_type" ), AliasSupport.YES },
				{ "index-name", mappingWithoutAnyProperty(), AliasSupport.NO },
				{ "discriminator", mappingWithDiscriminatorProperty( "_entity_type" ), AliasSupport.YES }
		};
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private final String strategyName;
	private final JsonObject expectedMappingContent;
	private final AliasSupport aliasSupport;

	private StubMappingIndexManager index1Manager;
	private StubMappingIndexManager index2Manager;

	public ElasticsearchTypeNameMappingBaseIT(String strategyName, JsonObject expectedMappingContent,
			AliasSupport aliasSupport) {
		this.strategyName = strategyName;
		this.expectedMappingContent = expectedMappingContent;
		this.aliasSupport = aliasSupport;
	}

	@Test
	public void singleIndexScope() {
		setup( IndexLifecycleStrategyName.DROP_AND_CREATE_AND_DROP );
		SearchResultAssert.assertThat(
				index1Manager.createScope().query().where( f -> f.matchAll() ).toQuery()
		)
				.hasDocRefHitsAnyOrder( c -> c
						.doc( TYPE1_NAME, ID_1 )
						.doc( TYPE1_NAME, ID_2 )
				);
	}

	@Test
	public void multiIndexScope() {
		setup( IndexLifecycleStrategyName.DROP_AND_CREATE_AND_DROP );

		SearchResultAssert.assertThat(
				index1Manager.createScope( index2Manager ).query().where( f -> f.matchAll() ).toQuery()
		)
				.hasDocRefHitsAnyOrder( c -> c
						.doc( TYPE1_NAME, ID_1 )
						.doc( TYPE1_NAME, ID_2 )
						.doc( TYPE2_NAME, ID_1 )
						.doc( TYPE2_NAME, ID_2 )
				);
	}

	@Test
	public void alias_singleIndexScope() {
		createIndexAndAliases();
		setup( IndexLifecycleStrategyName.NONE );

		SearchQuery<DocumentReference> query = index1Manager.createScope().query()
				.where( f -> f.matchAll() )
				.toQuery();

		// Should work even if aliases are not supported: the selected type-name mapping strategy is not actually used.
		SearchResultAssert.assertThat( query )
				.hasDocRefHitsAnyOrder( c -> c
						.doc( TYPE1_NAME, ID_1 )
						.doc( TYPE1_NAME, ID_2 )
				);
	}

	@Test
	public void alias_multiIndexScope() {
		createIndexAndAliases();
		setup( IndexLifecycleStrategyName.NONE );

		SearchQuery<DocumentReference> query = index1Manager.createScope( index2Manager ).query()
				.where( f -> f.matchAll() )
				.toQuery();

		if ( AliasSupport.YES.equals( aliasSupport ) ) {
			SearchResultAssert.assertThat( query )
					.hasDocRefHitsAnyOrder( c -> c
							.doc( TYPE1_NAME, ID_1 )
							.doc( TYPE1_NAME, ID_2 )
							.doc( TYPE2_NAME, ID_1 )
							.doc( TYPE2_NAME, ID_2 )
					);
		}
		else {
			SubTest.expectException( () -> query.fetch( 20 ) )
					.assertThrown()
					.isInstanceOf( SearchException.class );
		}
	}

	private void createIndexAndAliases() {
		String index1ActualName = INDEX1_NAME + INDEX_NAME_SUFFIX_WHEN_ALIASES;
		elasticsearchClient.index( index1ActualName ).deleteAndCreate();
		elasticsearchClient.index( index1ActualName ).type().putMapping( expectedMappingContent );
		elasticsearchClient.index( index1ActualName ).addAlias( INDEX1_NAME );
		String index2ActualName = INDEX2_NAME + INDEX_NAME_SUFFIX_WHEN_ALIASES;
		elasticsearchClient.index( index2ActualName ).deleteAndCreate();
		elasticsearchClient.index( index2ActualName ).type().putMapping( expectedMappingContent );
		elasticsearchClient.index( index2ActualName ).addAlias( INDEX2_NAME );
	}

	private void setup(IndexLifecycleStrategyName lifecycleStrategy) {
		setupHelper.start( BACKEND_NAME )
				.withIndexDefaultsProperty(
						BACKEND_NAME, ElasticsearchIndexSettings.LIFECYCLE_STRATEGY, lifecycleStrategy
				)
				.withBackendProperty(
						BACKEND_NAME, ElasticsearchBackendSettings.MAPPING_TYPE_NAME_STRATEGY, strategyName
				)
				.withIndex(
						INDEX1_NAME,
						options -> options.mappedType( TYPE1_NAME ),
						ignored -> { },
						indexManager -> this.index1Manager = indexManager
				)
				.withIndex(
						INDEX2_NAME,
						options -> options.mappedType( TYPE2_NAME ),
						ignored -> { },
						indexManager -> this.index2Manager = indexManager
				)
				.setup();

		initData();
	}

	private void initData() {
		IndexIndexingPlan<? extends DocumentElement> plan = index1Manager.createIndexingPlan();
		plan.add( referenceProvider( ID_1 ), document -> { } );
		plan.add( referenceProvider( ID_2 ), document -> { } );
		plan.execute().join();

		plan = index2Manager.createIndexingPlan();
		plan.add( referenceProvider( ID_1 ), document -> { } );
		plan.add( referenceProvider( ID_2 ), document -> { } );
		plan.execute().join();
	}

}
