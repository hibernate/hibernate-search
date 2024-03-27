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
public interface FacetRangeLimitContext<T> {
	FacetRangeLimitContext<T> excludeLimit();

	FacetRangeEndContext<T> to(T upperLimit);
}
