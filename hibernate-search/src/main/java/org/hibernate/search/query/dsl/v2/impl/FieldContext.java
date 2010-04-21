package org.hibernate.search.query.dsl.v2.impl;

import org.apache.lucene.analysis.Analyzer;

/**
 * @author Emmanuel Bernard
 */
public class FieldContext {
	private final String field;
	private boolean ignoreAnalyzer;
	private final QueryCustomizer fieldCustomizer;

	public FieldContext(String field) {
		this.field = field;
		this.fieldCustomizer = new QueryCustomizer();
	}

	public String getField() {
		return field;
	}

	public boolean isIgnoreAnalyzer() {
		return ignoreAnalyzer;
	}

	public void setIgnoreAnalyzer(boolean ignoreAnalyzer) {
		this.ignoreAnalyzer = ignoreAnalyzer;
	}

	public QueryCustomizer getFieldCustomizer() {
		return fieldCustomizer;
	}
}
