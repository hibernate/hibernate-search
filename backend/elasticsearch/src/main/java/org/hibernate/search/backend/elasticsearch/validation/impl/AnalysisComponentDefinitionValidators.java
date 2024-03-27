/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.validation.impl;

import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.CharFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenizerDefinition;

class AnalysisComponentDefinitionValidators {

	private AnalysisComponentDefinitionValidators() {
	}

	static Validator<CharFilterDefinition> charFilterDefinitionValidator() {
		return new AnalysisDefinitionValidator<>( new AnalysisParameterEquivalenceRegistry.Builder().build() );
	}

	static Validator<TokenizerDefinition> tokenizerDefinitionValidator() {
		return new AnalysisDefinitionValidator<>(
				new AnalysisParameterEquivalenceRegistry.Builder()
						.type( "edgeNGram" )
						.param( "token_chars" ).unorderedArray()
						.end()
						.type( "nGram" )
						.param( "token_chars" ).unorderedArray()
						.end()
						.type( "stop" )
						.param( "stopwords" ).unorderedArray()
						.end()
						.type( "word_delimiter" )
						.param( "protected_words" ).unorderedArray()
						.end()
						.type( "keyword_marker" )
						.param( "keywords" ).unorderedArray()
						.end()
						.type( "pattern_capture" )
						.param( "patterns" ).unorderedArray()
						.end()
						.type( "common_grams" )
						.param( "common_words" ).unorderedArray()
						.end()
						.type( "cjk_bigram" )
						.param( "ignored_scripts" ).unorderedArray()
						.end()
						.build()
		);
	}

	static Validator<TokenFilterDefinition> tokenFilterDefinitionValidator() {
		return new AnalysisDefinitionValidator<>(
				new AnalysisParameterEquivalenceRegistry.Builder()
						.type( "keep_types" )
						.param( "types" ).unorderedArray()
						.end()
						.build()
		);
	}
}
