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
public interface FacetRangeAboveBelowContext<T> extends FacetRangeStartContext<T> {
	FacetRangeBelowContinuationContext<T> below(T min);

	FacetRangeAboveContext<T> above(T max);
}

