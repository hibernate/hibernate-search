package org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonSerializable;

/**
 * A definition of an Elasticsearch analyzer, to be included in index settings.
 *
 */
@JsonSerializable
public class AnalyzerDefinition extends AbstractCompositeAnalysisDefinition {

	private String tokenizer;

	public String getTokenizer() {
		return tokenizer;
	}

	public void setTokenizer(String tokenizer) {
		this.tokenizer = tokenizer;
	}

}
