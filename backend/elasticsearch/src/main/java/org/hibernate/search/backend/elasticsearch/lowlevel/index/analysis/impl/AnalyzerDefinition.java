/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
