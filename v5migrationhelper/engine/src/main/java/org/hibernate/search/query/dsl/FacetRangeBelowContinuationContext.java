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
public interface FacetRangeBelowContinuationContext<T>
		extends FacetRangeStartContext<T>, FacetTermination, FacetParameterContext {
	FacetRangeBelowContinuationContext<T> excludeLimit();

	FacetRangeAboveContext<T> above(T max);
}
