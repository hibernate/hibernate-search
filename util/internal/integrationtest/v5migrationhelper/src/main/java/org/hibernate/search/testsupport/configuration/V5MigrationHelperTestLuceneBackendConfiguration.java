/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.configuration;

import java.util.Map;

import org.hibernate.search.util.impl.integrationtest.backend.lucene.LuceneBackendConfiguration;

public class V5MigrationHelperTestLuceneBackendConfiguration extends LuceneBackendConfiguration {
	@Override
	protected Map<String, Object> backendProperties() {
		Map<String, Object> properties = super.backendProperties();
		properties.put(
				"analysis.configurer",
				new V5MigrationHelperTestDefaultLuceneAnalysisConfigurer()
		);
		return properties;
	}

}
