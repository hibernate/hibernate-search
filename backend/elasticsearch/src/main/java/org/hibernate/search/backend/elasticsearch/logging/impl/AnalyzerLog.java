/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.logging.impl;

import static org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLog.ID_OFFSET;
import static org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLog.ID_OFFSET_LEGACY_ES;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

import com.google.gson.JsonElement;

@CategorizedLogger(
		category = AnalyzerLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface AnalyzerLog {
	String CATEGORY_NAME = "org.hibernate.search.analyzer";

	AnalyzerLog INSTANCE = LoggerFactory.make( AnalyzerLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------

	@Message(id = ID_OFFSET_LEGACY_ES + 55,
			value = "Duplicate tokenizer definitions: '%1$s'. Tokenizer names must be unique.")
	SearchException tokenizerNamingConflict(String remoteName);

	@Message(id = ID_OFFSET_LEGACY_ES + 56,
			value = "Duplicate char filter definitions: '%1$s'. Char filter names must be unique.")
	SearchException charFilterNamingConflict(String remoteName);

	@Message(id = ID_OFFSET_LEGACY_ES + 57,
			value = "Duplicate token filter definitions: '%1$s'. Token filter names must be unique.")
	SearchException tokenFilterNamingConflict(String remoteName);

	@Message(id = ID_OFFSET_LEGACY_ES + 75,
			value = "Unable to apply analysis configuration: %1$s")
	SearchException unableToApplyAnalysisConfiguration(String errorMessage, @Cause Exception e,
			@Param EventContext eventContext);

	@Message(id = ID_OFFSET_LEGACY_ES + 76,
			value = "Invalid analyzer definition for name '%1$s'. Analyzer definitions must at least define the tokenizer.")
	SearchException invalidElasticsearchAnalyzerDefinition(String name);

	@Message(id = ID_OFFSET_LEGACY_ES + 77,
			value = "Invalid tokenizer definition for name '%1$s'. Tokenizer definitions must at least define the tokenizer type.")
	SearchException invalidElasticsearchTokenizerDefinition(String name);

	@Message(id = ID_OFFSET_LEGACY_ES + 78,
			value = "Invalid char filter definition for name '%1$s'. Char filter definitions must at least define the char filter type.")
	SearchException invalidElasticsearchCharFilterDefinition(String name);

	@Message(id = ID_OFFSET_LEGACY_ES + 79,
			value = "Invalid token filter definition for name '%1$s'. Token filter definitions must at least define the token filter type.")
	SearchException invalidElasticsearchTokenFilterDefinition(String name);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------

	@Message(id = ID_OFFSET + 34,
			value = "Invalid typed analyzer definition for name '%1$s'. Typed analyzer definitions must at least define the analyzer type.")
	SearchException invalidElasticsearchTypedAnalyzerDefinition(String name);

	@Message(id = ID_OFFSET + 35,
			value = "Invalid index field type: both analyzer '%1$s' and normalizer '%2$s' are assigned to this type."
					+ " Either an analyzer or a normalizer can be assigned, but not both.")
	SearchException cannotApplyAnalyzerAndNormalizer(String analyzerName, String normalizerName, @Param EventContext context);

	@Message(id = ID_OFFSET + 36,
			value = "Invalid index field type: both analyzer '%1$s' and sorts are enabled."
					+ " Sorts are not supported on analyzed fields."
					+ " If you need an analyzer simply to transform the text (lowercasing, ...)"
					+ " without splitting it into tokens, use a normalizer instead."
					+ " If you need an actual analyzer (with tokenization), define two separate fields:"
					+ " one with an analyzer that is not sortable, and one with a normalizer that is sortable.")
	SearchException cannotUseAnalyzerOnSortableField(String analyzerName, @Param EventContext context);

	@Message(id = ID_OFFSET + 37,
			value = "Ambiguous value for parameter '%1$s': this parameter is set to two different values '%2$s' and '%3$s'.")
	SearchException analysisComponentParameterConflict(String name, JsonElement value1, JsonElement value2);

	@Message(id = ID_OFFSET + 60, value = "Cannot skip analysis on field '%1$s':"
			+ " the Elasticsearch backend will always normalize arguments before attempting matches on normalized fields.")
	SearchException skipAnalysisOnNormalizedField(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET + 62,
			value = "Invalid index field type: both null token '%2$s' ('indexNullAs')"
					+ " and analyzer '%1$s' are assigned to this type."
					+ " 'indexNullAs' is not supported on analyzed fields.")
	SearchException cannotUseIndexNullAsAndAnalyzer(String analyzerName, String indexNullAs, @Param EventContext context);

	@Message(id = ID_OFFSET + 76,
			value = "Invalid index field type: both analyzer '%1$s' and aggregations are enabled."
					+ " Aggregations are not supported on analyzed fields."
					+ " If you need an analyzer simply to transform the text (lowercasing, ...)"
					+ " without splitting it into tokens, use a normalizer instead."
					+ " If you need an actual analyzer (with tokenization), define two separate fields:"
					+ " one with an analyzer that is not aggregable, and one with a normalizer that is aggregable.")
	SearchException cannotUseAnalyzerOnAggregableField(String analyzerName, @Param EventContext context);

	@Message(id = ID_OFFSET + 87,
			value = "Invalid index field type: search analyzer '%1$s' is assigned to this type,"
					+ " but the indexing analyzer is missing."
					+ " Assign an indexing analyzer and a search analyzer, or remove the search analyzer.")
	SearchException searchAnalyzerWithoutAnalyzer(String searchAnalyzer, @Param EventContext context);

}
