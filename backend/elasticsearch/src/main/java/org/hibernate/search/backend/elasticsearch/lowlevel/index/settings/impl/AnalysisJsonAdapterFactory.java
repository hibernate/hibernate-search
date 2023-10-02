/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.lowlevel.index.settings.impl;

import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.AbstractConfiguredExtraPropertiesJsonAdapterFactory;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.AnalyzerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.CharFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.NormalizerDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenFilterDefinition;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.analysis.impl.TokenizerDefinition;

import com.google.gson.reflect.TypeToken;

public class AnalysisJsonAdapterFactory extends AbstractConfiguredExtraPropertiesJsonAdapterFactory {

	private static final TypeToken<Map<String, AnalyzerDefinition>> ANALYZER_DEFINITIONS_MAP_TYPE_TOKEN =
			new TypeToken<Map<String, AnalyzerDefinition>>() {
			};

	private static final TypeToken<Map<String, NormalizerDefinition>> NORMALIZER_DEFINITIONS_MAP_TYPE_TOKEN =
			new TypeToken<Map<String, NormalizerDefinition>>() {
			};

	private static final TypeToken<Map<String, TokenizerDefinition>> TOKENIZER_DEFINITIONS_MAP_TYPE_TOKEN =
			new TypeToken<Map<String, TokenizerDefinition>>() {
			};

	private static final TypeToken<Map<String, TokenFilterDefinition>> TOKEN_FILTER_DEFINITIONS_MAP_TYPE_TOKEN =
			new TypeToken<Map<String, TokenFilterDefinition>>() {
			};

	private static final TypeToken<Map<String, CharFilterDefinition>> CHAR_FILTER_DEFINITIONS_MAP_TYPE_TOKEN =
			new TypeToken<Map<String, CharFilterDefinition>>() {
			};

	@Override
	protected <T> void addFields(Builder<T> builder) {
		builder.add( "analyzers", ANALYZER_DEFINITIONS_MAP_TYPE_TOKEN );
		builder.add( "normalizers", NORMALIZER_DEFINITIONS_MAP_TYPE_TOKEN );
		builder.add( "tokenizers", TOKENIZER_DEFINITIONS_MAP_TYPE_TOKEN );
		builder.add( "tokenFilters", TOKEN_FILTER_DEFINITIONS_MAP_TYPE_TOKEN );
		builder.add( "charFilters", CHAR_FILTER_DEFINITIONS_MAP_TYPE_TOKEN );
	}
}
