/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.model;

import java.util.stream.Stream;

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
	 * @return The name of this property.
	 * @deprecated Use {@link #name()} instead.
	 */
	@Deprecated
	default String getName() {
		return name();
	}

	/**
	 * @param markerType A type of marker.
	 * @param <M> The type of returned markers.
	 * @return A {@link Stream} of markers with the given type found on this property.
	 */
	<M> Stream<M> markers(Class<M> markerType);

}
