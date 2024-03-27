/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.testsupport.migration;

import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.scope.spi.V5MigrationSearchScope;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.util.common.impl.CollectionHelper;

public class V5MigrationStandalonePojoSearchIntegratorAdapter implements SearchIntegrator {
	private final SearchMapping delegate;

	public V5MigrationStandalonePojoSearchIntegratorAdapter(SearchMapping delegate) {
		this.delegate = delegate;
	}

	@Override
	public V5MigrationSearchScope scope(Class<?>... targetTypes) {
		return new V5MigrationStandalonePojoSearchScopeAdapter( delegate.scope( CollectionHelper.asSet( targetTypes ) ) );
	}
}
