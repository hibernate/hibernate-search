package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Filter;

import org.hibernate.search.query.dsl.FuzzyContext;
import org.hibernate.search.query.dsl.TermMatchingContext;

/**
 * @author Emmanuel Bernard
 */
class ConnectedFuzzyContext implements FuzzyContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final TermQueryContext termContext;

	public ConnectedFuzzyContext(QueryCustomizer queryCustomizer, QueryBuildingContext queryContext) {
		this.queryCustomizer = queryCustomizer;
		this.termContext = new TermQueryContext( TermQueryContext.Approximation.FUZZY);
		this.queryContext = queryContext;
	}

	public TermMatchingContext onField(String field) {
		return new ConnectedTermMatchingContext(termContext, field, queryCustomizer, queryContext);
	}

	public ConnectedFuzzyContext withThreshold(float threshold) {
		termContext.setThreshold( threshold );
		return this;
	}

	public ConnectedFuzzyContext withPrefixLength(int prefixLength) {
		termContext.setPrefixLength( prefixLength );
		return this;
	}

	public FuzzyContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	public FuzzyContext withConstantScore() {
		queryCustomizer.withConstantScore();
		return this;
	}

	public FuzzyContext filteredBy(Filter filter) {
		queryCustomizer.filteredBy(filter);
		return this;
	}

}