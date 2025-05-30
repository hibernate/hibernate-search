/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.scope.impl;

import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.scope.spi.V5MigrationSearchScope;

public class V5MigrationOrmSearchScopeAdapter implements V5MigrationSearchScope {

	private final SearchScope<?> delegate;

	public V5MigrationOrmSearchScopeAdapter(SearchScope<?> delegate) {
		this.delegate = delegate;
	}

	@Override
	public Set<Class<?>> targetTypes() {
		return delegate.includedTypes().stream().map( SearchIndexedEntity::javaClass ).collect( Collectors.toSet() );
	}

	@Override
	public Set<IndexManager> indexManagers() {
		return delegate.includedTypes().stream().map( SearchIndexedEntity::indexManager ).collect( Collectors.toSet() );
	}

	@Override
	public SearchPredicateFactory predicate() {
		return delegate.predicate();
	}

	@Override
	public SearchSortFactory sort() {
		return delegate.sort();
	}

	@Override
	public SearchProjectionFactory<?, ?> projection() {
		return delegate.projection();
	}

	@Override
	public SearchProjection<Object> idProjection() {
		var factory = delegate.projection();
		// Not using factory.id() because that one throws an exception if IDs have inconsistent types.
		return factory.composite().from( factory.entityReference() )
				.as( EntityReference::id ).toProjection();
	}

	@Override
	public SearchProjection<? extends Class<?>> objectClassProjection() {
		var factory = delegate.projection();
		return factory.composite().from( factory.entityReference() )
				.as( EntityReference::type ).toProjection();
	}

	@Override
	public SearchAggregationFactory aggregation() {
		return delegate.aggregation();
	}

	public SearchScope<?> toSearchScope() {
		return delegate;
	}
}
