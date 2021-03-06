/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.AnalysisBuiltinOverrideITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.AnalysisCustomITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;

public class ElasticsearchTckBackendHelper implements TckBackendHelper {

	private final ElasticsearchTckBackendFeatures features = new ElasticsearchTckBackendFeatures( ElasticsearchTestDialect.get() );

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
			{
				useConfigurationTestRule();
			}

			@Override
			public SearchSetupHelper.SetupContext startSetup(SearchSetupHelper.SetupContext setupHelper) {
				// Make sure automatically created indexes will have an appropriate number of shards
				backendConfiguration.testElasticsearchClient().template( "sharded_index" )
						.create(
								"*",
								"{'number_of_shards': " + shardCount + "}"
						);

				// Nothing to change in the Hibernate Search configuration
				return setupHelper;
			}
		};
	}

	@Override
	public TckBackendSetupStrategy<?> createPeriodicRefreshBackendSetupStrategy(int refreshIntervalMs) {
		return new ElasticsearchTckBackendSetupStrategy() {
			{
				useConfigurationTestRule();
			}

			@Override
			public SearchSetupHelper.SetupContext startSetup(SearchSetupHelper.SetupContext setupHelper) {
				// Make sure automatically created indexes will have an appropriate number of shards
				backendConfiguration.testElasticsearchClient().template( "explicit_refresh_interval" )
						.create(
								"*",
								"{'refresh_interval': '" + refreshIntervalMs + "ms' }"
						);

				// Nothing to change in the Hibernate Search configuration
				return setupHelper;
			}
		};
	}
}
