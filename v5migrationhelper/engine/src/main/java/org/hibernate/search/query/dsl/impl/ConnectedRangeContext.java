/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl.impl;

import org.hibernate.search.query.dsl.RangeContext;
import org.hibernate.search.query.dsl.RangeMatchingContext;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
class ConnectedRangeContext implements RangeContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;

	public ConnectedRangeContext(QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = new QueryCustomizer();
	}

	@Override
	public RangeMatchingContext onField(String fieldName) {
		return new ConnectedRangeMatchingContext( fieldName, queryCustomizer, queryContext );
	}

	@Override
	public RangeContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	@Override
	public RangeContext withConstantScore() {
		queryCustomizer.withConstantScore();
		return this;
	}

	@Override
	public RangeContext filteredBy(Query filter) {
		queryCustomizer.filteredBy( filter );
		return this;
	}
}
