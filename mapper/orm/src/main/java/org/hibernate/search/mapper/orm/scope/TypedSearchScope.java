/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.scope;

import java.util.function.Function;

import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.TypedSearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.TypedSearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.engine.search.sort.dsl.TypedSearchSortFactory;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.util.common.SearchException;

/**
 * Represents a set of types and the corresponding indexes.
 * <p>
 * The scope can be used for search, to build search-related objects (predicate, sort, projection, aggregation, ...),
 * or to define the targeted entities/indexes
 * when {@link org.hibernate.search.mapper.orm.session.SearchSession#search(TypedSearchScope) passing it to the session}.
 * <p>
 * It can also be used to start large-scale operations, e.g. using a {@link #schemaManager()}, a {@link #workspace()}
 * or a {@link #massIndexer()}.
 *
 * @param <E> A supertype of all types in this scope.
 */
@SuppressWarnings("deprecation")
public interface TypedSearchScope<SR, E> extends SearchScope<E> {

	/**
	 * Initiate the building of a search predicate.
	 * <p>
	 * The predicate will only be valid for {@link org.hibernate.search.mapper.orm.session.SearchSession#search(TypedSearchScope) search queries}
	 * created using this scope or another scope instance targeting the same indexes.
	 * <p>
	 * Note this method is only necessary if you do not want to use lambda expressions,
	 * since you can {@link SearchQueryWhereStep#where(Function) define predicates with lambdas}
	 * within the search query DSL,
	 * removing the need to create separate objects to represent the predicates.
	 *
	 * @return A predicate factory.
	 * @see TypedSearchPredicateFactory
	 */
	@Override
	TypedSearchPredicateFactory<SR> predicate();

	/**
	 * Initiate the building of a search sort.
	 * <p>
	 * The sort will only be valid for {@link org.hibernate.search.mapper.orm.session.SearchSession#search(TypedSearchScope) search queries}
	 * created using this scope or another scope instance targeting the same indexes.
	 * or a wider scope.
	 * <p>
	 * Note this method is only necessary if you do not want to use lambda expressions,
	 * since you can {@link SearchQueryOptionsStep#sort(Function) define sorts with lambdas}
	 * within the search query DSL,
	 * removing the need to create separate objects to represent the sorts.
	 *
	 * @return A sort factory.
	 * @see TypedSearchSortFactory
	 */
	@Override
	TypedSearchSortFactory<SR> sort();

	/**
	 * Initiate the building of a search projection that will be valid for the indexes in this scope.
	 * <p>
	 * The projection will only be valid for {@link org.hibernate.search.mapper.orm.session.SearchSession#search(TypedSearchScope) search queries}
	 * created using this scope or another scope instance targeting the same indexes.
	 * <p>
	 * Note this method is only necessary if you do not want to use lambda expressions,
	 * since you can {@link SearchQuerySelectStep#select(Function)} define projections with lambdas}
	 * within the search query DSL,
	 * removing the need to create separate objects to represent the projections.
	 *
	 * @return A projection factory.
	 * @see TypedSearchProjectionFactory
	 */
	@Override
	@SuppressWarnings("deprecation")
	TypedSearchProjectionFactory<SR, EntityReference, E> projection();

	/**
	 * Initiate the building of a search aggregation that will be valid for the indexes in this scope.
	 * <p>
	 * The aggregation will only be usable in {@link org.hibernate.search.mapper.orm.session.SearchSession#search(TypedSearchScope) search queries}
	 * created using this scope or another scope instance targeting the same indexes.
	 * <p>
	 * Note this method is only necessary if you do not want to use lambda expressions,
	 * since you can {@link SearchQueryOptionsStep#aggregation(AggregationKey, SearchAggregation)} define aggregations with lambdas}
	 * within the search query DSL,
	 * removing the need to create separate objects to represent the aggregation.
	 *
	 * @return An aggregation factory.
	 * @see TypedSearchAggregationFactory
	 */
	@Override
	TypedSearchAggregationFactory<SR> aggregation();

	/**
	 * Extend the current search scope with the given extension,
	 * resulting in an extended search scope offering backend-specific utilities.
	 *
	 * @param extension The extension to apply.
	 * @param <T> The type of search scope provided by the extension.
	 * @return The extended search scope.
	 * @throws SearchException If the extension cannot be applied (wrong underlying technology, ...).
	 */
	<T> T extension(IndexScopeExtension<T> extension);

}
