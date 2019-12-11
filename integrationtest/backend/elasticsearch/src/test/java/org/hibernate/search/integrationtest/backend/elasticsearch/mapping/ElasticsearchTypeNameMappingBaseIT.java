/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.mapping;

import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

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

	private static final String ID_1 = "id_1";
	private static final String ID_2 = "id_2";

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] configurations() {
		return new Object[][] {
				{ null },
				{ "index-name" },
				{ "discriminator"  }
		};
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final String strategyName;

	private StubMappingIndexManager index1Manager;
	private StubMappingIndexManager index2Manager;

	public ElasticsearchTypeNameMappingBaseIT(String strategyName) {
		this.strategyName = strategyName;
	}

	@Test
	public void singleIndexScope() {
		setup();
		SearchResultAssert.assertThat(
				index1Manager.createScope().query().predicate( f -> f.matchAll() ).toQuery()
		)
				.hasDocRefHitsAnyOrder( c -> c
						.doc( TYPE1_NAME, ID_1 )
						.doc( TYPE1_NAME, ID_2 )
				);
	}

	@Test
	public void multiIndexScope() {
		setup();
		SearchResultAssert.assertThat(
				index1Manager.createScope( index2Manager ).query().predicate( f -> f.matchAll() ).toQuery()
		)
				.hasDocRefHitsAnyOrder( c -> c
						.doc( TYPE1_NAME, ID_1 )
						.doc( TYPE1_NAME, ID_2 )
						.doc( TYPE2_NAME, ID_1 )
						.doc( TYPE2_NAME, ID_2 )
				);
	}

	private void setup() {
		setupHelper.start( BACKEND_NAME )
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
