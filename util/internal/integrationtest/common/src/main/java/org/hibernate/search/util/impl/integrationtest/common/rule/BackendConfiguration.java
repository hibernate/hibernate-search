/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.rules.TestRule;

public abstract class BackendConfiguration {

	public Optional<TestRule> testRule() {
		return Optional.empty();
	}

	public <C extends MappingSetupHelper<C, ?, ?>.AbstractSetupContext> C setup(C setupContext,
			String backendNameOrNull, TestConfigurationProvider configurationProvider) {
		return setupContext
				.withBackendProperties(
						backendNameOrNull,
						configurationProvider.interpolateProperties( backendProperties() )
				);
	}

	protected abstract Map<String, Object> backendProperties();

}
