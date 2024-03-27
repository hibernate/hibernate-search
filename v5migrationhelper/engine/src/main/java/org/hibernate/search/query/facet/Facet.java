/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.facet;

import org.hibernate.search.query.dsl.FacetContext;

/**
 * A single facet (field value and count).
 *
 * @author Hardy Ferentschik
 * @deprecated See the deprecation note on {@link FacetContext}.
 */
@Deprecated
public interface Facet {
	/**
	 * @return the faceting name this {@code Facet}	belongs to.
	 *
	 * @see FacetingRequest#getFacetingName()
	 */
	String getFacetingName();

	/**
	 * Return the {@code Document} field name this facet is targeting.
	 * The field needs to be indexed with {@code Analyze.NO}.
	 *
	 * @return the {@code Document} field name this facet is targeting.
	 */
	String getFieldName();

	/**
	 * @return the value of this facet. In case of a discrete facet it is the actual
	 *         {@code Document} field value. In case of a range query the value is a
	 *         string representation of the range.
	 */
	String getValue();

	/**
	 * @return the facet count.
	 */
	int getCount();
}

