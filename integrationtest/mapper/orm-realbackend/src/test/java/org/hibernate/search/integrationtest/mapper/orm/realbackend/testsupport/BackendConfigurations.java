/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport;

import static org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration.BACKEND_TYPE;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.rule.MappingSetupHelper;

public class BackendConfigurations {

	private BackendConfigurations() {
	}

	public static BackendConfiguration simple() {
		switch ( BACKEND_TYPE ) {
			case "lucene":
				return new LuceneBackendConfiguration();
			case "elasticsearch":
				return new ElasticsearchBackendConfiguration();
			default:
				throw new IllegalStateException( "Unknown backend type:" + BACKEND_TYPE );
		}
	}

	public static BackendConfiguration hashBasedSharding(int shardCount) {
		switch ( BACKEND_TYPE ) {
			case "lucene":
				return new LuceneBackendConfiguration() {
					@Override
					public <C extends MappingSetupHelper<C, ?, ?>.AbstractSetupContext> C setup(C setupContext,
							String backendNameOrNull, TestConfigurationProvider configurationProvider) {
						return super.setup( setupContext, backendNameOrNull, configurationProvider )
								.withBackendProperty(
										backendNameOrNull, LuceneIndexSettings.SHARDING_STRATEGY, "hash"
								)
								.withBackendProperty(
										backendNameOrNull, LuceneIndexSettings.SHARDING_NUMBER_OF_SHARDS, shardCount
								);
					}
				};
			case "elasticsearch":
				return new ElasticsearchBackendConfiguration() {
					@Override
					public <C extends MappingSetupHelper<C, ?, ?>.AbstractSetupContext> C setup(C setupContext,
							String backendNameOrNull, TestConfigurationProvider configurationProvider) {
						// Make sure automatically created indexes will have an appropriate number of shards
						testElasticsearchClient.template( "sharded_index" )
								.create(
										"*",
										99999, // Override other templates, if any
										"{'number_of_shards': " + shardCount + "}"
								);
						return super.setup( setupContext, backendNameOrNull, configurationProvider );
					}
				};
			default:
				throw new IllegalStateException( "Unknown backend type: " + BACKEND_TYPE );
		}
	}

}
