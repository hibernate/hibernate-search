package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Filter;

import org.hibernate.search.query.dsl.FuzzyContext;
import org.hibernate.search.query.dsl.TermMatchingContext;
import org.hibernate.search.query.dsl.TermContext;
import org.hibernate.search.query.dsl.WildcardContext;

/**
 * @author Emmanuel Bernard
 */
class ConnectedTermContext implements TermContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final TermQueryContext termContext;

	public ConnectedTermContext(QueryBuildingContext queryContext) {
		this.queryContext = queryContext;
		this.queryCustomizer = new QueryCustomizer();
		this.termContext = new TermQueryContext( TermQueryContext.Approximation.EXACT);
	}

	public TermMatchingContext onField(String field) {
		return new ConnectedTermMatchingContext( termContext, field, queryCustomizer, queryContext);
	}

	public TermMatchingContext onFields(String... fields) {
		return new ConnectedTermMatchingContext( termContext, fields, queryCustomizer, queryContext);
	}

	public FuzzyContext fuzzy() {
		return new ConnectedFuzzyContext( queryCustomizer, queryContext );
	}

	public WildcardContext wildcard() {
		return new ConnectedWildcardContext(queryCustomizer, queryContext);
	}

	public ConnectedTermContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	public ConnectedTermContext withConstantScore() {
		queryCustomizer.withConstantScore();
		return this;
	}

	public ConnectedTermContext filteredBy(Filter filter) {
		queryCustomizer.filteredBy(filter);
		return this;
	}
}
