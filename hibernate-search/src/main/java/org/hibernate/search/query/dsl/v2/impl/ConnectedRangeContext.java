package org.hibernate.search.query.dsl.v2.impl;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Filter;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.v2.RangeContext;
import org.hibernate.search.query.dsl.v2.RangeMatchingContext;

/**
 * @author Emmanuel Bernard
 */
class ConnectedRangeContext implements RangeContext {
	private final SearchFactory factory;
	private final Analyzer queryAnalyzer;
	private final QueryCustomizer queryCustomizer;

	public ConnectedRangeContext(Analyzer queryAnalyzer, SearchFactory factory) {
		this.factory = factory;
		this.queryAnalyzer = queryAnalyzer;
		this.queryCustomizer = new QueryCustomizer();
	}

	public RangeMatchingContext onField(String fieldName) {
		return new ConnectedRangeMatchingContext(fieldName, queryCustomizer, queryAnalyzer, factory);
	}

	public RangeContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	public RangeContext constantScore() {
		queryCustomizer.constantScore();
		return this;
	}

	public RangeContext filter(Filter filter) {
		queryCustomizer.filter(filter);
		return this;
	}
}
