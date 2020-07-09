/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import java.util.Map;

import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;

class BackendMockSetupStrategy implements BackendSetupStrategy {
	private final BackendMock defaultBackendMock;
	private final Map<String, BackendMock> namedBackendMocks;

	BackendMockSetupStrategy(BackendMock defaultBackendMock, Map<String, BackendMock> namedBackendMocks) {
		this.defaultBackendMock = defaultBackendMock;
		this.namedBackendMocks = namedBackendMocks;
	}

	@Override
	public <C extends MappingSetupHelper<C, ?, ?>.AbstractSetupContext> C start(C setupContext,
			TestConfigurationProvider configurationProvider) {
		setupContext = setupContext.withBackendProperty( "type",
				defaultBackendMock.factory() );
		for ( Map.Entry<String, BackendMock> entry : namedBackendMocks.entrySet() ) {
			setupContext = setupContext.withBackendProperty( entry.getKey(),
					"type", entry.getValue().factory() );
		}
		return setupContext;
	}
}
