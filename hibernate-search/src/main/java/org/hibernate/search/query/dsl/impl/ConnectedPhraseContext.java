package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.search.Filter;

import org.hibernate.search.query.dsl.PhraseContext;
import org.hibernate.search.query.dsl.PhraseMatchingContext;

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

	public PhraseContext withSlop(int slop) {
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

	public PhraseContext withConstantScore() {
		queryCustomizer.withConstantScore();
		return this;
	}

	public PhraseContext filteredBy(Filter filter) {
		queryCustomizer.filteredBy(filter);
		return this;
	}
}