/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.testsupport;

import static org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration.BACKEND_TYPE;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.extension.MappingSetupHelper;

public final class BackendConfigurations {

	private BackendConfigurations() {
	}

	// Plain configuration, without analysis configurers
	public static BackendConfiguration plain() {
		switch ( BACKEND_TYPE ) {
			case "lucene":
				return new LuceneBackendConfiguration();
			case "elasticsearch":
				return new ElasticsearchBackendConfiguration();
			default:
				throw new IllegalStateException( "Unknown backend type: " + BACKEND_TYPE );
		}
	}

	public static BackendConfiguration simple() {
		switch ( BACKEND_TYPE ) {
			case "lucene":
				return new DocumentationLuceneBackendConfiguration();
			case "elasticsearch":
				return new DocumentationElasticsearchBackendConfiguration();
			default:
				throw new IllegalStateException( "Unknown backend type: " + BACKEND_TYPE );
		}
	}

	public static BackendConfiguration hashBasedSharding(int shardCount) {
		switch ( BACKEND_TYPE ) {
			case "lucene":
				return new DocumentationLuceneBackendConfiguration() {
					@Override
					public <C extends MappingSetupHelper<C, ?, ?, ?, ?>.AbstractSetupContext> C setup(C setupContext,
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
				return new DocumentationElasticsearchBackendConfiguration() {
					@Override
					public <C extends MappingSetupHelper<C, ?, ?, ?, ?>.AbstractSetupContext> C setup(C setupContext,
							String backendNameOrNull, TestConfigurationProvider configurationProvider) {
						return super.setup( setupContext, backendNameOrNull, configurationProvider )
								.withBackendProperty( ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE,
										"index-settings-for-tests/" + shardCount + "-shards.json" );
					}
				};
			default:
				throw new IllegalStateException( "Unknown backend type: " + BACKEND_TYPE );
		}
	}

}
