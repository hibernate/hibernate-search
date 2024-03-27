/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.testsupport.migration;

import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.query.engine.spi.V5MigrationSearchSession;
import org.hibernate.search.scope.spi.V5MigrationSearchScope;

@SuppressWarnings("rawtypes")
public class V5MigrationStandalonePojoSearchSessionAdapter implements V5MigrationSearchSession {
	private final SearchSession delegate;

	public V5MigrationStandalonePojoSearchSessionAdapter(SearchSession delegate) {
		this.delegate = delegate;
	}

	@Override
	public SearchQuerySelectStep<?, ?, ?, ?, ?, ?> search(V5MigrationSearchScope scope) {
		return delegate.search( ( (V5MigrationStandalonePojoSearchScopeAdapter) scope ).toSearchScope() );
	}
}
