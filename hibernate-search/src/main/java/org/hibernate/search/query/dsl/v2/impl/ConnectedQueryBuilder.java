package org.hibernate.search.query.dsl.v2.impl;

import org.apache.lucene.analysis.Analyzer;

import org.hibernate.search.SearchFactory;
import org.hibernate.search.query.dsl.v2.AllContext;
import org.hibernate.search.query.dsl.v2.BooleanJunction;
import org.hibernate.search.query.dsl.v2.FuzzyContext;
import org.hibernate.search.query.dsl.v2.QueryBuilder;
import org.hibernate.search.query.dsl.v2.TermContext;
import org.hibernate.search.query.dsl.v2.WildcardContext;

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

	public TermContext exact() {
		return new ConnectedTermContext(queryAnalyzer, factory);
	}

	public FuzzyContext fuzzy() {
		return new ConnectedFuzzyContext(queryAnalyzer, factory);
	}

	public WildcardContext wildcard() {
		return new ConnectedWildcardContext(queryAnalyzer, factory);
	}

	//fixme Have to use raw types but would be nice to not have to
	public BooleanJunction bool() {
		return new BooleanQueryBuilder();
	}

	public AllContext all() {
		return new ConnectedAllContext();
	}
}
