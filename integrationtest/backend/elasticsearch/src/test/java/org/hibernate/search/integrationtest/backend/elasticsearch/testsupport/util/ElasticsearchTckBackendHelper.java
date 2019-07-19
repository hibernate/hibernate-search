/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.AnalysisCustomITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.AnalysisOverrideITAnalysisConfigurer;
import org.hibernate.search.util.impl.integrationtest.elasticsearch.ElasticsearchTestHostConnectionConfiguration;
import org.hibernate.search.util.impl.integrationtest.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendHelper;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendSetupStrategy;

public class ElasticsearchTckBackendHelper implements TckBackendHelper {

	public static final String DEFAULT_BACKEND_PROPERTIES_PATH = "/backend-defaults.properties";

	private final ElasticsearchTckBackendFeatures features = new ElasticsearchTckBackendFeatures( ElasticsearchTestDialect.get() );

	@Override
	public TckBackendFeatures getBackendFeatures() {
		return features;
	}

	@Override
	public TckBackendSetupStrategy createDefaultBackendSetupStrategy() {
		return TckBackendSetupStrategy.of( DEFAULT_BACKEND_PROPERTIES_PATH, createProperties() );
	}

	@Override
	public TckBackendSetupStrategy createMultiTenancyBackendSetupStrategy() {
		return TckBackendSetupStrategy.of(
				DEFAULT_BACKEND_PROPERTIES_PATH,
				createProperties( c -> c.accept( "multi_tenancy_strategy", "discriminator" ) )
		);
	}

	@Override
	public TckBackendSetupStrategy createAnalysisCustomBackendSetupStrategy() {
		return TckBackendSetupStrategy.of(
				DEFAULT_BACKEND_PROPERTIES_PATH,
				createProperties( c ->
						c.accept( "analysis_configurer", AnalysisCustomITAnalysisConfigurer.class.getName() )
				)
		);
	}

	@Override
	public TckBackendSetupStrategy createAnalysisOverrideBackendSetupStrategy() {
		return TckBackendSetupStrategy.of(
				DEFAULT_BACKEND_PROPERTIES_PATH,
				createProperties( c ->
						c.accept( "analysis_configurer", AnalysisOverrideITAnalysisConfigurer.class.getName() )
				)
		);
	}

	private Map<String, Object> createProperties() {
		Map<String, Object> map = new LinkedHashMap<>();
		// Always add configuration options that allow to connect to Elasticsearch
		ElasticsearchTestHostConnectionConfiguration.get().addToBackendProperties( map );
		return map;
	}

	private Map<String, Object> createProperties(Consumer<BiConsumer<String, Object>> overrides) {
		Map<String, Object> map = createProperties();
		overrides.accept( map::put );
		return map;
	}
}
