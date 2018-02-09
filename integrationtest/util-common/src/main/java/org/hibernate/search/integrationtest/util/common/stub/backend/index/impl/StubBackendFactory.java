/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common.stub.backend.index.impl;

import org.hibernate.search.engine.backend.spi.Backend;
import org.hibernate.search.engine.backend.spi.BackendFactory;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.BuildContext;

public class StubBackendFactory implements BackendFactory {
	@Override
	public Backend<?> create(String name, BuildContext context, ConfigurationPropertySource propertySource) {
		return new StubBackend( name );
	}
}
