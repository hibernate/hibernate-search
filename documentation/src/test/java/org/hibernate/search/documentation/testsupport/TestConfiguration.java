/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.testsupport;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.mapper.orm.DatabaseContainer;

public final class TestConfiguration {

	private TestConfiguration() {
	}

	/**
	 * @param configurationProvider A test configuration provider.
	 *
	 * @return Configuration properties to use when bootstrapping Hibernate ORM with JPA.
	 */
	public static Map<String, Object> ormMapperProperties(TestConfigurationProvider configurationProvider) {
		Map<String, Object> properties = databaseConnectionProperties();

		// Hack: override example properties set in persistence.xml
		properties.put( "hibernate.search.backend.hosts", "" );
		properties.put( "hibernate.search.backend.protocol", "" );

		Map<String, String> backendProperties = BackendConfigurations.plain()
				.backendProperties( configurationProvider );
		for ( Map.Entry<String, String> entry : backendProperties.entrySet() ) {
			properties.put( "hibernate.search.backend." + entry.getKey(), entry.getValue() );
		}
		properties.put( "hibernate.search.schema_management.strategy", "drop-and-create-and-drop" );
		properties.put( "hibernate.search.indexing.plan.synchronization.strategy", "sync" );

		return properties;
	}

	public static Map<String, Object> databaseConnectionProperties() {
		Map<String, Object> properties = new HashMap<>();
		DatabaseContainer.configuration().add( properties );
		return properties;
	}

	/**
	 * @param configurationProvider A test configuration provider.
	 * @param backendConfiguration The backend configuration.
	 *
	 * @return Configuration properties to use when bootstrapping a Standalone POJO Mapper.
	 */
	public static Map<String, String> standalonePojoMapperProperties(TestConfigurationProvider configurationProvider,
			BackendConfiguration backendConfiguration) {
		Map<String, String> properties = new HashMap<>();

		// Hack: override example properties set by caller
		properties.put( "hibernate.search.backend.hosts", "" );
		properties.put( "hibernate.search.backend.protocol", "" );

		Map<String, String> backendProperties = backendConfiguration
				.backendProperties( configurationProvider );
		for ( Map.Entry<String, String> entry : backendProperties.entrySet() ) {
			properties.put( "hibernate.search.backend." + entry.getKey(), entry.getValue() );
		}
		properties.put( "hibernate.search.schema_management.strategy", "drop-and-create-and-drop" );
		properties.put( "hibernate.search.indexing.plan.synchronization.strategy", "sync" );

		return properties;
	}

}
