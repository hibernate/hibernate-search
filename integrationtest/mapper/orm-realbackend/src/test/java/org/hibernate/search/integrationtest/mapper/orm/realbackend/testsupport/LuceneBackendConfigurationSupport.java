/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.realbackend.testsupport;

import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.extension.MappingSetupHelper;

class LuceneBackendConfigurationSupport {

	private LuceneBackendConfigurationSupport() {
	}

	public static BackendConfiguration simple() {
		return new LuceneBackendConfiguration();
	}

	public static BackendConfiguration hashBasedSharding(int shardCount) {
		return new LuceneBackendConfiguration() {
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
	}

}
