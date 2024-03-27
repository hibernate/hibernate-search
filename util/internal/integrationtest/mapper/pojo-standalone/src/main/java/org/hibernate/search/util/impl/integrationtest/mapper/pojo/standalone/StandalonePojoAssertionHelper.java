/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone;

import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.util.impl.integrationtest.common.assertion.MappingAssertionHelper;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendConfiguration;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendSetupStrategy;

public class StandalonePojoAssertionHelper extends MappingAssertionHelper<SearchMapping> {
	public StandalonePojoAssertionHelper(BackendConfiguration backendConfiguration) {
		super( backendConfiguration );
	}

	public StandalonePojoAssertionHelper(BackendSetupStrategy backendSetupStrategy) {
		super( backendSetupStrategy );
	}

	@Override
	protected void doRefresh(SearchMapping entryPoint) {
		entryPoint.scope( Object.class ).workspace().refresh();
	}
}
