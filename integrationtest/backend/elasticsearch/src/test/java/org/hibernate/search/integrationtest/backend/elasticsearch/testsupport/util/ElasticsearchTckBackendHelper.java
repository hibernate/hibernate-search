/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import java.util.Optional;

import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.AnalysisCustomITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.AnalysisOverrideITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;

import org.junit.rules.TestRule;

public class ElasticsearchTckBackendHelper implements TckBackendHelper {

	private final ElasticsearchTckBackendFeatures features = new ElasticsearchTckBackendFeatures( ElasticsearchTestDialect.get() );

	@Override
	public TckBackendFeatures getBackendFeatures() {
		return features;
	}

	@Override
	public TckBackendSetupStrategy createDefaultBackendSetupStrategy() {
		return new ElasticsearchTckBackendSetupStrategy();
	}

	@Override
	public TckBackendSetupStrategy createMultiTenancyBackendSetupStrategy() {
		return new ElasticsearchTckBackendSetupStrategy()
				.setProperty( "multi_tenancy.strategy", "discriminator" );
	}

	@Override
	public TckBackendSetupStrategy createAnalysisCustomBackendSetupStrategy() {
		return new ElasticsearchTckBackendSetupStrategy()
				.setProperty( "analysis.configurer", AnalysisCustomITAnalysisConfigurer.class.getName() );
	}

	@Override
	public TckBackendSetupStrategy createAnalysisOverrideBackendSetupStrategy() {
		return new ElasticsearchTckBackendSetupStrategy()
				.setProperty( "analysis.configurer", AnalysisOverrideITAnalysisConfigurer.class.getName() );
	}

	@Override
	public TckBackendSetupStrategy createNoShardingBackendSetupStrategy() {
		/*
		 * Just configure Elasticsearch to only have one shard.
		 * This is the default when we launch ES as part of the Maven build,
		 * but it may not be the case when testing with a provided ES cluster.
		 */
		return createHashBasedShardingBackendSetupStrategy( 1 );
	}

	@Override
	public TckBackendSetupStrategy createHashBasedShardingBackendSetupStrategy(int shardCount) {
		return new ElasticsearchTckBackendSetupStrategy() {
			private final TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

			@Override
			public Optional<TestRule> getTestRule() {
				return Optional.of( elasticsearchClient );
			}

			@Override
			public SearchSetupHelper.SetupContext startSetup(SearchSetupHelper.SetupContext setupHelper) {
				// Make sure automatically created indexes will have an appropriate number of shards
				elasticsearchClient.template( "sharded_index" )
						.create(
								"*",
								99999, // Override other templates, if any
								"{'number_of_shards': " + shardCount + "}"
						);

				// Nothing to change in the Hibernate Search configuration
				return setupHelper;
			}
		};
	}

	@Override
	public TckBackendSetupStrategy createPeriodicRefreshBackendSetupStrategy(int refreshIntervalMs) {
		return new ElasticsearchTckBackendSetupStrategy() {
			private final TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

			@Override
			public Optional<TestRule> getTestRule() {
				return Optional.of( elasticsearchClient );
			}

			@Override
			public SearchSetupHelper.SetupContext startSetup(SearchSetupHelper.SetupContext setupHelper) {
				// Make sure automatically created indexes will have an appropriate number of shards
				elasticsearchClient.template( "explicit_refresh_interval" )
						.create(
								"*",
								99999, // Override other templates, if any
								"{'refresh_interval': '" + refreshIntervalMs + "ms' }"
						);

				// Nothing to change in the Hibernate Search configuration
				return setupHelper;
			}
		};
	}
}
