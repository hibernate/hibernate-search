/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.DefaultITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

class ElasticsearchTckBackendSetupStrategy extends TckBackendSetupStrategy<ElasticsearchBackendConfiguration> {
	private static final String ELASTICSEARCH_BACKEND_CLIENT_TYPE_PROPERTY_KEY =
			"org.hibernate.search.integrationtest.backend.elasticsearch.client.type";
	private static final String ELASTICSEARCH_BACKEND_CLIENT_TYPE;

	// Uncomment one of the following lines to set the backend type when running tests from the IDE
	public static final String IDE_ELASTICSEARCH_BACKEND_CLIENT_TYPE = null;
	// public static final String IDE_ELASTICSEARCH_BACKEND_CLIENT_TYPE = "default";
	//	public static final String IDE_ELASTICSEARCH_BACKEND_CLIENT_TYPE = "jdk-rest-client";
	//	public static final String IDE_ELASTICSEARCH_BACKEND_CLIENT_TYPE = "opensearch-rest-client";
	//	public static final String IDE_ELASTICSEARCH_BACKEND_CLIENT_TYPE = "elasticsearch-rest5";

	static {
		String property = System.getProperty( ELASTICSEARCH_BACKEND_CLIENT_TYPE_PROPERTY_KEY );
		if ( property == null ) {
			ELASTICSEARCH_BACKEND_CLIENT_TYPE = IDE_ELASTICSEARCH_BACKEND_CLIENT_TYPE;
		}
		else {
			ELASTICSEARCH_BACKEND_CLIENT_TYPE = property;
		}
	}

	ElasticsearchTckBackendSetupStrategy() {
		super( new ElasticsearchBackendConfiguration() );
		setProperty( "analysis.configurer", BeanReference.ofInstance( new DefaultITAnalysisConfigurer() ) );
		if ( ELASTICSEARCH_BACKEND_CLIENT_TYPE != null ) {
			setProperty( ElasticsearchBackendSettings.CLIENT_FACTORY, ELASTICSEARCH_BACKEND_CLIENT_TYPE );
		}
	}

	@Override
	public TckBackendAccessor createBackendAccessor(TestConfigurationProvider configurationProvider) {
		TestElasticsearchClient client = TestElasticsearchClient.create();
		client.open( configurationProvider );
		return new ElasticsearchTckBackendAccessor( client );
	}

}
