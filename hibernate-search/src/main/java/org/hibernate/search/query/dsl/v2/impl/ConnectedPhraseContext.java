package org.hibernate.search.query.dsl.v2.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Filter;

import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.query.dsl.v2.PhraseContext;
import org.hibernate.search.query.dsl.v2.PhraseMatchingContext;

/**
 * @author Emmanuel Bernard
 */
class ConnectedPhraseContext implements PhraseContext {
	private final QueryBuildingContext queryContext;
	private final QueryCustomizer queryCustomizer;
	private final PhraseQueryContext phraseContext;


	public ConnectedPhraseContext(QueryBuildingContext queryContext) {
		this.queryCustomizer = new QueryCustomizer();
		this.phraseContext = new PhraseQueryContext();
		this.queryContext = queryContext;
	}

	public PhraseContext slop(int slop) {
		phraseContext.setSlop( slop );
		return this;
	}

	public PhraseMatchingContext onField(String fieldName) {
		return new ConnectedPhraseMatchingContext(fieldName, phraseContext, queryCustomizer, queryContext);
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