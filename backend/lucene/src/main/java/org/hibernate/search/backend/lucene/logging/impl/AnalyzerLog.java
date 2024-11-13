/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.logging.impl;

import static org.hibernate.search.backend.lucene.logging.impl.LuceneLog.ID_OFFSET;
import static org.hibernate.search.backend.lucene.logging.impl.LuceneLog.ID_OFFSET_LEGACY_ENGINE;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@CategorizedLogger(
		category = AnalyzerLog.CATEGORY_NAME,
		description = """
				Logs information on misconfigured or misbehaving analyzers/normalisers.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface AnalyzerLog {
	String CATEGORY_NAME = "org.hibernate.search.analyzer";

	AnalyzerLog INSTANCE = LoggerFactory.make( AnalyzerLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 329,
			value = "Unable to apply analysis configuration: %1$s")
	SearchException unableToApplyAnalysisConfiguration(String errorMessage, @Cause Exception e);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 337,
			value = "Ambiguous value for parameter '%1$s': this parameter is set to two different values '%2$s' and '%3$s'.")
	SearchException analysisComponentParameterConflict(String name, String value1, String value2);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 342,
			value = "Invalid index field type: both analyzer '%1$s' and normalizer '%2$s' are assigned to this type."
					+ " Either an analyzer or a normalizer can be assigned, but not both.")
	SearchException cannotApplyAnalyzerAndNormalizer(String analyzerName, String normalizerName, @Param EventContext context);

	@LogMessage(level = Logger.Level.WARN)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 344,
			value = "Invalid normalizer implementation: the normalizer for definition '%s' produced %d tokens."
					+ " Normalizers should never produce more than one token."
					+ " The tokens have been concatenated by Hibernate Search,"
					+ " but you should fix your normalizer definition.")
	void normalizerProducedMultipleTokens(String normalizerName, int token);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 345,
			value = "Invalid index field type: both analyzer '%1$s' and sorts are enabled."
					+ " Sorts are not supported on analyzed fields."
					+ " If you need an analyzer simply to transform the text (lowercasing, ...)"
					+ " without splitting it into tokens, use a normalizer instead."
					+ " If you need an actual analyzer (with tokenization), define two separate fields:"
					+ " one with an analyzer that is not sortable, and one with a normalizer that is sortable.")
	SearchException cannotUseAnalyzerOnSortableField(String analyzerName, @Param EventContext context);

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 353,
			value = "Unknown analyzer: '%1$s'. Make sure you defined this analyzer.")
	SearchException unknownAnalyzer(String analyzerName, @Param EventContext context);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------

	@Message(id = ID_OFFSET + 52,
			value = "Unable to create analyzer for name '%1$s': %2$s")
	SearchException unableToCreateAnalyzer(String name, String causeMessage, @Cause Exception e);

	@Message(id = ID_OFFSET + 53,
			value = "Unable to create normalizer for name '%1$s': %2$s")
	SearchException unableToCreateNormalizer(String name, String causeMessage, @Cause Exception e);

	@Message(id = ID_OFFSET + 54,
			value = "Unknown normalizer: '%1$s'. Make sure you defined this normalizer.")
	SearchException unknownNormalizer(String normalizerName, @Param EventContext context);

	@Message(id = ID_OFFSET + 73,
			value = "Invalid index field type: both null token '%2$s' ('indexNullAs')"
					+ " and analyzer '%1$s' are assigned to this type."
					+ " 'indexNullAs' is not supported on analyzed fields.")
	SearchException cannotUseIndexNullAsAndAnalyzer(String analyzerName, String indexNullAs, @Param EventContext context);

	@Message(id = ID_OFFSET + 94,
			value = "Invalid index field type: both analyzer '%1$s' and aggregations are enabled."
					+ " Aggregations are not supported on analyzed fields."
					+ " If you need an analyzer simply to transform the text (lowercasing, ...)"
					+ " without splitting it into tokens, use a normalizer instead."
					+ " If you need an actual analyzer (with tokenization), define two separate fields:"
					+ " one with an analyzer that is not aggregable, and one with a normalizer that is aggregable.")
	SearchException cannotUseAnalyzerOnAggregableField(String analyzerName, @Param EventContext context);

	@Message(id = ID_OFFSET + 104,
			value = "Invalid index field type: search analyzer '%1$s' is assigned to this type,"
					+ " but the indexing analyzer is missing."
					+ " Assign an indexing analyzer and a search analyzer, or remove the search analyzer.")
	SearchException searchAnalyzerWithoutAnalyzer(String searchAnalyzer, @Param EventContext context);

	@Message(id = ID_OFFSET + 142,
			value = "Unable to create instance of analysis component '%1$s': %2$s")
	SearchException unableToCreateAnalysisComponent(@FormatWith(ClassFormatter.class) Class<?> type, String causeMessage,
			@Cause Exception cause);

	@Message(id = ID_OFFSET + 181, value = "An analyzer '%1$s' cannot be found.")
	SearchException noSuchAnalyzer(String analyzer);

	@Message(id = ID_OFFSET + 182, value = "A normalizer '%1$s' cannot be found.")
	SearchException noSuchNormalizer(String normalizer);

	@Message(id = ID_OFFSET + 183, value = "Failed to apply '%1$s' to '%2$s': '%3$s'")
	SearchException unableToPerformAnalysisOperation(String analyzerName, String string, String reason,
			@Cause IOException cause);
}
