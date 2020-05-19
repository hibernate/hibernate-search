/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Properties;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.backend.elasticsearch.multitenancy.MultiTenancyStrategyName;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.IndexSettings;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;

import org.junit.Test;

public class MyConfigurationIT {
	// tag::build-hibernate-configuration[]
	private Properties buildHibernateConfiguration() {
		Properties config = new Properties();
		// add hibernate configuration
		String myBackend = "myBackend";
		// backend configuration
		config.put( BackendSettings.backendKey( myBackend, ElasticsearchBackendSettings.HOSTS ), "127.0.0.1:9200" );
		config.put( BackendSettings.backendKey( myBackend, ElasticsearchBackendSettings.PROTOCOL ), "http" );
		config.put(
				BackendSettings.backendKey( myBackend, BackendSettings.TYPE ), ElasticsearchBackendSettings.TYPE_NAME );
		config.put(
				BackendSettings.backendKey( myBackend, ElasticsearchBackendSettings.MULTI_TENANCY_STRATEGY ),
				MultiTenancyStrategyName.DISCRIMINATOR.externalRepresentation()
		);
		config.put( BackendSettings.backendKey( myBackend, ElasticsearchBackendSettings.VERSION ), "7.7" );
		config.put(
				BackendSettings.backendKey( myBackend, ElasticsearchBackendSettings.VERSION_CHECK_ENABLED ), "false" );
		// index configuration
		config.put(
				IndexSettings.indexDefaultsKey( myBackend, ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS ),
				IndexStatus.YELLOW.externalRepresentation()
		);
		// orm configuration
		config.put(
				HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
				AutomaticIndexingSynchronizationStrategyNames.ASYNC
		);
		// engine configuration
		config.put( EngineSettings.BACKGROUND_FAILURE_HANDLER, "myFailureHandler" );
		config.put( EngineSettings.DEFAULT_BACKEND, myBackend );
		return config;
	}
	// end::build-hibernate-configuration[]

	@Test
	public void shouldBuildHibernateConfiguration() {
		assertThat( buildHibernateConfiguration() )
				.containsOnly(
						entry( "hibernate.search.backends.myBackend.hosts", "127.0.0.1:9200" ),
						entry( "hibernate.search.backends.myBackend.protocol", "http" ),
						entry( "hibernate.search.backends.myBackend.type", "elasticsearch" ),
						entry( "hibernate.search.backends.myBackend.multi_tenancy.strategy", "discriminator" ),
						entry( "hibernate.search.backends.myBackend.version", "7.7" ),
						entry( "hibernate.search.backends.myBackend.version_check.enabled", "false" ),
						entry( "hibernate.search.backends.myBackend.index_defaults.schema_management.minimal_required_status", "yellow" ),
						entry( "hibernate.search.automatic_indexing.synchronization.strategy", "async" ),
						entry( "hibernate.search.background_failure_handler", "myFailureHandler" ),
						entry( "hibernate.search.default_backend", "myBackend" )
				);
	}
}
