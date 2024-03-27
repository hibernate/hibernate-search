/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.facet;

import org.hibernate.search.query.dsl.FacetContext;

/**
 * Specifies the order in which the facets are returned.
 *
 * @author Hardy Ferentschik
 * @deprecated See the deprecation note on {@link FacetContext}.
 */
@Deprecated
public enum FacetSortOrder {
	/**
	 * Facets are returned by count with the lowest count first
	 */
	COUNT_ASC,

	/**
	 * Facets are returned by count with the lowest count first
	 */
	COUNT_DESC,

	/**
	 * Facets are returned in the alphabetical order
	 */
	FIELD_VALUE,

	/**
	 * The order in which ranges were defined. Only valid for range faceting
	 */
	RANGE_DEFINITION_ORDER
}
