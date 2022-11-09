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
import java.util.concurrent.CompletionStage;

import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;

import org.junit.rules.TestRule;

public interface BackendSetupStrategy {

	default Optional<TestRule> getTestRule() {
		return Optional.empty();
	}

	<C extends MappingSetupHelper<C, ?, ?, ?>.AbstractSetupContext> C start(C setupContext,
			TestConfigurationProvider configurationProvider,
			CompletionStage<BackendMappingHandle> mappingHandlePromise);

	static BackendSetupStrategy withSingleBackendMock(BackendMock defaultBackendMock) {
		return new BackendMockSetupStrategy( defaultBackendMock, Collections.emptyMap() );
	}

	static BackendSetupStrategy withMultipleBackendMocks(BackendMock defaultBackendMock,
			Map<String, BackendMock> namedBackendMocks) {
		return new BackendMockSetupStrategy( defaultBackendMock, namedBackendMocks );
	}

	static BackendSetupStrategy withSingleBackend(BackendConfiguration backendConfiguration) {
		return withMultipleBackends( backendConfiguration, Collections.emptyMap() );
	}

	static BackendSetupStrategy withMultipleBackends(BackendConfiguration defaultBackendConfiguration,
			Map<String, BackendConfiguration> namedBackendConfigurations) {
		return new ActualBackendSetupStrategy( defaultBackendConfiguration, namedBackendConfigurations );
	}
}
