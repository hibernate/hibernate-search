/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl;

/**
 * @author Hardy Ferentschik
 * @deprecated See the deprecation note on {@link FacetContext}.
 */
@Deprecated
public interface FacetContinuationContext {
	/**
	 * Start building a range request
	 *
	 * @param <T> the type of the values in the range
	 * @return a {@code FacetRangeContext} to continue building the facet request
	 */
	<T> FacetRangeAboveBelowContext<T> range();

	/**
	 * Start building a discrete facet
	 *
	 * @return  a {@code FacetRangeContext} to continue building the facet request
	 */
	DiscreteFacetContext discrete();
}

