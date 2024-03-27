/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.entity;

import org.hibernate.search.engine.backend.index.IndexManager;

/**
 * A descriptor of an indexed entity type,
 * exposing in particular the index manager for this entity.
 *
 * @param <E> The entity type.
 */
public interface SearchIndexedEntity<E> {

	/**
	 * @return The {@link jakarta.persistence.Entity#name() JPA name} of the entity.
	 */
	String jpaName();

	/**
	 * @return The Java class of the entity.
	 */
	Class<E> javaClass();

	/**
	 * @return The index manager this entity is indexed in.
	 */
	IndexManager indexManager();

}
