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

class DefaultTckBackendSetupStrategy implements TckBackendSetupStrategy {
	private final String propertiesClasspathResourcePath;
	private final Map<String, Object> overrides;

	DefaultTckBackendSetupStrategy(String propertiesClasspathResourcePath, Map<String, Object> overrides) {
		this.propertiesClasspathResourcePath = propertiesClasspathResourcePath;
		this.overrides = overrides;
	}

	@Override
	public Optional<TestRule> getTestRule() {
		return Optional.empty();
	}

	@Override
	public ConfigurationPropertySource createBackendConfigurationPropertySource(
			TestConfigurationProvider configurationProvider) {
		ConfigurationPropertySource original = ConfigurationPropertySource.fromMap(
				configurationProvider.getPropertiesFromFile( propertiesClasspathResourcePath )
		);

		if ( overrides != null && !overrides.isEmpty() ) {
			return original.withOverride( ConfigurationPropertySource.fromMap( overrides ) );
		}
		else {
			return original;
		}
	}

	@Override
	public SearchSetupHelper.SetupContext startSetup(SearchSetupHelper.SetupContext setupContext) {
		return setupContext;
	}

}
