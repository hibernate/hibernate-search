/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl;

import com.google.gson.annotations.JsonAdapter;

/**
 * A definition of an Elasticsearch analyzer, to be included in index settings.
 *
 */
@JsonAdapter(AnalyzerDefinitionJsonAdapterFactory.class)
public class AnalyzerDefinition extends AbstractCompositeAnalysisDefinition {

	private String tokenizer;

	public String getTokenizer() {
		return tokenizer;
	}

	public void setTokenizer(String tokenizer) {
		this.tokenizer = tokenizer;
	}

}
