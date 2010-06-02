package org.hibernate.search.query.dsl.v2.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Filter;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.query.dsl.v2.TermMatchingContext;
import org.hibernate.search.query.dsl.v2.WildcardContext;

/**
 * @author Emmanuel Bernard
 */
class ConnectedWildcardContext implements WildcardContext {
	private final SearchFactoryImplementor factory;
	private final Analyzer queryAnalyzer;
	private final QueryCustomizer queryCustomizer;
	private final TermQueryContext context;

	public ConnectedWildcardContext(Analyzer queryAnalyzer, SearchFactoryImplementor factory, QueryCustomizer queryCustomizer) {
		this.factory = factory;
		this.queryAnalyzer = queryAnalyzer;
		this.queryCustomizer = queryCustomizer;
		this.context = new TermQueryContext( TermQueryContext.Approximation.WILDCARD);
	}

	public TermMatchingContext onField(String field) {
		return new ConnectedTermMatchingContext(context, field, queryCustomizer, queryAnalyzer, factory);
	}

	public WildcardContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	public WildcardContext constantScore() {
		queryCustomizer.constantScore();
		return this;
	}

	public WildcardContext filter(Filter filter) {
		queryCustomizer.filter(filter);
		return this;
	}
}