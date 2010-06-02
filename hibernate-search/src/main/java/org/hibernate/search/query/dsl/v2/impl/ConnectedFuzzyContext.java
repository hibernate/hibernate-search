package org.hibernate.search.query.dsl.v2.impl;

import org.apache.lucene.search.Filter;

import org.hibernate.search.query.dsl.v2.FuzzyContext;
import org.hibernate.search.query.dsl.v2.TermMatchingContext;

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

	public ConnectedFuzzyContext threshold(float threshold) {
		termContext.setThreshold( threshold );
		return this;
	}

	public ConnectedFuzzyContext prefixLength(int prefixLength) {
		termContext.setPrefixLength( prefixLength );
		return this;
	}

	public FuzzyContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	public FuzzyContext constantScore() {
		queryCustomizer.constantScore();
		return this;
	}

	public FuzzyContext filter(Filter filter) {
		queryCustomizer.filter(filter);
		return this;
	}

}