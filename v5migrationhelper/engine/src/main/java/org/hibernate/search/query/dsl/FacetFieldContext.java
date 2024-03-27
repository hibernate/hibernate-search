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
public interface FacetFieldContext {
	/**
	 *
	 * @param fieldName the field fieldName to be used for faceting
	 * @return a {@code FacetContinuationContext} to continue building the facet request
	 */
	FacetContinuationContext onField(String fieldName);
}

