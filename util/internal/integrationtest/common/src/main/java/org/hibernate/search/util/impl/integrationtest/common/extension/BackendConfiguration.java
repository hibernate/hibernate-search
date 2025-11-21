/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.extension;


import static org.hibernate.search.util.impl.test.logging.impl.TestLog.TEST_LOGGER;

import java.util.Map;

import org.hibernate.search.engine.cfg.BackendSettings;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

public abstract class BackendConfiguration {

	private static final String BACKEND_TYPE_PROPERTY_KEY = "org.hibernate.search.integrationtest.backend.type";
	private static final String ELASTICSEARCH_BACKEND_CLIENT_TYPE_PROPERTY_KEY =
			"org.hibernate.search.integrationtest.backend.elasticsearch.client.type";

	// Uncomment one of the following lines to set the backend type when running tests from the IDE
	public static final String IDE_BACKEND_TYPE = "lucene";
	//	public static final String IDE_BACKEND_TYPE = "elasticsearch";

	// Uncomment one of the following lines to set the backend type when running tests from the IDE
	public static final String IDE_ELASTICSEARCH_BACKEND_CLIENT_TYPE = null;
	// public static final String IDE_ELASTICSEARCH_BACKEND_CLIENT_TYPE = "default";
	//	public static final String IDE_ELASTICSEARCH_BACKEND_CLIENT_TYPE = "jdk-rest-client";
	//	public static final String IDE_ELASTICSEARCH_BACKEND_CLIENT_TYPE = "opensearch-rest-client";
	//	public static final String IDE_ELASTICSEARCH_BACKEND_CLIENT_TYPE = "elasticsearch-rest5";

	public static final String BACKEND_TYPE;
	public static final String ELASTICSEARCH_BACKEND_CLIENT_TYPE;
	public static final boolean IS_IDE;
	static {
		String property = System.getProperty( BACKEND_TYPE_PROPERTY_KEY );
		if ( property == null ) {
			BACKEND_TYPE = IDE_BACKEND_TYPE;
			IS_IDE = true;
			TEST_LOGGER.warn( "The backend type wasn't set; tests are probably running from an IDE."
					+ " Defaulting to backend type '" + BACKEND_TYPE + "' and setting it explicitly"
					+ " to avoid problems with classpaths containing multiple backend types." );
			TEST_LOGGER.warn( "To test another backend type, change the constant 'IDE_BACKEND_TYPE' in class '"
					+ BackendConfiguration.class.getName() + "'." );
			TEST_LOGGER.warn( "Tests of the backend type auto-detection feature will not work properly." );
		}
		else {
			BACKEND_TYPE = property;
			IS_IDE = false;
		}
		if ( isElasticsearch() ) {
			property = System.getProperty( ELASTICSEARCH_BACKEND_CLIENT_TYPE_PROPERTY_KEY );
			if ( property == null ) {
				ELASTICSEARCH_BACKEND_CLIENT_TYPE = IDE_ELASTICSEARCH_BACKEND_CLIENT_TYPE;
			}
			else {
				ELASTICSEARCH_BACKEND_CLIENT_TYPE = property;
			}
		}
		else {
			ELASTICSEARCH_BACKEND_CLIENT_TYPE = null;
		}
	}

	public static boolean isElasticsearch() {
		return "elasticsearch".equals( BACKEND_TYPE );
	}

	public static boolean isLucene() {
		return "lucene".equals( BACKEND_TYPE );
	}

	public <C extends MappingSetupHelper<C, ?, ?, ?, ?>.AbstractSetupContext> C setup(C setupContext,
			String backendNameOrNull, TestConfigurationProvider configurationProvider) {
		setupContext = setupContext
				.withBackendProperties( backendNameOrNull, backendProperties( configurationProvider ) );

		return setupContext;
	}

	public final Map<String, String> backendProperties(TestConfigurationProvider configurationProvider) {
		Map<String, String> rawBackendProperties = rawBackendProperties();
		if ( IS_IDE ) {
			// More than one backend type in the classpath, we have to set it explicitly.
			rawBackendProperties.put( BackendSettings.TYPE, BACKEND_TYPE );
		}
		if ( ELASTICSEARCH_BACKEND_CLIENT_TYPE != null ) {
			rawBackendProperties.put( "client_factory", ELASTICSEARCH_BACKEND_CLIENT_TYPE );
		}
		return configurationProvider.interpolateProperties( rawBackendProperties );
	}

	public abstract Map<String, String> rawBackendProperties();

	public abstract boolean supportsExplicitPurge();

	public abstract boolean supportsExplicitRefresh();

}
