/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

public final class JpaConfiguration {

	private JpaConfiguration() {
	}

	/**
	 * @param configurationProvider A test configuration provider.
	 *
	 * @return Configuration properties to use when bootstrapping Hibernate ORM with JPA.
	 */
	public static Map<String, String> properties(TestConfigurationProvider configurationProvider) {
		Map<String, String> properties = new HashMap<>();
		Map<String, String> backendProperties = BackendConfigurations.plain()
				.backendProperties( configurationProvider );
		for ( Map.Entry<String, String> entry : backendProperties.entrySet() ) {
			properties.put( "hibernate.search.backend." + entry.getKey(), entry.getValue() );
		}
		properties.put( "hibernate.search.schema_management.strategy", "drop-and-create-and-drop" );
		properties.put( "hibernate.search.automatic_indexing.synchronization.strategy", "sync" );

		// Hack: override example properties from persistence.xml
		properties.put( "hibernate.search.backend.hosts", "" );
		properties.put( "hibernate.search.backend.protocol", "" );

		return properties;
	}

}
