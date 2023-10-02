/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.common;

/**
 * A reference to an indexed document.
 */
public interface DocumentReference {

	/**
	 * @return The name of the type of the referenced document.
	 * The type name is mapper-specific. For example, in the Hibernate ORM mapper, it will be the JPA entity name.
	 */
	String typeName();

	/**
	 * @return The identifier of the referenced document.
	 * The identifier is returned as it was generated during document building,
	 * i.e. it does <strong>not</strong> take into account backend-specific transformations
	 * such as appending a tenant ID when using multi-tenancy.
	 */
	String id();

}
