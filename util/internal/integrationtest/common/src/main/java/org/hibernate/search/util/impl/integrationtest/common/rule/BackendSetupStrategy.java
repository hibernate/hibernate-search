/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

import org.junit.rules.TestRule;

public interface BackendSetupStrategy {

	default Optional<TestRule> getTestRule() {
		return Optional.empty();
	}

	<C extends MappingSetupHelper<C, ?, ?>.AbstractSetupContext> C start(C setupContext,
			TestConfigurationProvider configurationProvider);

	static BackendSetupStrategy withBackendMocks(BackendMock defaultBackendMock, BackendMock ... otherBackendMocks) {
		return new BackendMockSetupStrategy( defaultBackendMock, otherBackendMocks );
	}

	static BackendSetupStrategy withSingleBackend(String backendName, BackendConfiguration backendConfiguration) {
		return withMultipleBackends( backendName, Collections.singletonMap( backendName, backendConfiguration ) );
	}

	static BackendSetupStrategy withMultipleBackends(String defaultBackendName,
			Map<String, BackendConfiguration> backendConfigurations) {
		return new ActualBackendSetupStrategy( defaultBackendName, backendConfigurations );
	}

}
