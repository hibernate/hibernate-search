/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.configuration;

import java.util.Properties;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.multitenancy.MultiTenancyStrategyName;
import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.IndexSettings;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategyNames;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;

public class MyConfiguration {
	// tag::build-hibernate-configuration[]
	protected Properties buildHibernateConfiguration() {
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
				MultiTenancyStrategyName.DISCRIMINATOR
		);
		config.put( BackendSettings.backendKey( myBackend, ElasticsearchBackendSettings.VERSION ), "7.6" );
		config.put(
				BackendSettings.backendKey( myBackend, ElasticsearchBackendSettings.VERSION_CHECK_ENABLED ), "false" );
		// index configuration
		config.put(
				IndexSettings.indexDefaultsKey( myBackend, ElasticsearchIndexSettings.LIFECYCLE_STRATEGY ),
				IndexLifecycleStrategyName.NONE
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
}
