/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * An accessor allowing the retrieval of an element, for example a property, from a POJO.
 * <p>
 * Accessors are created by {@link PojoModelCompositeElement} instances.
 */
@Incubating
public interface PojoElementAccessor<T> {

	/**
	 * Reads the element from the given parent element.
	 * @param parentElement An object compatible with this accessor, i.e. with the right type.
	 * @return The element pointed to by this accessor, extracted from the given parent element.
	 */
	T read(Object parentElement);

}
