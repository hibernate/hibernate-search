/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl;

import org.hibernate.search.query.facet.FacetingRequest;

/**
 * @author Hardy Ferentschik
 * @deprecated See the deprecation note on {@link FacetContext}.
 */
@Deprecated
public interface FacetTermination {
	/**
	 * @return the {@link FacetingRequest} produced by the building process.
	 */
	FacetingRequest createFacetingRequest();
}
