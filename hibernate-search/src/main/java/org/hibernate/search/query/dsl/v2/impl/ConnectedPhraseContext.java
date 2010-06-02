package org.hibernate.search.query.dsl.v2.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Filter;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.query.dsl.v2.PhraseContext;
import org.hibernate.search.query.dsl.v2.PhraseMatchingContext;
import org.hibernate.search.query.dsl.v2.RangeContext;
import org.hibernate.search.query.dsl.v2.RangeMatchingContext;

/**
 * @author Emmanuel Bernard
 */
class ConnectedPhraseContext implements PhraseContext {
	private final SearchFactoryImplementor factory;
	private final Analyzer queryAnalyzer;
	private final QueryCustomizer queryCustomizer;
	private final PhraseQueryContext queryContext;


	public ConnectedPhraseContext(Analyzer queryAnalyzer, SearchFactoryImplementor factory) {
		this.factory = factory;
		this.queryAnalyzer = queryAnalyzer;
		this.queryCustomizer = new QueryCustomizer();
		this.queryContext = new PhraseQueryContext();
	}

	public PhraseContext slop(int slop) {
		queryContext.setSlop( slop );
		return this;
	}

	public PhraseMatchingContext onField(String fieldName) {
		return new ConnectedPhraseMatchingContext(fieldName, queryContext, queryCustomizer, queryAnalyzer, factory);
	}

	public PhraseContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	public PhraseContext constantScore() {
		queryCustomizer.constantScore();
		return this;
	}

	public PhraseContext filter(Filter filter) {
		queryCustomizer.filter(filter);
		return this;
	}
}