/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping;

import java.util.Collection;
import java.util.Collections;
import javax.persistence.Entity;
import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.mapper.orm.scope.SearchScope;

/**
 * The Hibernate Search mapping between the Hibernate ORM model and the backend(s).
 * <p>
 * Provides entry points to Hibernate Search operations that are not tied to a specific ORM session.
 */
public interface SearchMapping {

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
	 * @return The underlying {@link EntityManagerFactory} used by this {@link SearchMapping}.
	 */
	EntityManagerFactory toEntityManagerFactory();

	/**
	 * @return The underlying {@link SessionFactory} used by this {@link SearchMapping}.
	 */
	SessionFactory toOrmSessionFactory();

	/**
	 * @param indexName The key to get the required {@link IndexManager} instance.
	 * @return The index manager for the index having {@code indexName} as name.
	 */
	IndexManager getIndexManager(String indexName);

	/**
	 * @param backendName The key to get the required {@link Backend} instance.
	 * @return The backend having {@code backendName} as name.
	 */
	Backend getBackend(String backendName);
}
