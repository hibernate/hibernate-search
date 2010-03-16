package org.hibernate.search.query.dsl.v2.impl;

import org.apache.lucene.analysis.Analyzer;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.v2.QueryBuilder;
import org.hibernate.search.query.dsl.v2.TermContext;

/**
 * Assuming connection with the search factory
 * 
 * @author Emmanuel Bernard
 */
public class ConnectedQueryBuilder implements QueryBuilder {
	private final Analyzer queryAnalyzer;
	private final SearchFactory factory;

	public ConnectedQueryBuilder(Analyzer queryAnalyzer, SearchFactory factory) {
		this.queryAnalyzer = queryAnalyzer;
		this.factory = factory;
	}

	public TermContext term() {
		return new ConnectedTermContext( queryAnalyzer, factory);
	}
}
