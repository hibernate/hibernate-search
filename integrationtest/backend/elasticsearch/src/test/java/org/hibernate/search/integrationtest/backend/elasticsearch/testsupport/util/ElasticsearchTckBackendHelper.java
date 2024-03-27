/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryDslExtension;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.AnalysisBuiltinOverrideITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.AnalysisCustomITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;

public class ElasticsearchTckBackendHelper implements TckBackendHelper {

	private final ElasticsearchTckBackendFeatures features = new ElasticsearchTckBackendFeatures();

	@Override
	public TckBackendFeatures getBackendFeatures() {
		return features;
	}

	@Override
	public TckBackendSetupStrategy<?> createDefaultBackendSetupStrategy() {
		return new ElasticsearchTckBackendSetupStrategy();
	}

	@Override
	public TckBackendSetupStrategy<?> createMultiTenancyBackendSetupStrategy() {
		return new ElasticsearchTckBackendSetupStrategy()
				.setProperty( "multi_tenancy.strategy", "discriminator" );
	}

	@Override
	public TckBackendSetupStrategy<?> createNoShardingMultiTenancyBackendSetupStrategy() {
		return createNoShardingBackendSetupStrategy()
				.setProperty( "multi_tenancy.strategy", "discriminator" );
	}

	@Override
	public TckBackendSetupStrategy<?> createAnalysisNotConfiguredBackendSetupStrategy() {
		return new ElasticsearchTckBackendSetupStrategy()
				.setProperty( "analysis.configurer", null );
	}

	@Override
	public TckBackendSetupStrategy<?> createAnalysisBuiltinOverridesBackendSetupStrategy() {
		return new ElasticsearchTckBackendSetupStrategy()
				.expectCustomBeans()
				.setProperty( "analysis.configurer", AnalysisBuiltinOverrideITAnalysisConfigurer.class.getName() );
	}

	@Override
	public TckBackendSetupStrategy<?> createAnalysisCustomBackendSetupStrategy() {
		return new ElasticsearchTckBackendSetupStrategy()
				.expectCustomBeans()
				.setProperty( "analysis.configurer", AnalysisCustomITAnalysisConfigurer.class.getName() );
	}

	@Override
	public TckBackendSetupStrategy<?> createNoShardingBackendSetupStrategy() {
		/*
		 * Just configure Elasticsearch to only have one shard.
		 * This is the default when we launch ES as part of the Maven build,
		 * but it may not be the case when testing with a provided ES cluster.
		 */
		return createHashBasedShardingBackendSetupStrategy( 1 );
	}

	@Override
	public TckBackendSetupStrategy<?> createHashBasedShardingBackendSetupStrategy(int shardCount) {
		return new ElasticsearchTckBackendSetupStrategy() {
			@Override
			public SearchSetupHelper.SetupContext startSetup(SearchSetupHelper.SetupContext setupHelper) {
				// Make sure automatically created indexes will have an appropriate number of shards
				return setupHelper.withBackendProperty( ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE,
						"index-settings-for-tests/" + shardCount + "-shards.json" );
			}
		};
	}

	@Override
	public TckBackendSetupStrategy<?> createRarePeriodicRefreshBackendSetupStrategy() {
		return new ElasticsearchTckBackendSetupStrategy() {

			@Override
			public SearchSetupHelper.SetupContext startSetup(SearchSetupHelper.SetupContext setupHelper) {
				// Make sure automatically created indexes will perform auto refresh very rarely
				return setupHelper.withBackendProperty( ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE,
						"index-settings-for-tests/rare-periodic-refresh.json" );
			}
		};
	}

	@Override
	public PredicateFinalStep createSlowPredicate(SearchPredicateFactory f) {
		return f.extension( ElasticsearchExtension.get() )
				.fromJson( "{\"script\": {"
						+ "  \"script\": \""
						+ "  long end = System.nanoTime() + 10000000L;"
						+ "  while ( System.nanoTime() < end ) {"
						// We can't use Thread.sleep, so let's do something slow.
						// Note that (0L, 100000000L) takes forever to execute on AWS ES Service,
						// so we'll stick to a shorter range.
						+ "LongStream.range( 0L, 1000000L ).sum();"
						+ "}"
						+ "\""
						+ "} }" );
	}

	@Override
	public <
			R,
			E,
			LOS> SearchQueryDslExtension<? extends SearchQuerySelectStep<?, R, E, LOS, ?, ?>, R, E, LOS> queryDslExtension() {
		return ElasticsearchExtension.get();
	}
}
