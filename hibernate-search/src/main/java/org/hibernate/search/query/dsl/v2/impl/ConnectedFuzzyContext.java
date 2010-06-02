package org.hibernate.search.query.dsl.v2.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Filter;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.query.dsl.v2.FuzzyContext;
import org.hibernate.search.query.dsl.v2.TermMatchingContext;

/**
 * @author Emmanuel Bernard
 */
class ConnectedFuzzyContext implements FuzzyContext {
	private final SearchFactoryImplementor factory;
	private final Analyzer queryAnalyzer;
	private final QueryCustomizer queryCustomizer;
	private final TermQueryContext context;

	public ConnectedFuzzyContext(Analyzer queryAnalyzer, SearchFactoryImplementor factory, QueryCustomizer queryCustomizer) {
		this.factory = factory;
		this.queryAnalyzer = queryAnalyzer;
		this.queryCustomizer = queryCustomizer;
		this.context = new TermQueryContext( TermQueryContext.Approximation.FUZZY);
	}

	public TermMatchingContext onField(String field) {
		return new ConnectedTermMatchingContext(context, field, queryCustomizer, queryAnalyzer, factory);
	}

	public ConnectedFuzzyContext threshold(float threshold) {
		context.setThreshold( threshold );
		return this;
	}

	public ConnectedFuzzyContext prefixLength(int prefixLength) {
		context.setPrefixLength( prefixLength );
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