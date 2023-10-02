/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.SpatialMatchingContext;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.query.dsl.WithinContext;

/**
 * @author Emmanuel Bernard
 */
public class ConnectedSpatialMatchingContext implements SpatialMatchingContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final SpatialQueryContext spatialContext;

	public ConnectedSpatialMatchingContext(QueryBuildingContext queryContext, QueryCustomizer queryCustomizer,
			SpatialQueryContext spatialContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = queryCustomizer;
		this.spatialContext = spatialContext;
	}

	@Override
	public WithinContext within(double distance, Unit unit) {
		spatialContext.setRadius( distance, unit );
		return new ConnectedWithinContext( this );
	}

	QueryBuildingContext getQueryContext() {
		return queryContext;
	}

	QueryCustomizer getQueryCustomizer() {
		return queryCustomizer;
	}

	SpatialQueryContext getSpatialContext() {
		return spatialContext;
	}
}
