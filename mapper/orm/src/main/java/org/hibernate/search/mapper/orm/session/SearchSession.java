/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session;

import java.util.Collection;
import java.util.Collections;

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.search.dsl.query.HibernateOrmSearchQueryResultDefinitionContext;

public interface SearchSession {

	/**
	 * Initiate the building of a search query.
	 * <p>
	 * The query will target the indexes mapped to the given type, or to any of its sub-types.
	 *
	 * @param type An indexed type, or a supertype of all indexed types that will be targeted by the search query.
	 * @param <T> An indexed type, or a supertype of all indexed types that will be targeted by the search query.
	 * @return A context allowing to define the search query,
	 * and ultimately {@link SearchQueryContext#toQuery() get the resulting query}.
	 * @see HibernateOrmSearchQueryResultDefinitionContext
	 */
	default <T> HibernateOrmSearchQueryResultDefinitionContext<T> search(Class<T> type) {
		return scope( type ).search();
	}

	/**
	 * Initiate the building of a search query.
	 * <p>
	 * The query will target the indexes mapped to the given types, or to any of their sub-types.
	 *
	 * @param types A collection of indexed types, or supertypes of all indexed types that will be targeted by the search query.
	 * @param <T> A supertype of all indexed types that will be targeted by the search query.
	 * @return A context allowing to define the search query,
	 * and ultimately {@link SearchQueryContext#toQuery() get the resulting query}.
	 * @see HibernateOrmSearchQueryResultDefinitionContext
	 */
	default <T> HibernateOrmSearchQueryResultDefinitionContext<T> search(Collection<? extends Class<? extends T>> types) {
		return scope( types ).search();
	}

	/**
	 * Create a {@link SearchScope} limited to the given type.
	 *
	 * @param type A type to include in the scope.
	 * @param <T> A type to include in the scope.
	 * @return The created scope.
	 * @see SearchScope
	 */
	default <T> org.hibernate.search.mapper.orm.search.SearchScope<T> scope(Class<T> type) {
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
	<T> org.hibernate.search.mapper.orm.search.SearchScope<T> scope(Collection<? extends Class<? extends T>> types);

	/**
	 * Creates a {@link MassIndexer} to rebuild the indexes of some or all indexed entity types.
	 * <p>
	 * The indexer will apply to the indexes mapped to the given types, or to any of their sub-types.
	 * <p>
	 * {@link MassIndexer} instances cannot be reused.
	 *
	 * @param types An array of indexed types, or supertypes of all indexed types that will be reindexed.
	 * If empty, all indexed types will be reindexed.
	 * @return The created mass indexer.
	 */
	MassIndexer createIndexer(Class<?>... types);

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
	void setAutomaticIndexingSynchronizationStrategy(AutomaticIndexingSynchronizationStrategy synchronizationStrategy);
}
