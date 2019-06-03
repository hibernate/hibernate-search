/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.scope;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortTerminalContext;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.writing.SearchWriter;
import org.hibernate.search.mapper.orm.search.dsl.query.HibernateOrmSearchQueryResultDefinitionContext;
import org.hibernate.search.mapper.pojo.search.PojoReference;

/**
 * Represents a set of types and the corresponding indexes,
 * allowing to build search-related objects (query, predicate, ...)
 * taking into account the relevant indexes and their metadata (underlying technology, field types, ...).
 *
 * @param <E> A supertype of all types in this scope.
 */
public interface SearchScope<E> {

	/**
	 * Initiate the building of a search query.
	 * <p>
	 * The query will target the indexes mapped to types in this scope, or to any of their sub-types.
	 *
	 * @return A context allowing to define the search query,
	 * and ultimately {@link SearchQueryContext#toQuery() get the resulting query}.
	 * @see HibernateOrmSearchQueryResultDefinitionContext
	 */
	HibernateOrmSearchQueryResultDefinitionContext<E> search();

	/**
	 * Initiate the building of a search predicate.
	 * <p>
	 * The predicate will only be valid for {@link #search() search queries} created using this scope
	 * or a wider scope.
	 * <p>
	 * Note this method is only necessary if you do not want to use lambda expressions,
	 * since you can {@link SearchQueryResultContext#predicate(Function) define predicates with lambdas}
	 * within the search query DSL,
	 * removing the need to create separate objects to represent the predicates.
	 *
	 * @return A context allowing to define the predicate,
	 * and ultimately {@link SearchPredicateTerminalContext#toPredicate() get the resulting predicate}.
	 * @see SearchPredicateFactoryContext
	 */
	SearchPredicateFactoryContext predicate();

	/**
	 * Initiate the building of a search sort.
	 * <p>
	 * The sort will only be valid for {@link #search() search queries} created using this scope
	 * or a wider scope.
	 * <p>
	 * Note this method is only necessary if you do not want to use lambda expressions,
	 * since you can {@link SearchQueryContext#sort(Consumer) define sorts with lambdas}
	 * within the search query DSL,
	 * removing the need to create separate objects to represent the sorts.
	 *
	 * @return A context allowing to define the sort,
	 * and ultimately {@link SearchSortTerminalContext#toSort() get the resulting sort}.
	 * @see SearchSortContainerContext
	 */
	SearchSortContainerContext sort();

	/**
	 * Initiate the building of a search projection that will be valid for the indexes in this scope.
	 * <p>
	 * The projection will only be valid for {@link #search() search queries} created using this scope
	 * or a wider scope.
	 * <p>
	 * Note this method is only necessary if you do not want to use lambda expressions,
	 * since you can {@link HibernateOrmSearchQueryResultDefinitionContext#asProjection(Function)} define projections with lambdas}
	 * within the search query DSL,
	 * removing the need to create separate objects to represent the projections.
	 *
	 * @return A context allowing to define the projection,
	 * and ultimately {@link SearchProjectionTerminalContext#toProjection() get the resulting projection}.
	 * @see SearchProjectionFactoryContext
	 */
	SearchProjectionFactoryContext<PojoReference, E> projection();

	/**
	 * Create a {@link SearchWriter} for the indexes mapped to types in this scope, or to any of their sub-types.
	 *
	 * @return A {@link SearchWriter}.
	 */
	SearchWriter writer();

	/**
	 * Create a {@link MassIndexer} for the indexes mapped to types in this scope, or to any of their sub-types.
	 * <p>
	 * {@link MassIndexer} instances cannot be reused.
	 *
	 * @return A {@link MassIndexer}.
	 */
	MassIndexer massIndexer();

}
