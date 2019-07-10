/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.Optional;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.AnalysisCustomITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.AnalysisOverrideITAnalysisConfigurer;
import org.hibernate.search.util.impl.integrationtest.elasticsearch.ElasticsearchTestHostConnectionConfiguration;
import org.hibernate.search.util.impl.integrationtest.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.elasticsearch.rule.TestElasticsearchClient;

import org.junit.rules.TestRule;

public class ElasticsearchTckBackendHelper implements TckBackendHelper {

	public static final String DEFAULT_BACKEND_PROPERTIES_PATH = "/backend-defaults.properties";

	private final ElasticsearchTckBackendFeatures features = new ElasticsearchTckBackendFeatures( ElasticsearchTestDialect.get() );

	@Override
	public TckBackendFeatures getBackendFeatures() {
		return features;
	}

	@Override
	public TckBackendSetupStrategy createDefaultBackendSetupStrategy() {
		return TckBackendSetupStrategy.of( DEFAULT_BACKEND_PROPERTIES_PATH, createProperties() );
	}

	@Override
	public TckBackendSetupStrategy createMultiTenancyBackendSetupStrategy() {
		return TckBackendSetupStrategy.of(
				DEFAULT_BACKEND_PROPERTIES_PATH,
				createProperties( c -> c.accept( "multi_tenancy_strategy", "discriminator" ) )
		);
	}

	@Override
	public TckBackendSetupStrategy createAnalysisCustomBackendSetupStrategy() {
		return TckBackendSetupStrategy.of(
				DEFAULT_BACKEND_PROPERTIES_PATH,
				createProperties( c ->
						c.accept( "analysis_configurer", AnalysisCustomITAnalysisConfigurer.class.getName() )
				)
		);
	}

	@Override
	public TckBackendSetupStrategy createAnalysisOverrideBackendSetupStrategy() {
		return TckBackendSetupStrategy.of(
				DEFAULT_BACKEND_PROPERTIES_PATH,
				createProperties( c ->
						c.accept( "analysis_configurer", AnalysisOverrideITAnalysisConfigurer.class.getName() )
				)
		);
	}

	private Map<String, Object> createProperties() {
		Map<String, Object> map = new LinkedHashMap<>();
		// Always add configuration options that allow to connect to Elasticsearch
		ElasticsearchTestHostConnectionConfiguration.get().addToBackendProperties( map );
		return map;
	}

	private Map<String, Object> createProperties(Consumer<BiConsumer<String, Object>> overrides) {
		Map<String, Object> map = createProperties();
		overrides.accept( map::put );
		return map;
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
		return new TckBackendSetupStrategy() {
			private final TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

			@Override
			public Optional<TestRule> getTestRule() {
				return Optional.of( elasticsearchClient );
			}

			@Override
			public ConfigurationPropertySource createBackendConfigurationPropertySource(
					TestConfigurationProvider configurationProvider) {
				Map<String, Object> properties = configurationProvider.getPropertiesFromFile(
						DEFAULT_BACKEND_PROPERTIES_PATH
				);
				ElasticsearchTestHostConnectionConfiguration.get().addToBackendProperties( properties );
				return ConfigurationPropertySource.fromMap( properties );
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
}
