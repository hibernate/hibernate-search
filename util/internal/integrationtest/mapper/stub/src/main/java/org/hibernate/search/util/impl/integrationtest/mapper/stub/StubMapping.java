/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.common.spi.SearchIntegration;

public interface StubMapping extends AutoCloseable, BackendMappingContext {

	@Override
	void close();

	SearchIntegration integration();

	StubSession session();

	StubSession session(String tenantId);

	/**
	 * @return A fixture for this mapping, to easily set various mapping-related components.
	 */
	StubMappingFixture with();

}
