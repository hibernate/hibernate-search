package org.hibernate.search.query.dsl.impl;

import org.apache.lucene.analysis.Analyzer;

import org.hibernate.search.engine.SearchFactoryImplementor;

/**
 * Keep the query builder contextual information
 * 
 * @author Emmanuel Bernard
 */
public class QueryBuildingContext {
	private final SearchFactoryImplementor factory;
	private final Analyzer queryAnalyzer;
	private final Class<?> entityType;

	public QueryBuildingContext(SearchFactoryImplementor factory, Analyzer queryAnalyzer, Class<?> entityType) {
		this.factory = factory;
		this.queryAnalyzer = queryAnalyzer;
		this.entityType = entityType;
	}

	public SearchFactoryImplementor getFactory() {
		return factory;
	}

	public Analyzer getQueryAnalyzer() {
		return queryAnalyzer;
	}

	public Class<?> getEntityType() {
		return entityType;
	}
}
