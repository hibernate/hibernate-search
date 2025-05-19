/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.scope;

import java.util.function.Function;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.TypedSearchAggregationFactory;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.TypedSearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.engine.search.sort.dsl.TypedSearchSortFactory;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * Represents a set of types and the corresponding indexes.
 * <p>
 * The scope can be used for search, to build search-related objects (predicate, sort, projection, aggregation, ...),
 * or to define the targeted entities/indexes
 * when {@link SearchSession#search(TypedSearchScope) passing it to the session}.
 * <p>
 * It can also be used to start large-scale operations, e.g. using a {@link #schemaManager()}
 * or a {@link #massIndexer()}.
 *
 * @param <E> A supertype of all types in this scope.
 */
@Incubating
public interface TypedSearchScope<SR, E> extends SearchScope<E> {

	/**
	 * Initiate the building of a search predicate.
	 * <p>
	 * The predicate will only be valid for {@link SearchSession#search(TypedSearchScope) search queries}
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
	 * The sort will only be valid for {@link SearchSession#search(TypedSearchScope) search queries}
	 * created using this scope or another scope instance targeting the same indexes.
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
	 * The projection will only be valid for {@link SearchSession#search(TypedSearchScope) search queries}
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
	TypedSearchProjectionFactory<SR, EntityReference, E> projection();

	/**
	 * Initiate the building of a search aggregation that will be valid for the indexes in this scope.
	 * <p>
	 * The aggregation will only be usable in {@link SearchSession#search(TypedSearchScope) search queries}
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

}
