/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.impl;

import org.hibernate.search.mapper.orm.mapping.SearchMapping;
import org.hibernate.search.scope.impl.V5MigrationOrmSearchScopeAdapter;
import org.hibernate.search.scope.spi.V5MigrationSearchScope;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.util.common.impl.CollectionHelper;

public class V5MigrationOrmSearchIntegratorAdapter implements SearchIntegrator {
	private final SearchMapping delegate;

	public V5MigrationOrmSearchIntegratorAdapter(SearchMapping delegate) {
		this.delegate = delegate;
	}

	@Override
	public V5MigrationSearchScope scope(Class<?>... targetTypes) {
		return new V5MigrationOrmSearchScopeAdapter( delegate.scope( CollectionHelper.asSet( targetTypes ) ) );
	}

	public SearchMapping toSearchMapping() {
		return delegate;
	}
}
