/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.scope.spi;

import java.util.Set;

import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.search.aggregation.dsl.TypedSearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.TypedSearchProjectionFactory;
import org.hibernate.search.engine.search.sort.dsl.TypedSearchSortFactory;

/**
 * @deprecated This class will be removed without replacement. Use actual API instead.
 */
@Deprecated
public interface V5MigrationSearchScope {

	Set<Class<?>> targetTypes();

	Set<IndexManager> indexManagers();

	TypedSearchPredicateFactory<?> predicate();

	TypedSearchSortFactory<?> sort();

	TypedSearchProjectionFactory<?, ?, ?> projection();

	SearchProjection<Object> idProjection();

	SearchProjection<? extends Class<?>> objectClassProjection();

	TypedSearchAggregationFactory<?> aggregation();

}
