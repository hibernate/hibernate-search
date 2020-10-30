/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.persistence.Entity;
import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.mapper.orm.automaticindexing.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.schema.management.SearchSchemaManager;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.search.loading.dsl.SearchLoadingOptionsStep;
import org.hibernate.search.mapper.orm.work.SearchIndexingPlan;
import org.hibernate.search.mapper.orm.work.SearchWorkspace;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;

public interface SearchSession {

	/**
	 * Initiate the building of a search query.
	 * <p>
	 * The query will target the indexes mapped to the given type, or to any of its sub-types.
	 *
	 * @param type An indexed type, or a supertype of all indexed types that will be targeted by the search query.
	 * @param <T> An indexed type, or a supertype of all indexed types that will be targeted by the search query.
	 * @return The initial step of a DSL where the search query can be defined.
	 * @see SearchQuerySelectStep
	 */
	default <T> SearchQuerySelectStep<
			SearchQueryOptionsStep<?, T, SearchLoadingOptionsStep, ?, ?>,
			EntityReference,
			T,
			SearchLoadingOptionsStep,
			SearchProjectionFactory<EntityReference, T>,
			SearchPredicateFactory
			> search(
			Class<T> type) {
		return search( Collections.singleton( type ) );
	}

	/**
	 * Initiate the building of a search query.
	 * <p>
	 * The query will target the indexes mapped to the given types, or to any of their sub-types.
	 *
	 * @param types A collection of indexed types, or supertypes of all indexed types that will be targeted by the search query.
	 * @param <T> A supertype of all indexed types that will be targeted by the search query.
	 * @return The initial step of a DSL where the search query can be defined.
	 * @see SearchQuerySelectStep
	 */
	<T> SearchQuerySelectStep<
			SearchQueryOptionsStep<?, T, SearchLoadingOptionsStep, ?, ?>,
			EntityReference,
			T,
			SearchLoadingOptionsStep,
			SearchProjectionFactory<EntityReference, T>,
			SearchPredicateFactory
			> search(
			Collection<? extends Class<? extends T>> types);

	/**
	 * Initiate the building of a search query.
	 * <p>
	 * The query will target the indexes in the given scope.
	 *
	 * @param scope A scope representing all indexed types that will be targeted by the search query.
	 * @param <T> A supertype of all types in the given scope.
	 * @return The initial step of a DSL where the search query can be defined.
	 * @see SearchQuerySelectStep
	 */
	<T> SearchQuerySelectStep<
			SearchQueryOptionsStep<?, T, SearchLoadingOptionsStep, ?, ?>,
			EntityReference,
			T,
			SearchLoadingOptionsStep,
			SearchProjectionFactory<EntityReference, T>,
			SearchPredicateFactory
			> search(
			SearchScope<T> scope);

	/**
	 * Create a {@link SearchSchemaManager} for all indexes.
	 *
	 * @return A {@link SearchSchemaManager}.
	 */
	default SearchSchemaManager schemaManager() {
		return schemaManager( Collections.singleton( Object.class ) );
	}

	/**
	 * Create a {@link SearchSchemaManager} for the indexes mapped to the given type, or to any of its sub-types.
	 *
	 * @param types One or more indexed types, or supertypes of all indexed types that will be targeted by the schema manager.
	 * @return A {@link SearchSchemaManager}.
	 */
	default SearchSchemaManager schemaManager(Class<?> ... types) {
		return schemaManager( Arrays.asList( types ) );
	}

	/**
	 * Create a {@link SearchSchemaManager} for the indexes mapped to the given types, or to any of their sub-types.
	 *
	 * @param types A collection of indexed types, or supertypes of all indexed types that will be targeted by the schema manager.
	 * @return A {@link SearchSchemaManager}.
	 */
	SearchSchemaManager schemaManager(Collection<? extends Class<?>> types);

	/**
	 * Create a {@link SearchWorkspace} for the indexes mapped to all indexed types.
	 *
	 * @return A {@link SearchWorkspace}.
	 */
	default SearchWorkspace workspace() {
		return workspace( Collections.singleton( Object.class ) );
	}

	/**
	 * Create a {@link SearchWorkspace} for the indexes mapped to the given type, or to any of its sub-types.
	 *
	 * @param types One or more indexed types, or supertypes of all indexed types that will be targeted by the workspace.
	 * @return A {@link SearchWorkspace}.
	 */
	default SearchWorkspace workspace(Class<?> ... types) {
		return workspace( Arrays.asList( types ) );
	}

