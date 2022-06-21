/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.standalone.session;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.engine.search.query.dsl.SearchQuerySelectStep;
import org.hibernate.search.mapper.pojo.standalone.common.EntityReference;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScope;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexer;
import org.hibernate.search.mapper.pojo.standalone.work.SearchIndexingPlan;
import org.hibernate.search.mapper.pojo.standalone.massindexing.MassIndexer;
import org.hibernate.search.mapper.pojo.standalone.schema.management.SearchSchemaManager;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A Hibernate Search session, bound to a particular tenant identifier (if any),
 * and with its own overridden settings regarding commits, refresh, etc.
 * <p>
 * Provides entry points to Hibernate Search operations that involve indexing and searching.
 */
@Incubating
public interface SearchSession extends AutoCloseable {

	/**
	 * Execute any pending work in the {@link #indexingPlan() indexing plan}
	 * and release any resource held by this session.
	 */
	@Override
	void close();

	/**
	 * Determine whether the search session is open.
	 * @return true until the search session has been closed
	 */
	boolean isOpen();

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
	 * @return A {@link MassIndexer}.
	 */
	MassIndexer massIndexer(Collection<? extends Class<?>> types);

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
	default <T> SearchQuerySelectStep<?, EntityReference, T, ?, ?, ?> search(Class<T> type) {
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
	<T> SearchQuerySelectStep<?, EntityReference, T, ?, ?, ?> search(Collection<? extends Class<? extends T>> types);

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
	<T> SearchQuerySelectStep<?, EntityReference, T, ?, ?, ?> search(SearchScope<T> scope);

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
	default SearchSchemaManager schemaManager(Class<?>... types) {
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
	 * @return The indexing plan for this session. It will be executed upon closing this session.
	 */
	SearchIndexingPlan indexingPlan();

	/**
	 * @return The indexer for this session.
	 */
	SearchIndexer indexer();

	/**
	 * @return The tenant identifier for this session.
	 */
	String tenantIdentifier();

}
