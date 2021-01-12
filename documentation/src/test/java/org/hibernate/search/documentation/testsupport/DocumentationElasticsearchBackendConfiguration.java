/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
