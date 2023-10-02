/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.testsupport;

import java.util.Map;

import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchBackendConfiguration;

public class DocumentationElasticsearchBackendConfiguration extends ElasticsearchBackendConfiguration {

	@Override
	public Map<String, String> rawBackendProperties() {
		Map<String, String> properties = super.rawBackendProperties();
		properties.put(
				"analysis.configurer",
				"constructor:" + ElasticsearchSimpleMappingAnalysisConfigurer.class.getName()
		);
		return properties;
	}
}
