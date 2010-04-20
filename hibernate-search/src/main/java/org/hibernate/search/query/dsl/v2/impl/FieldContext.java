package org.hibernate.search.query.dsl.v2.impl;

import org.apache.lucene.analysis.Analyzer;

/**
 * @author Emmanuel Bernard
 */
public class FieldContext {
	private final String field;
	private final Analyzer queryAnalyzer;
	private final QueryCustomizer queryCustomizer;
	private boolean ignoreAnalyzer;

	public FieldContext(String field, Analyzer queryAnalyzer, QueryCustomizer queryCustomizer) {
		this.field = field;
		this.queryAnalyzer = queryAnalyzer;
		this.queryCustomizer = queryCustomizer;
	}

	public String getField() {
		return field;
	}

	public Analyzer getQueryAnalyzer() {
		return queryAnalyzer;
	}

	public QueryCustomizer getQueryCustomizer() {
		return queryCustomizer;
	}

	public boolean isIgnoreAnalyzer() {
		return ignoreAnalyzer;
	}

	public void setIgnoreAnalyzer(boolean ignoreAnalyzer) {
		this.ignoreAnalyzer = ignoreAnalyzer;
	}
}
