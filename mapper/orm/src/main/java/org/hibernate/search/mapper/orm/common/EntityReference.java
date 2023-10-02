/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.common;

/**
 * The (legacy) EntityReference interface specific to the Hibernate ORM mapper.
 *
 * @deprecated Use {@link org.hibernate.search.engine.common.EntityReference} instead.
 */
@Deprecated
public interface EntityReference extends org.hibernate.search.engine.common.EntityReference {

	/**
	 * @return The name of the referenced entity in the Hibernate ORM mapping.
	 * @see jakarta.persistence.Entity#name()
	 */
	@Override
	String name();

}
