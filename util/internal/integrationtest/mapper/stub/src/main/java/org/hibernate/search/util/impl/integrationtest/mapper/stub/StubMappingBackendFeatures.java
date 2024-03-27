/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.stub;

public interface StubMappingBackendFeatures {

	default boolean supportsExplicitRefresh() {
		return true;
	}

}
