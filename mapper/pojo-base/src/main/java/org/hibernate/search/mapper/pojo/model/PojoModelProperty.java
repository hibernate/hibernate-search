/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.model;

import java.util.Collection;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A model element representing a property bound to a bridge.
 *
 * @see org.hibernate.search.mapper.pojo.bridge.PropertyBridge
 */
@Incubating
public interface PojoModelProperty extends PojoModelCompositeElement {

	/**
	 * @return The name of this property.
	 */
	String name();

	/**
	 * @param markerType A type of marker.
	 * @param <M> The type of returned markers.
	 * @return A collection of markers with the given type found on this property.
	 */
	<M> Collection<M> markers(Class<M> markerType);

}
