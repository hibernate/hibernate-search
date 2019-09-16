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

import javax.persistence.EntityManager;

import org.hibernate.Session;
import org.hibernate.search.mapper.orm.writing.SearchWriter;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.scope.SearchScope;
import org.hibernate.search.mapper.orm.search.query.dsl.HibernateOrmSearchQueryHitTypeStep;

public interface SearchSession {

	/**
	 * Initiate the building of a search query.
	 * <p>
	 * The query will target the indexes mapped to the given type, or to any of its sub-types.
	 *
	 * @param type An indexed type, or a supertype of all indexed types that will be targeted by the search query.
	 * @param <T> An indexed type, or a supertype of all indexed types that will be targeted by the search query.
	 * @return The initial step of a DSL where the search query can be defined.
	 * @see HibernateOrmSearchQueryHitTypeStep
	 */
	default <T> HibernateOrmSearchQueryHitTypeStep<T> search(Class<T> type) {
		return scope( type ).search();
	}

	/**
	 * Initiate the building of a search query.
	 * <p>
	 * The query will target the indexes mapped to the given types, or to any of their sub-types.
	 *
	 * @param types A collection of indexed types, or supertypes of all indexed types that will be targeted by the search query.
	 * @param <T> A supertype of all indexed types that will be targeted by the search query.
	 * @return The initial step of a DSL where the search query can be defined.
	 * @see HibernateOrmSearchQueryHitTypeStep
	 */
	default <T> HibernateOrmSearchQueryHitTypeStep<T> search(Collection<? extends Class<? extends T>> types) {
		return scope( types ).search();
	}

	/**
	 * Create a {@link SearchWriter} for the indexes mapped to all indexed types.
	 *
	 * @return A {@link SearchWriter}.
	 */
	default SearchWriter writer() {
		return writer( Collections.singleton( Object.class ) );
	}

	/**
	 * Create a {@link SearchWriter} for the indexes mapped to the given type, or to any of its sub-types.
	 *
	 * @param types One or more indexed types, or supertypes of all indexed types that will be targeted by the writer.
	 * @return A {@link SearchWriter}.
	 */
	default SearchWriter writer(Class<?> ... types) {
		return writer( Arrays.asList( types ) );
	}

	/**
	 * Create a {@link SearchWriter} for the indexes mapped to the given types, or to any of their sub-types.
	 *
	 * @param types A collection of indexed types, or supertypes of all indexed types that will be targeted by the writer.
	 * @return A {@link SearchWriter}.
	 */
	default SearchWriter writer(Collection<? extends Class<?>> types) {
		return scope( types ).writer();
	}

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
	 * @param types An array of indexed types, or supertypes of all indexed types that will be targeted by the writer.
	 * @return The created mass indexer.
	 */
	default MassIndexer massIndexer(Class<?>... types) {
		return massIndexer( Arrays.asList( types ) );
	}

	/**
	 * Creates a {@link MassIndexer} to rebuild the indexes mapped to the given types, or to any of their sub-types.
	 *
	 * @param types A collection of indexed types, or supertypes of all indexed types that will be targeted by the writer.
	 * @return A {@link SearchWriter}.
	 */
	default MassIndexer massIndexer(Collection<? extends Class<?>> types) {
		return scope( types ).massIndexer();
	}

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
	 * Creates a {@link MassIndexer} to rebuild the indexes of some or all indexed entity types.
	 * <p>
	 * The indexer will apply to the indexes mapped to the given types, or to any of their sub-types.
	 * <p>
	 * {@link MassIndexer} instances cannot be reused.
	 *
	 * @param types An array of indexed types, or supertypes of all indexed types that will be reindexed.
	 * If empty, all indexed types will be reindexed.
	 * @return The created mass indexer.
	 * @deprecated Use {@link #massIndexer()} or {@link #massIndexer(Class[])} instead.
	 */
	@Deprecated
	default MassIndexer createIndexer(Class<?>... types) {
		return types.length == 0 ? massIndexer() : massIndexer( types );
	}

	/**
	 * @return The write plan for this session, allowing to explicitly index or delete entities,
	 * or to process entity changes or even write to the indexes before the transaction is committed.
	 */
	SearchSessionWritePlan writePlan();

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
