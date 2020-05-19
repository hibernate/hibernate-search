/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.common;

/**
 * A reference to an indexed entity.
 */
public interface EntityReference {

	/**
	 * @return The type of the referenced entity.
	 */
	Class<?> type();

	/**
	 * @return The type of the referenced entity.
	 * @deprecated Use {@link #type()} instead.
	 */
	@Deprecated
	default Class<?> getType() {
		return type();
	}

	/**
	 * @return The name of the referenced entity in the Hibernate ORM mapping.
	 * @see javax.persistence.Entity#name()
	 */
	String name();

	/**
	 * @return The name of the referenced entity in the Hibernate ORM mapping.
	 * @see javax.persistence.Entity#name()
	 * @deprecated Use {@link #name()} instead.
	 */
	@Deprecated
	default String getName() {
		return name();
	}

	/**
	 * @return The identifier of the referenced entity,
	 * Generally this is the entity ID,
	 * but a different value may be returned if another property than the entity ID is defined as {@code @DocumentId}.
	 */
	Object id();

	/**
	 * @return The identifier of the referenced entity,
	 * Generally this is the entity ID,
	 * but a different value may be returned if another property than the entity ID is defined as {@code @DocumentId}.
	 * @deprecated Use {@link #id()} instead.
	 */
	@Deprecated
	default Object getId() {
		return id();
	}

}
