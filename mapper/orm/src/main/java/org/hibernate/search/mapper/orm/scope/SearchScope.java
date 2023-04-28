/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.scope;

import java.util.Set;
import java.util.function.Function;

import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;
import org.hibernate.search.engine.search.highlighter.dsl.SearchHighlighterFactory;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.engine.search.sort.dsl.SearchSortFactory;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.util.common.SearchException;

/**
 * Represents a set of types and the corresponding indexes.
 * <p>
 * The scope can be used for search, to build search-related objects (predicate, sort, projection, aggregation, ...),
 * or to define the targeted entities/indexes
 * when {@link org.hibernate.search.mapper.orm.session.SearchSession#search(SearchScope) passing it to the session}.
 * <p>
 * It can also be used to start large-scale operations, e.g. using a {@link #schemaManager()}, a {@link #workspace()}
 * or a {@link #massIndexer()}.
 *
 * @param <E> A supertype of all types in this scope.
 */
public interface SearchScope<E> {

	/**
	 * Initiate the building of a search predicate.
	 * <p>
	 * The predicate will only be valid for {@link org.hibernate.search.mapper.orm.session.SearchSession#search(SearchScope) search queries}
	 * created using this scope or another scope instance targeting the same indexes.
	 * <p>
	 * Note this method is only necessary if you do not want to use lambda expressions,
	 * since you can {@link SearchQueryWhereStep#where(Function) define predicates with lambdas}
	 * within the search query DSL,
	 * removing the need to create separate objects to represent the predicates.
	 *
	 * @return A predicate factory.
	 * @see SearchPredicateFactory
	 */
	SearchPredicateFactory predicate();

	/**
	 * Initiate the building of a search sort.
	 * <p>
	 * The sort will only be valid for {@link org.hibernate.search.mapper.orm.session.SearchSession#search(SearchScope) search queries}
	 * created using this scope or another scope instance targeting the same indexes.
	 * or a wider scope.
	 * <p>
	 * Note this method is only necessary if you do not want to use lambda expressions,
	 * since you can {@link SearchQueryOptionsStep#sort(Function) define sorts with lambdas}
	 * within the search query DSL,
	 * removing the need to create separate objects to represent the sorts.
	 *
	 * @return A sort factory.
	 * @see SearchSortFactory
	 */
	SearchSortFactory sort();

	/**
	 * Initiate the building of a search projection that will be valid for the indexes in this scope.
	 * <p>
	 * The projection will only be valid for {@link org.hibernate.search.mapper.orm.session.SearchSession#search(SearchScope) search queries}
	 * created using this scope or another scope instance targeting the same indexes.
	 * <p>
	 * Note this method is only necessary if you do not want to use lambda expressions,
	 * since you can {@link SearchQuerySelectStep#select(Function)} define projections with lambdas}
	 * within the search query DSL,
	 * removing the need to create separate objects to represent the projections.
	 *
	 * @return A projection factory.
	 * @see SearchProjectionFactory
	 */
	SearchProjectionFactory<EntityReference, E> projection();

	/**
	 * Initiate the building of a search aggregation that will be valid for the indexes in this scope.
	 * <p>
	 * The aggregation will only be usable in {@link org.hibernate.search.mapper.orm.session.SearchSession#search(SearchScope) search queries}
	 * created using this scope or another scope instance targeting the same indexes.
	 * <p>
	 * Note this method is only necessary if you do not want to use lambda expressions,
	 * since you can {@link SearchQueryOptionsStep#aggregation(AggregationKey, SearchAggregation)} define aggregations with lambdas}
	 * within the search query DSL,
	 * removing the need to create separate objects to represent the aggregation.
	 *
	 * @return An aggregation factory.
	 * @see SearchAggregationFactory
	 */
	SearchAggregationFactory aggregation();

	/**
	 * Create a {@link SearchSchemaManager} for the indexes mapped to types in this scope, or to any of their sub-types.
	 *
	 * @return A {@link SearchSchemaManager}.
	 */
	SearchSchemaManager schemaManager();

	/**
	 * Initiate the building of a highlighter that will be valid for the indexes in this scope.
	 * <p>
	 * The highlighter will only be valid for {@link org.hibernate.search.mapper.orm.session.SearchSession#search(SearchScope) search queries}
	 * created using this scope or another scope instance targeting the same indexes.
	 * <p>
	 * Note this method is only necessary if you do not want to use lambda expressions,
	 * since you can {@link SearchQueryOptionsStep#highlighter(Function) define highlighters with lambdas}
	 * within the search query DSL,
	 * removing the need to create separate objects to represent the projections.
	 *
	 * @return A highlighter factory.
	 */
	SearchHighlighterFactory highlighter();

	/**
	 * Create a {@link SearchWorkspace} for the indexes mapped to types in this scope, or to any of their sub-types.
	 * <p>
	 * This method only works for single-tenant applications.
	 * If multi-tenancy is enabled, use {@link #workspace(String)} instead.
	 *
	 * @return A {@link SearchWorkspace}.
	 */
	SearchWorkspace workspace();

	/**
	 * Create a {@link SearchWorkspace} for the indexes mapped to types in this scope, or to any of their sub-types.
	 * <p>
	 * This method only works for multi-tenant applications.
	 * If multi-tenancy is disabled, use {@link #workspace()} instead.
	 *
	 * @param tenantId The identifier of the tenant whose index content should be targeted.
	 * @return A {@link SearchWorkspace}.
	 */
	SearchWorkspace workspace(String tenantId);

	/**
	 * Create a {@link MassIndexer} for the indexes mapped to types in this scope, or to any of their sub-types.
	 * <p>
	 * This method primarily is intended for single-tenant applications.
	 * If multi-tenancy is enabled, consider using {@link #massIndexer(String)}/{@link #massIndexer(Set)} instead.
	 * In case this method is used in multi-tenant environment - {@link MassIndexer} for all configured tenants will be created.
	 * <p>
	 * {@link MassIndexer} instances cannot be reused.
	 *
	 * @return A {@link MassIndexer}.
	 */
	MassIndexer massIndexer();

	/**
	 * Create a {@link MassIndexer} for the indexes mapped to types in this scope, or to any of their sub-types.
	 * <p>
	 * This method only works for multi-tenant applications.
	 * If multi-tenancy is disabled, use {@link #massIndexer()} instead.
	 * <p>
	 * {@link MassIndexer} instances cannot be reused.
	 *
	 * @param tenantId The identifier of the tenant whose index content should be targeted.
	 * @return A {@link MassIndexer}.
	 */
	MassIndexer massIndexer(String tenantId);

	/**
	 * Create a {@link MassIndexer} for the indexes mapped to types in this scope, or to any of their sub-types.
	 * <p>
	 * This method works for both single- and multi-tenant applications.
	 * If multi-tenancy is disabled, simply keep the set of tenants empty.
	 * <p>
	 * {@link MassIndexer} instances cannot be reused.
	 *
	 * @param tenantIds The tenants identifiers whose index content should be targeted. If empty, all tenants will be targeted.
	 * @return A {@link MassIndexer}.
	 */
	MassIndexer massIndexer(Set<String> tenantIds);

	/**
	 * @return A set containing one {@link SearchIndexedEntity} for each indexed entity in this scope.
	 */
	Set<? extends SearchIndexedEntity<? extends E>> includedTypes();

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
