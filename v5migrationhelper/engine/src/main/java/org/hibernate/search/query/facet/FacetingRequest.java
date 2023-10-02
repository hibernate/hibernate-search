/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.facet;

import org.hibernate.search.query.dsl.FacetContext;

/**
 * Faceting request interface.
 *
 * @author Hardy Ferentschik
 * @deprecated See the deprecation note on {@link FacetContext}.
 */
@Deprecated
public interface FacetingRequest {
	/**
	 * @return the name of this faceting request. The faceting name can be an arbitrary string.
	 */
	String getFacetingName();

	/**
	 * @return the name of the facet field this faceting request is defined on
	 */
	String getFieldName();

	/**
	 * @return the sort order of the returned {@code Facet}s for this request
	 */
	FacetSortOrder getSort();

	/**
	 * @return the maximum number of facets returned for this request
	 */
	int getMaxNumberOfFacets();

	/**
	 * @return {@code true} if facets with a count of 0 should be included in the returned facet list
	 */
	boolean hasZeroCountsIncluded();
}
