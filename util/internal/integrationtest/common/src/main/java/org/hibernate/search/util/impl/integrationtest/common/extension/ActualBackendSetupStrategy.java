/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.common.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.BackendMappingHandle;

class ActualBackendSetupStrategy implements BackendSetupStrategy {
	private final BackendConfiguration defaultBackendConfiguration;
	private final Map<String, BackendConfiguration> namedBackendConfigurations;
	private final List<BackendConfiguration> allConfigurations;

	ActualBackendSetupStrategy(BackendConfiguration defaultBackendConfiguration,
			Map<String, BackendConfiguration> namedBackendConfigurations) {
		this.defaultBackendConfiguration = defaultBackendConfiguration;
		this.namedBackendConfigurations = namedBackendConfigurations;
		allConfigurations = new ArrayList<>();
		if ( defaultBackendConfiguration != null ) {
			allConfigurations.add( defaultBackendConfiguration );
		}
		allConfigurations.removeIf( Objects::isNull );
	}

	@Override
	public String toString() {
		return namedBackendConfigurations.isEmpty()
				? defaultBackendConfiguration.toString()
				: allConfigurations.toString();
	}

	@Override
	public <C extends MappingSetupHelper<C, ?, ?, ?, ?>.AbstractSetupContext> C start(C setupContext,
			TestConfigurationProvider configurationProvider,
			// The mapping handle is not used by actual backends (only by BackendMock).
			CompletionStage<BackendMappingHandle> mappingHandlePromise) {
		if ( defaultBackendConfiguration != null ) {
			setupContext = defaultBackendConfiguration.setup( setupContext, null, configurationProvider );
		}
		for ( Map.Entry<String, BackendConfiguration> entry : namedBackendConfigurations.entrySet() ) {
			String name = entry.getKey();
			BackendConfiguration configuration = entry.getValue();
			setupContext = configuration.setup( setupContext, name, configurationProvider );
		}
		return setupContext;
	}

	@Override
	public boolean supportsExplicitRefresh() {
		return allConfigurations.stream().allMatch( BackendConfiguration::supportsExplicitRefresh );
	}

	@Override
	public boolean isMockBackend() {
		return false;
	}
}
