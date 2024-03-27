/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
