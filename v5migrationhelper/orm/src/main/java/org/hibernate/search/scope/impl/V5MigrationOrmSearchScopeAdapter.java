/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.scope.impl;

import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.mapper.orm.common.EntityReference;
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
		SearchProjectionFactory<EntityReference, ?> factory = delegate.projection();
		return factory.composite( EntityReference::id, factory.entityReference() ).toProjection();
	}

	@Override
	public SearchProjection<? extends Class<?>> objectClassProjection() {
		SearchProjectionFactory<EntityReference, ?> factory = delegate.projection();
		return factory.composite( EntityReference::type, factory.entityReference() ).toProjection();
	}

	@Override
	public SearchAggregationFactory aggregation() {
		return delegate.aggregation();
	}

	public SearchScope<?> toSearchScope() {
		return delegate;
	}
}
