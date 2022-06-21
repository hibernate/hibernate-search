/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.definition;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;

public final class BridgeTestUtils {

	private BridgeTestUtils() {
	}

	public static BackendMappingContext toBackendMappingContext(SearchMapping mapping) {
		return (BackendMappingContext) mapping;
	}

	public static BackendSessionContext toBackendSessionContext(SearchSession session) {
		return (BackendSessionContext) session;
	}

}
