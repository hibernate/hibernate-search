/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.facet;

import org.hibernate.search.query.dsl.FacetContext;

/**
 * @author Hardy Ferentschik
 * @deprecated See the deprecation note on {@link FacetContext}.
 */
@Deprecated
public interface RangeFacet<T> extends Facet {
	/**
	 * @return the lower boundary of this range
	 */
	T getMin();

	/**
	 * @return the upper boundary of this range
	 */
	T getMax();

	/**
	 * @return {@code true} if the lower boundary is included in the range, {@code false} otherwise
	 */
	boolean isIncludeMin();

	/**
	 * @return {@code true} if the upper boundary is included in the range, {@code false} otherwise
	 */
	boolean isIncludeMax();
}
