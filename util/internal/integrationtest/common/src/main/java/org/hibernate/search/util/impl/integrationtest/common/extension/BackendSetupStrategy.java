/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.extension;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;

public interface BackendSetupStrategy {

	<C extends MappingSetupHelper<C, ?, ?, ?, ?>.AbstractSetupContext> C start(C setupContext,
			TestConfigurationProvider configurationProvider,
			CompletionStage<BackendMappingHandle> mappingHandlePromise);

	boolean supportsExplicitRefresh();

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

	boolean isMockBackend();
}
