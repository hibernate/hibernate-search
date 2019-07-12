/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.List;

import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.index.impl.StubBackendFactory;

class BackendMockSetupStrategy implements BackendSetupStrategy {
	private final String defaultBackendName;
	private final List<BackendMock> backendMocks;

	BackendMockSetupStrategy(BackendMock defaultBackendMock, BackendMock ... otherBackendMocks) {
		this.defaultBackendName = defaultBackendMock.getBackendName();
		this.backendMocks = CollectionHelper.asList( defaultBackendMock, otherBackendMocks );
	}

	@Override
	public <C extends MappingSetupHelper<C, ?, ?>.AbstractSetupContext> C start(C setupContext,
			TestConfigurationProvider configurationProvider) {
		for ( BackendMock backendMock : backendMocks ) {
			setupContext = setupContext.withBackendProperty(
					backendMock.getBackendName(), "type", StubBackendFactory.class.getName()
			);
		}
		return setupContext.withPropertyRadical( EngineSettings.DEFAULT_BACKEND, defaultBackendName );
	}
}
