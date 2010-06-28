package org.hibernate.search.query.dsl.v2.impl;

import org.apache.lucene.search.Filter;

import org.hibernate.search.query.dsl.v2.RangeContext;
import org.hibernate.search.query.dsl.v2.RangeMatchingContext;

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

	public RangeMatchingContext onField(String fieldName) {
		return new ConnectedRangeMatchingContext(fieldName, queryCustomizer, queryContext);
	}

	public RangeContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	public RangeContext withConstantScore() {
		queryCustomizer.withConstantScore();
		return this;
	}

	public RangeContext filteredBy(Filter filter) {
		queryCustomizer.filteredBy(filter);
		return this;
	}
}
