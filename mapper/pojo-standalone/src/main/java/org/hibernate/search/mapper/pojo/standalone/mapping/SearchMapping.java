/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.mapping;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotatedTypeSource;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.scope.SearchScopeProvider;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSessionBuilder;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.common.reflect.spi.ValueHandleFactory;

/**
 * The Hibernate Search mapping between the POJO model and the backend(s).
 * <p>
 * Provides entry points to Hibernate Search operations that are not tied to a specific {@link SearchSession session}.
 */
@Incubating
public interface SearchMapping extends SearchScopeProvider {

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
	 * @param entityName The name of an indexed entity. See {@link SearchEntity#name()}.
	 * @return A {@link SearchIndexedEntity} for the indexed entity with the given name.
	 * @throws org.hibernate.search.util.common.SearchException If the name does not match any indexed entity.
	 */
	SearchIndexedEntity<?> indexedEntity(String entityName);

	/**
	 * @return A collection containing one {@link SearchIndexedEntity} for each indexed entity
	 */
	Collection<? extends SearchIndexedEntity<?>> allIndexedEntities();

	/**
	 * @param indexName The name of an index. See {@link Indexed#index()}.
	 * @return The index manager for the index having {@code indexName} as name.
	 */
	IndexManager indexManager(String indexName);

	/**
	 * @return The default backend, if any.
	 */
	Backend backend();

	/**
	 * @param backendName The name of a backend. See {@link Indexed#backend()}.
	 * @return The backend having {@code backendName} as name.
	 */
	Backend backend(String backendName);

	/**
	 * @param annotatedTypeSource A source of types to be processed for annotations by Hibernate Search.
	 * @return A {@link SearchMapping} builder.
	 * @see AnnotatedTypeSource
	 */
	static SearchMappingBuilder builder(AnnotatedTypeSource annotatedTypeSource) {
		return new SearchMappingBuilder( annotatedTypeSource );
	}

	/**
	 * @param annotatedTypeSource A source of types to be processed for annotations by Hibernate Search.
	 * @param lookup A {@link MethodHandles.Lookup} to perform reflection on mapped types.
	 * @return A {@link SearchMapping} builder.
	 * @see AnnotatedTypeSource
	 */
	static SearchMappingBuilder builder(AnnotatedTypeSource annotatedTypeSource, MethodHandles.Lookup lookup) {
		return builder( annotatedTypeSource )
				.valueReadHandleFactory( ValueHandleFactory.usingMethodHandle( lookup ) );
	}

}
