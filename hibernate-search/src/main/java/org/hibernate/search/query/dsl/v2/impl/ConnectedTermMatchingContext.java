package org.hibernate.search.query.dsl.v2.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.v2.TermMatchingContext;
import org.hibernate.search.query.dsl.v2.TermTermination;

/**
* @author Emmanuel Bernard
*/
public class ConnectedTermMatchingContext implements TermMatchingContext {
	private final SearchFactory factory;
	private final String field;
	private final Analyzer queryAnalyzer;
	private final QueryCustomizer queryCustomizer;
	private boolean ignoreAnalyzer;
	private final QueryContext queryContext;
	private final FieldContext fieldContext;
	private final List<ConnectedTermMatchingContext> otherFields;

	public ConnectedTermMatchingContext(QueryContext queryContext,
			String field, QueryCustomizer queryCustomizer, Analyzer queryAnalyzer, SearchFactory factory) {
		this.factory = factory;
		this.field = field;
		this.queryAnalyzer = queryAnalyzer;
		this.queryCustomizer = queryCustomizer;
		this.queryContext = queryContext;
		this.otherFields = new ArrayList<ConnectedTermMatchingContext>();
		this.fieldContext = null;
	}

	public TermTermination matches(String text) {
		return new ConnectedSingleTermQueryBuilder( queryContext, ignoreAnalyzer, text, field, queryCustomizer, queryAnalyzer, factory);
	}

	public TermMatchingContext boostedTo(float boost) {
		queryCustomizer.boostedTo( boost );
		return this;
	}

	public TermMatchingContext ignoreAnalyzer() {
		this.ignoreAnalyzer = true;
		return this;
	}
}
