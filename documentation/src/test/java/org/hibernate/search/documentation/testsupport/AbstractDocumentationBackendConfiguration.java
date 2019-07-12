/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.documentation.testsupport;

import java.util.Map;

import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendConfiguration;

abstract class AbstractDocumentationBackendConfiguration implements BackendConfiguration {

	static Map<String, Object> getBackendProperties(
			TestConfigurationProvider configurationProvider, String configurationId) {
		return configurationProvider.getPropertiesFromFile(
				"/hibernate-test-" + configurationId + ".properties"
		);
	}
}
