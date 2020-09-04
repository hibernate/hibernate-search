/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.entity;

import org.hibernate.search.engine.backend.index.IndexManager;

/**
 * A descriptor of an indexed entity type,
 * exposing in particular the index manager for this entity.
 *
 * @param <E> The entity type.
 */
@SuppressWarnings("deprecation")
public interface SearchIndexedEntity<E>
		extends org.hibernate.search.mapper.orm.mapping.SearchIndexedEntity {

	/**
	 * @return The {@link javax.persistence.Entity#name() JPA name} of the entity.
	 */
	@Override
	String jpaName();

	/**
	 * @return The Java class of the entity.
	 */
	@Override
	Class<E> javaClass();

	/**
	 * @return The index manager this entity is indexed in.
	 */
	@Override
	IndexManager indexManager();

}