	/**
	 * Create a {@link SearchWorkspace} for the indexes mapped to the given types, or to any of their sub-types.
	 *
	 * @param types A collection of indexed types, or supertypes of all indexed types that will be targeted by the workspace.
	 * @return A {@link SearchWorkspace}.
	 */
	SearchWorkspace workspace(Collection<? extends Class<?>> types);

	/**
	 * Creates a {@link MassIndexer} to rebuild the indexes of all indexed entity types.
	 * <p>
	 * {@link MassIndexer} instances cannot be reused.
	 *
	 * @return The created mass indexer.
	 */
	default MassIndexer massIndexer() {
		return massIndexer( Object.class );
	}

	/**
	 * Creates a {@link MassIndexer} to rebuild the indexes mapped to the given types, or to any of their sub-types.
	 * <p>
	 * {@link MassIndexer} instances cannot be reused.
	 *
	 * @param types An array of indexed types, or supertypes of all indexed types that will be targeted by the workspace.
	 * @return The created mass indexer.
	 */
	default MassIndexer massIndexer(Class<?>... types) {
		return massIndexer( Arrays.asList( types ) );
	}

	/**
	 * Creates a {@link MassIndexer} to rebuild the indexes mapped to the given types, or to any of their sub-types.
	 *
	 * @param types A collection of indexed types, or supertypes of all indexed types that will be targeted by the workspace.
	 * @return A {@link SearchWorkspace}.
	 */
	MassIndexer massIndexer(Collection<? extends Class<?>> types);

	/**
	 * Create a {@link SearchScope} limited to the given type.
	 *
	 * @param type A type to include in the scope.
	 * @param <T> A type to include in the scope.
	 * @return The created scope.
	 * @see SearchScope
	 */
	default <T> SearchScope<T> scope(Class<T> type) {
		return scope( Collections.singleton( type ) );
	}

	/**
	 * Create a {@link SearchScope} limited to the given types.
	 *
	 * @param types A collection of types to include in the scope.
	 * @param <T> A supertype of all types to include in the scope.
	 * @return The created scope.
	 * @see SearchScope
	 */
	<T> SearchScope<T> scope(Collection<? extends Class<? extends T>> types);

	/**
	 * Create a {@link SearchScope} limited to entity types referenced by their name.
	 *
	 * @param expectedSuperType A supertype of all entity types to include in the scope.
	 * @param entityName An entity name. See {@link Entity#name()}.
	 * @param <T> A supertype of all entity types to include in the scope.
	 * @return The created scope.
	 * @see SearchScope
	 */
	default <T> SearchScope<T> scope(Class<T> expectedSuperType, String entityName) {
		return scope( expectedSuperType, Collections.singleton( entityName ) );
	}

	/**
	 * Create a {@link SearchScope} limited to entity types referenced by their name.
	 *
	 * @param expectedSuperType A supertype of all entity types to include in the scope.
	 * @param entityNames A collection of entity names. See {@link Entity#name()}.
	 * @param <T> A supertype of all entity types to include in the scope.
	 * @return The created scope.
	 * @see SearchScope
	 */
	<T> SearchScope<T> scope(Class<T> expectedSuperType, Collection<String> entityNames);

	/**
	 * @return The indexing plan for this session, allowing to explicitly index entities or delete them from the index,
	 * or to process entity changes or even write to the indexes before the transaction is committed.
	 */
	SearchIndexingPlan indexingPlan();

	/**
	 * @return The underlying {@link EntityManager} used by this {@link SearchSession}.
	 */
	EntityManager toEntityManager();

	/**
	 * @return The underlying {@link Session} used by this {@link SearchSession}.
	 */
	Session toOrmSession();

	/**
	 * Set the {@link AutomaticIndexingSynchronizationStrategy} to use for this session.
	 * <p>
	 * Behavior is undefined if called while entity changes are pending:
	 * be sure to call this only just after creating a session,
	 * or just after committing a transaction.
	 *
	 * @param synchronizationStrategy The synchronization strategy to use
	 * @see AutomaticIndexingSynchronizationStrategy
	 */
	void automaticIndexingSynchronizationStrategy(AutomaticIndexingSynchronizationStrategy synchronizationStrategy);

}
