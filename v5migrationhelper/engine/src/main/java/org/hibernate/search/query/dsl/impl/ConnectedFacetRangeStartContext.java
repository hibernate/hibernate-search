/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.FacetRangeLimitContext;
import org.hibernate.search.query.dsl.FacetRangeStartContext;

/**
 * @author Hardy Ferentschik
 */
public class ConnectedFacetRangeStartContext<T> implements FacetRangeStartContext<T> {
	private final FacetBuildingContext context;

	public ConnectedFacetRangeStartContext(FacetBuildingContext context) {
		this.context = context;
	}

	@Override
	public FacetRangeLimitContext<T> from(T rangeStart) {
		context.setRangeStart( rangeStart );
		return new ConnectedFacetRangeLimitContext<T>( context );
	}
}

