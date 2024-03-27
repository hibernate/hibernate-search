/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.sort;

import org.hibernate.search.annotations.Spatial;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 * @deprecated See the deprecation note on {@link SortContext}.
 */
@Deprecated
public interface SortDistanceNoFieldContext {

	/**
	 * Order elements by distance computed from the coordinates carried by the given field.
	 * <p>The distance is computed between the value of the given field (which must be
	 * a {@link Spatial} field) and reference coordinates, to be provided in the
	 * {@link SortDistanceFieldContext next context}.
	 * @param fieldName The name of the index field carrying the spatial coordinates.
	 * @return {@code this} for method chaining
	 */
	SortDistanceFieldContext onField(String fieldName);

}
