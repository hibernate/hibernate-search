/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.scope.spi;

import java.util.Set;

import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.mapper.pojo.loading.spi.PojoSelectionLoadingContextBuilder;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexer;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;

/**
 * @param <R> The type of entity references, i.e. the type of hits returned by
 * {@link SearchQuerySelectStep#selectEntityReference()} reference queries},
 * @param <E> The type of loaded entities, i.e. the type of hits returned by
 * {@link SearchQuerySelectStep#selectEntity() entity queries},
 * or the type of objects returned for {@link SearchProjectionFactory#entity() entity projections}.
 * @param <C> The type of indexed type extended contexts; i.e. the type of elements in the set returned by
 * {@link #includedIndexedTypes()}.
 * or the type of objects returned for {@link SearchProjectionFactory#entity() entity projections}.
 */
public interface PojoScopeDelegate<R extends EntityReference, E, C> {

	Set<C> includedIndexedTypes();

	<LOS> SearchQuerySelectStep<?, R, E, LOS, SearchProjectionFactory<R, E>, ?> search(
			PojoScopeSessionContext sessionContext,
			PojoSelectionLoadingContextBuilder<LOS> loadingContextBuilder);

	SearchPredicateFactory predicate();

	SearchSortFactory sort();

	SearchProjectionFactory<R, E> projection();

	SearchAggregationFactory aggregation();

	SearchHighlighterFactory highlighter();

	PojoScopeWorkspace workspace(String tenantId);

	PojoScopeWorkspace workspace(Set<String> tenantIds);

	PojoScopeSchemaManager schemaManager();

	PojoMassIndexer massIndexer(PojoMassIndexingContext context, Set<String> tenantIds);

	<T> T extension(IndexScopeExtension<T> extension);
}
