package org.hibernate.search.query.dsl.v2.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Filter;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.v2.TermContext;
import org.hibernate.search.query.dsl.v2.TermMatchingContext;

/**
 * @author Emmanuel Bernard
 */
class ConnectedTermContext implements TermContext {
	private final SearchFactory factory;
	private final Analyzer queryAnalyzer;
	private final QueryCustomizer queryCustomizer;
	private final QueryContext context;

	public ConnectedTermContext(Analyzer queryAnalyzer, SearchFactory factory) {
		this.factory = factory;
		this.queryAnalyzer = queryAnalyzer;
		this.queryCustomizer = new QueryCustomizer();
		this.context = new QueryContext( QueryContext.Approximation.EXACT);
	}

	public TermMatchingContext onField(String field) {
		return new ConnectedTermMatchingContext(context, field, queryCustomizer, queryAnalyzer, factory);
	}

	public TermMatchingContext onFields(String... fields) {
		return new ConnectedTermMatchingContext(context, fields, queryCustomizer, queryAnalyzer, factory);
	}

	public TermContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	public TermContext constantScore() {
		queryCustomizer.constantScore();
		return this;
	}

	public TermContext filter(Filter filter) {
		queryCustomizer.filter(filter);
		return this;
	}
}
