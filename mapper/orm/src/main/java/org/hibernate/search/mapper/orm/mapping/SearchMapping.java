/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.mapping;

import java.util.Collection;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.mapper.orm.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.orm.scope.SearchScopeProvider;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.work.SearchIndexingPlanFilter;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The Hibernate Search mapping between the Hibernate ORM model and the backend(s).
 * <p>
 * Provides entry points to Hibernate Search operations that are not tied to a specific ORM session.
 */
public interface SearchMapping extends SearchScopeProvider {

	/**
	 * @return The underlying {@link EntityManagerFactory} used by this {@link SearchMapping}.
	 */
	EntityManagerFactory toEntityManagerFactory();

	/**
	 * @return The underlying {@link SessionFactory} used by this {@link SearchMapping}.
	 */
	SessionFactory toOrmSessionFactory();

	/**
	 * @param entityType The type of an indexed entity.
	 * This must be the exact type; passing the type of a mapped-superclass for example will not work.
	 * @return A {@link SearchIndexedEntity} for the indexed entity with the exact given type.
	 * @param <E> The type of an indexed entity.
	 * @throws org.hibernate.search.util.common.SearchException If the type does not match any indexed entity.
	 */
	<E> SearchIndexedEntity<E> indexedEntity(Class<E> entityType);

	/**
	 * @param entityName The name of an indexed entity. See {@link Entity#name()}.
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
	 * Set a filter defining which types must be included/excluded when indexed within indexing plans (either automatically or manually).
	 * <p>
	 * This does not affect indexing that does not rely on indexing plans, like the mass indexer.
	 * <p>
	 * By default, all indexed and contained types are included.
	 *
	 * @param filter The filter that includes/excludes types when indexed.
	 */
	@Incubating
	void indexingPlanFilter(SearchIndexingPlanFilter filter);

	/**
	 * Extend the current search mapping with the given extension,
	 * resulting in an extended search mapping offering mapper-specific utilities.
	 *
	 * @param extension The extension to apply.
	 * @param <T> The type of search mapping provided by the extension.
	 * @return The extended search mapping.
	 * @throws SearchException If the extension cannot be applied (wrong underlying technology, ...).
	 */
	default <T> T extension(SearchMappingExtension<T> extension) {
		return extension.extendOrFail( this );
	}

}
