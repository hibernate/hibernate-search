/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.mapper.orm;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.util.impl.integrationtest.common.assertion.MappingAssertionHelper;
import org.hibernate.search.util.impl.integrationtest.common.extension.BackendSetupStrategy;

public class OrmAssertionHelper extends MappingAssertionHelper<EntityManagerFactory> {
	public OrmAssertionHelper(BackendSetupStrategy backendSetupStrategy) {
		super( backendSetupStrategy );
	}

	@Override
	protected void doRefresh(EntityManagerFactory entryPoint) {
		Search.mapping( entryPoint ).scope( Object.class ).workspace().refresh();
	}
}
