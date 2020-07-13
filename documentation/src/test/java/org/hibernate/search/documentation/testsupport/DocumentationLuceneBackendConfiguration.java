/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import java.util.Map;

import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;

public class DocumentationLuceneBackendConfiguration extends LuceneBackendConfiguration {
	@Override
	protected Map<String, Object> getBackendProperties() {
		Map<String, Object> properties = super.getBackendProperties();
		properties.put(
				"analysis.configurer",
				new LuceneSimpleMappingAnalysisConfigurer()
		);
		return properties;
	}

}
