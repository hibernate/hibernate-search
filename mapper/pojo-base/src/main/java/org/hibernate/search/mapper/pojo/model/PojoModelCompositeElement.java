/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model;

import java.util.Collection;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A potentially composite element in the POJO model.
 * <p>
 * Offers ways to create {@link PojoElementAccessor accessors} allowing
 * to retrieve data from objects passed to bridges.
 *
 * @see PojoModelType
 * @see PojoModelProperty
 */
@Incubating
public interface PojoModelCompositeElement extends PojoModelElement {

	/**
	 * @param type The expected type of values returned by the accessor.
	 * @param <T> The expected type of values returned by the accessor.
	 * @return An accessor able to retrieve this element from an object, provided it has the right type.
	 * @throws org.hibernate.search.util.common.SearchException If this element
	 * is not {@link #isAssignableTo(Class) assignable} to the given type.
	 */
	<T> PojoElementAccessor<T> createAccessor(Class<T> type);

	/**
	 * @return An accessor able to retrieve this element from an object, provided it has the right type.
	 */
	PojoElementAccessor<?> createAccessor();

	/**
	 * @param name The name of a property.
	 * @return A element representing the given property on the current element.
	 */
	PojoModelProperty property(String name);

	/**
	 * @return A collection of all properties of the current element.
	 */
	Collection<? extends PojoModelProperty> properties();

}
