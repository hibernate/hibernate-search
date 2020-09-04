/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.mapping;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.mapper.javabean.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.javabean.scope.SearchScope;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.javabean.session.SearchSessionBuilder;

public interface SearchMapping {

	/**
	 * Create a {@link SearchScope} limited to the given type.
	 *
	 * @param type A type to include in the scope.
	 * @return The created scope.
	 * @see SearchScope
	 */
	default SearchScope scope(Class<?> type) {
		return scope( Collections.singleton( type ) );
	}

	/**
	 * Create a {@link SearchScope} limited to the given types.
	 *
	 * @param types A collection of types to include in the scope.
	 * @return The created scope.
	 * @see SearchScope
	 */
	SearchScope scope(Collection<? extends Class<?>> types);

	/**
	 * @return A new session allowing to {@link SearchSession#indexingPlan() index} or
	 * {@link SearchSession#search(Class) search for} entities.
	 * @see #createSessionWithOptions()
	 */
	SearchSession createSession();

	/**
	 * @return A session builder allowing to more finely configure the new session.
	 * @see #createSession()
	 */
	SearchSessionBuilder createSessionWithOptions();

	/**
	 * @param entityType The type of an indexed entity.
	 * This must be the exact type; passing the type of a mapped-superclass for example will not work.
	 * @return A {@link SearchIndexedEntity} for the indexed entity with the exact given type.
	 * @param <E> The type of an indexed entity.
	 * @throws org.hibernate.search.util.common.SearchException If the type does not match any indexed entity.
	 */
	<E> SearchIndexedEntity<E> indexedEntity(Class<E> entityType);

	/**
	 * @param entityName The name of an indexed entity. See {@link SearchMappingBuilder#addEntityType(Class, String)}.
	 * @return A {@link SearchIndexedEntity} for the indexed entity with the given name.
	 * @throws org.hibernate.search.util.common.SearchException If the name does not match any indexed entity.
	 */
	SearchIndexedEntity<?> indexedEntity(String entityName);

	/**
	 * @return A collection containing one {@link SearchIndexedEntity} for each indexed entity
	 */
	Collection<? extends SearchIndexedEntity<?>> allIndexedEntities();

	static SearchMappingBuilder builder() {
		return builder( MethodHandles.publicLookup() );
	}

	static SearchMappingBuilder builder(MethodHandles.Lookup lookup) {
		return new SearchMappingBuilder( lookup );
	}

}
