package org.hibernate.search.query.dsl.v2.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Filter;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.v2.FuzzyContext;
import org.hibernate.search.query.dsl.v2.TermContext;
import org.hibernate.search.query.dsl.v2.TermMatchingContext;
import org.hibernate.search.query.dsl.v2.WildcardContext;

/**
 * @author Emmanuel Bernard
 */
class ConnectedTermContext implements TermContext {
	private final SearchFactory factory;
	private final Analyzer queryAnalyzer;
	private final QueryCustomizer queryCustomizer;
	private final TermQueryContext context;

	public ConnectedTermContext(Analyzer queryAnalyzer, SearchFactory factory) {
		this.factory = factory;
		this.queryAnalyzer = queryAnalyzer;
		this.queryCustomizer = new QueryCustomizer();
		this.context = new TermQueryContext( TermQueryContext.Approximation.EXACT);
	}

	public TermMatchingContext onField(String field) {
		return new ConnectedTermMatchingContext(context, field, queryCustomizer, queryAnalyzer, factory);
	}

	public TermMatchingContext onFields(String... fields) {
		return new ConnectedTermMatchingContext(context, fields, queryCustomizer, queryAnalyzer, factory);
	}

	public FuzzyContext fuzzy() {
		return new ConnectedFuzzyContext( queryAnalyzer, factory, queryCustomizer );
	}

	public WildcardContext wildcard() {
		return new ConnectedWildcardContext( queryAnalyzer, factory, queryCustomizer);
	}

	public ConnectedTermContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	public ConnectedTermContext constantScore() {
		queryCustomizer.constantScore();
		return this;
	}

	public ConnectedTermContext filter(Filter filter) {
		queryCustomizer.filter(filter);
		return this;
	}
}
