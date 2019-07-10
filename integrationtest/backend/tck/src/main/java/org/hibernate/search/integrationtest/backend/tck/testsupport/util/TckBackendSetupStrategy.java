/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.testsupport.util;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.rules.TestRule;

public interface TckBackendSetupStrategy {

	Optional<TestRule> getTestRule();

	ConfigurationPropertySource createBackendConfigurationPropertySource(TestConfigurationProvider configurationProvider);

	SearchSetupHelper.SetupContext startSetup(SearchSetupHelper.SetupContext setupHelper);

	static TckBackendSetupStrategy of(String propertiesClasspathResourcePath) {
		return of( propertiesClasspathResourcePath, null );
	}

	static TckBackendSetupStrategy of(String propertiesClasspathResourcePath, Map<String, Object> overrides) {
		return new DefaultTckBackendSetupStrategy( propertiesClasspathResourcePath, overrides );
	}

}
