/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.testsupport;


import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.extension.MappingSetupHelper;

public final class ElasticsearchBackendConfigurationSupport {

	private ElasticsearchBackendConfigurationSupport() {
	}

	// Plain configuration, without analysis configurers
	public static BackendConfiguration plain() {
		return new ElasticsearchBackendConfiguration();
	}

	public static BackendConfiguration simple() {
		return new DocumentationElasticsearchBackendConfiguration();
	}

	public static BackendConfiguration hashBasedSharding(int shardCount) {
		return new DocumentationElasticsearchBackendConfiguration() {
			@Override
			public <C extends MappingSetupHelper<C, ?, ?, ?, ?>.AbstractSetupContext> C setup(C setupContext,
					String backendNameOrNull, TestConfigurationProvider configurationProvider) {
				return super.setup( setupContext, backendNameOrNull, configurationProvider )
						.withBackendProperty( ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE,
								"index-settings-for-tests/" + shardCount + "-shards.json" );
			}

		};
	}

	public static boolean isVectorSearchSupportedByElasticsearch() {
		return ElasticsearchTestDialect.isActualVersion(
				es -> !es.isLessThan( "8.12.0" ),
				os -> !os.isLessThan( "2.9.0" ),
				aoss -> true
		);
	}

	public static boolean isKnnSupportedByElasticsearch() {
		return ElasticsearchTestDialect.isActualVersion(
				es -> !es.isLessThan( "8.12.0" ),
				os -> !os.isLessThan( "2.9.0" ),
				aoss -> true
		);
	}

	public static boolean isKnnSimilaritySupportedByElasticsearch() {
		return ElasticsearchTestDialect.isActualVersion(
				es -> !es.isLessThan( "8.12.0" ),
				os -> false,
				aoss -> false
		);
	}

}
