package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Filter;

import org.hibernate.search.query.dsl.RangeContext;
import org.hibernate.search.query.dsl.RangeMatchingContext;

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
