/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport;


import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.extension.MappingSetupHelper;

class ElasticsearchBackendConfigurationSupport {

	private ElasticsearchBackendConfigurationSupport() {
	}

	public static BackendConfiguration simple() {
		return new org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration();
	}

	public static BackendConfiguration hashBasedSharding(int shardCount) {
		return new org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration() {
			@Override
			public <C extends MappingSetupHelper<C, ?, ?, ?, ?>.AbstractSetupContext> C setup(C setupContext,
					String backendNameOrNull, TestConfigurationProvider configurationProvider) {
				return super.setup( setupContext, backendNameOrNull, configurationProvider )
						.withBackendProperty( ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE,
								"index-settings-for-tests/" + shardCount + "-shards.json" );
			}
		};
	}

}
