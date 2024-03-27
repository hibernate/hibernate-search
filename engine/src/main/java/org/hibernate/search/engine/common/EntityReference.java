/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.common;

/**
 * A reference to an indexed or contained entity.
 */
public interface EntityReference {

	/**
	 * @return The type of the referenced entity.
	 */
	Class<?> type();

	/**
	 * @return The name of the referenced entity.
	 */
	String name();

	/**
	 * @return The identifier of the referenced entity for Hibernate Search.
	 * This is the value of the property used to generate the document ID,
	 * which is generally also the entity ID (though depending on the mapping this may be another unique property).
	 */
	Object id();

}
