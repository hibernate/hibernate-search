/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

import org.hibernate.search.engine.backend.reporting.spi.BackendMappingHints;

public final class StubMappingHints implements BackendMappingHints {
	public static final StubMappingHints INSTANCE = new StubMappingHints();

	private StubMappingHints() {
	}

	@Override
	public String noEntityProjectionAvailable() {
		return getClass().getName() + "#noEntityProjectionAvailable";
	}
}
