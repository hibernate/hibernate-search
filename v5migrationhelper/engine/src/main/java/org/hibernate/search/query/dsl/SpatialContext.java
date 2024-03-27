/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl;

/**
 * @author Emmanuel Bernard
 * @deprecated See the deprecation note on {@link QueryBuilder}.
 */
@Deprecated
public interface SpatialContext extends QueryCustomization<SpatialContext>, SpatialMatchingContext {

	//TODO score by proximity

	/**
	 * Used to select the spatial field/coordinates used for this query. If not specified, the default field
	 * ({@link org.hibernate.search.annotations.Spatial#COORDINATES_DEFAULT_FIELD}) is used.
	 *
	 * <p>
	 * Note: An entity can have multiple {@link org.hibernate.search.annotations.Spatial} annotations defining
	 * different sets of coordinates. Each non-default {@code Spatial} instance has a name to identify it. Use this method
	 * to specify the targeted coordinate field.
	 * </p>
	 *
	 * @param fieldName The name of the set of coordinates to target for the query
	 *
	 * @return {@code SpatialMatchingContext} instance for continuation
	 */
	SpatialMatchingContext onField(String fieldName);

}
