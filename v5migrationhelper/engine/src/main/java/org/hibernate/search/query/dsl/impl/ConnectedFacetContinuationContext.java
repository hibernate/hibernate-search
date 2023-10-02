/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.DiscreteFacetContext;
import org.hibernate.search.query.dsl.FacetContinuationContext;
import org.hibernate.search.query.dsl.FacetRangeAboveBelowContext;

/**
 * @author Hardy Ferentschik
 */
public class ConnectedFacetContinuationContext implements FacetContinuationContext {
	private final FacetBuildingContext context;

	public ConnectedFacetContinuationContext(FacetBuildingContext context) {
		this.context = context;
	}

	@Override
	public <T> FacetRangeAboveBelowContext<T> range() {
		context.setRangeQuery( true );
		return new ConnectedFacetRangeAboveBelowContext<T>( context );
	}

	@Override
	public DiscreteFacetContext discrete() {
		return new ConnectedDiscreteFacetContext( context );
	}
}

