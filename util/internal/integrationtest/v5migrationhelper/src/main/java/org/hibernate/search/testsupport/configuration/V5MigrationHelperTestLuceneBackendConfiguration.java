/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.testsupport.configuration;

import java.util.Map;

import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;

public class V5MigrationHelperTestLuceneBackendConfiguration extends LuceneBackendConfiguration {
	@Override
	public Map<String, String> rawBackendProperties() {
		Map<String, String> properties = super.rawBackendProperties();
		properties.put(
				"analysis.configurer",
				"constructor:" + V5MigrationHelperTestDefaultLuceneAnalysisConfigurer.class.getName()
		);
		return properties;
	}

}
