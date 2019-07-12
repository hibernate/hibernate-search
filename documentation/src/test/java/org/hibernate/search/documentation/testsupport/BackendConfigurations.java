/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.rule.MappingSetupHelper;

public final class BackendConfigurations {

	private BackendConfigurations() {
	}

	public static List<BackendConfiguration> simple() {
		return Arrays.asList(
				new LuceneBackendConfiguration(),
				new ElasticsearchBackendConfiguration()
		);
	}

	public static List<BackendConfiguration> hashBasedSharding(int shardCount) {
		return Arrays.asList(
				new LuceneBackendConfiguration() {
					@Override
					public <C extends MappingSetupHelper<C, ?, ?>.AbstractSetupContext> C setupWithName(C setupContext,
							String backendName, TestConfigurationProvider configurationProvider) {
						return super.setupWithName( setupContext, backendName, configurationProvider )
								.withIndexDefaultsProperty(
										backendName, LuceneIndexSettings.SHARDING_STRATEGY, "hash"
								)
								.withIndexDefaultsProperty(
										backendName, LuceneIndexSettings.SHARDING_NUMBER_OF_SHARDS, shardCount
								);
					}
				},
				new ElasticsearchBackendConfiguration() {
					@Override
					public <C extends MappingSetupHelper<C, ?, ?>.AbstractSetupContext> C setupWithName(C setupContext,
							String backendName, TestConfigurationProvider configurationProvider) {
						// Make sure automatically created indexes will have an appropriate number of shards
						testElasticsearchClient.template( "sharded_index" )
								.create(
										"*",
										99999, // Override other templates, if any
										"{'number_of_shards': " + shardCount + "}"
								);
						return super.setupWithName( setupContext, backendName, configurationProvider );
					}
				}
		);
	}

}
