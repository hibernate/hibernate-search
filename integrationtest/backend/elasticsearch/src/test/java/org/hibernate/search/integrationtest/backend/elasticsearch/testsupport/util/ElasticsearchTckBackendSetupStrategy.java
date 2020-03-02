/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.DefaultITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendAccessor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchTestHostConnectionConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.rules.TestRule;

class ElasticsearchTckBackendSetupStrategy implements TckBackendSetupStrategy {

	private final Map<String, Object> properties = new LinkedHashMap<>();

	ElasticsearchTckBackendSetupStrategy() {
		setProperty( "type", "elasticsearch" );
		setProperty( "log.json_pretty_printing", "true" );
		setProperty( "analysis.configurer", DefaultITAnalysisConfigurer.class.getName() );
		setProperty( "index_defaults.schema_management.minimal_required_status", "yellow" );
		// Always add configuration options that allow to connect to Elasticsearch
		ElasticsearchTestHostConnectionConfiguration.get().addToBackendProperties( properties );
	}

	ElasticsearchTckBackendSetupStrategy setProperty(String key, Object value) {
		properties.put( key, value );
		return this;
	}

	@Override
	public Optional<TestRule> getTestRule() {
		return Optional.empty();
	}

	@Override
	public ConfigurationPropertySource createBackendConfigurationPropertySource(
			TestConfigurationProvider configurationProvider) {
		return ConfigurationPropertySource.fromMap(
				configurationProvider.interpolateProperties( properties )
		);
	}

	@Override
	public TckBackendAccessor createBackendAccessor(TestConfigurationProvider configurationProvider) {
		TestElasticsearchClient client = new TestElasticsearchClient();
		client.open( configurationProvider );
		return new ElasticsearchTckBackendAccessor( client );
	}

	@Override
	public SearchSetupHelper.SetupContext startSetup(SearchSetupHelper.SetupContext setupContext) {
		return setupContext;
	}
}
