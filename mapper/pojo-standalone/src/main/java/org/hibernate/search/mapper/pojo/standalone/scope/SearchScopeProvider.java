/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.scope;

import java.util.Collection;
import java.util.Collections;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A provider of {@link SearchScope} instances.
 *
 * @see org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping
 * @see org.hibernate.search.mapper.pojo.standalone.session.SearchSession
 */
@Incubating
public interface SearchScopeProvider {

	/**
	 * Creates a {@link SearchScope} limited to
	 * indexed entity types among the given class and its subtypes.
	 *
	 * @param clazz A class that must be an indexed entity type or a supertype of such type.
	 * @param <T> A supertype of all indexed entity types to include in the scope.
	 * @return The created scope.
	 * @see SearchScope
	 */
	default <T> SearchScope<T> scope(Class<T> clazz) {
		return scope( Collections.singleton( clazz ) );
	}

	/**
	 * Creates a {@link SearchScope} limited to
	 * indexed entity types among the given classes and their subtypes.
	 *
	 * @param classes A collection of classes.
	 * Each must be an indexed entity type or a supertype of such type.
	 * @param <T> A supertype of all indexed entity types to include in the scope.
	 * @return The created scope.
	 * @see SearchScope
	 */
	<T> SearchScope<T> scope(Collection<? extends Class<? extends T>> classes);

	/**
	 * Creates a {@link SearchScope} limited to
	 * indexed entity types among the entity with the given name and its subtypes.
	 *
	 * @param expectedSuperType A supertype of all entity types to include in the scope.
	 * @param entityName An entity name. See {@link SearchEntity#name()}.
	 * The referenced entity type must be an indexed entity type or a supertype of such type.
	 * @param <T> A supertype of all indexed entity types to include in the scope.
	 * @return The created scope.
	 * @see SearchScope
	 */
	default <T> SearchScope<T> scope(Class<T> expectedSuperType, String entityName) {
		return scope( expectedSuperType, Collections.singleton( entityName ) );
	}

	/**
	 * Creates a {@link SearchScope} limited to
	 * indexed entity types among the entities with the given names and their subtypes.
	 *
	 * @param expectedSuperType A supertype of all indexed entity types to include in the scope.
	 * @param entityNames A collection of entity names. See {@link SearchEntity#name()}.
	 * Each entity type referenced in the collection must be an indexed entity type or a supertype of such type.
	 * @param <T> A supertype of all indexed entity types to include in the scope.
	 * @return The created scope.
	 * @see SearchScope
	 */
	<T> SearchScope<T> scope(Class<T> expectedSuperType, Collection<String> entityNames);

}
