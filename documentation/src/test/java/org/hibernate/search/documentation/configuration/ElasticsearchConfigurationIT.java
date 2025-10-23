/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Properties;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.IndexSettings;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.pojo.work.IndexingPlanSynchronizationStrategyNames;

import org.junit.jupiter.api.Test;

class ElasticsearchConfigurationIT {
	// tag::build-hibernate-configuration[]
	private Properties buildHibernateConfiguration() {
		Properties config = new Properties();
		// backend configuration
		config.put( BackendSettings.backendKey( ElasticsearchBackendSettings.HOSTS ), "127.0.0.1:9200" );
		config.put( BackendSettings.backendKey( ElasticsearchBackendSettings.PROTOCOL ), "http" );
		// index configuration
		config.put(
				IndexSettings.indexKey( "myIndex", ElasticsearchIndexSettings.INDEXING_MAX_BULK_SIZE ),
				20
		);
		// orm configuration
		config.put(
				HibernateOrmMapperSettings.INDEXING_PLAN_SYNCHRONIZATION_STRATEGY,
				IndexingPlanSynchronizationStrategyNames.ASYNC
		);
		// engine configuration
		config.put( EngineSettings.BACKGROUND_FAILURE_HANDLER, "myFailureHandler" );
		return config;
	}
	// end::build-hibernate-configuration[]

	@Test
	void shouldBuildHibernateConfiguration() {
		assertThat( buildHibernateConfiguration() )
				.containsOnly(
						entry( "hibernate.search.backend.hosts", "127.0.0.1:9200" ),
						entry( "hibernate.search.backend.protocol", "http" ),
						entry( "hibernate.search.backend.indexes.myIndex.indexing.max_bulk_size", 20 ),
						entry( "hibernate.search.indexing.plan.synchronization.strategy", "async" ),
						entry( "hibernate.search.background_failure_handler", "myFailureHandler" )
				);
	}
}
