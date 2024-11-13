/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.logging.impl;

import static org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@CategorizedLogger(
		category = MappingLog.CATEGORY_NAME,
		description = """
				Logs the information on normalizing the index names for the Elasticsearch backend.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface MappingLog extends BasicLogger {
	String CATEGORY_NAME = "org.hibernate.search.mapping";

	MappingLog INSTANCE = LoggerFactory.make( MappingLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	@Message(id = ID_OFFSET + 20,
			value = "Duplicate index field definition: '%1$s'."
					+ " Index field names must be unique."
					+ " Look for two property mappings with the same field name,"
					+ " or two indexed-embeddeds with prefixes that lead to conflicting index field names,"
					+ " or two custom bridges declaring index fields with the same name.")
	SearchException indexSchemaNodeNameConflict(String name, @Param EventContext context);

	@Message(id = ID_OFFSET + 30,
			value = "Conflicting index names: Hibernate Search indexes '%1$s' and '%2$s'"
					+ " both target the Elasticsearch index name or alias '%3$s'")
	SearchException conflictingIndexNames(String firstHibernateSearchIndexName, String secondHibernateSearchIndexName,
			String nameOrAlias);

	@Message(id = ID_OFFSET + 45, value = "No built-in index field type for class: '%1$s'.")
	SearchException cannotGuessFieldType(@FormatWith(ClassFormatter.class) Class<?> inputType, @Param EventContext context);

	@Message(id = ID_OFFSET + 54,
			value = "Incomplete field definition."
					+ " You must call toReference() to complete the field definition.")
	SearchException incompleteFieldDefinition(@Param EventContext context);

	@Message(id = ID_OFFSET + 55,
			value = "Multiple calls to toReference() for the same field definition."
					+ " You must call toReference() exactly once.")
	SearchException cannotCreateReferenceMultipleTimes(@Param EventContext context);

	@Message(id = ID_OFFSET + 67, value = "Invalid index field type: missing decimal scale."
			+ " Define the decimal scale explicitly.  %1$s")
	SearchException nullDecimalScale(String hint, @Param EventContext eventContext);

	@Message(id = ID_OFFSET + 70,
			value = "Invalid index field type: decimal scale '%1$s' is positive."
					+ " The decimal scale of BigInteger fields must be zero or negative.")
	SearchException invalidDecimalScale(Integer decimalScale, @Param EventContext eventContext);

	@Message(id = ID_OFFSET + 96,
			value = "Invalid Elasticsearch index layout:"
					+ " the write alias and read alias are set to the same value: '%1$s'."
					+ " The write alias and read alias must be different.")
	SearchException sameWriteAndReadAliases(URLEncodedString writeAndReadAlias);

	@Message(id = ID_OFFSET + 107,
			value = "Duplicate index field template definition: '%1$s'."
					+ " Multiple bridges may be trying to access the same index field template, "
					+ " or two indexed-embeddeds may have prefixes that lead to conflicting field names,"
					+ " or you may have declared multiple conflicting mappings."
					+ " In any case, there is something wrong with your mapping and you should fix it.")
	SearchException indexSchemaFieldTemplateNameConflict(String name, @Param EventContext context);

	@Message(id = ID_OFFSET + 131, value = "Unable to find the given custom index settings file: '%1$s'.")
	SearchException customIndexSettingsFileNotFound(String filePath, @Param EventContext context);

	@Message(id = ID_OFFSET + 132, value = "Error on loading the given custom index settings file '%1$s': %2$s")
	SearchException customIndexSettingsErrorOnLoading(String filePath, String causeMessage, @Cause Exception cause,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 133,
			value = "There are some JSON syntax errors on the given custom index settings file '%1$s': %2$s")
	SearchException customIndexSettingsJsonSyntaxErrors(String filePath, String causeMessage, @Cause Exception cause,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 137, value = "The index schema named predicate '%1$s' was added twice.")
	SearchException indexSchemaNamedPredicateNameConflict(String relativeFilterName, @Param EventContext context);

	@Message(id = ID_OFFSET + 151, value = "Unable to find the given custom index mapping file: '%1$s'.")
	SearchException customIndexMappingFileNotFound(String filePath, @Param EventContext context);

	@Message(id = ID_OFFSET + 152, value = "Error on loading the given custom index mapping file '%1$s': %2$s")
	SearchException customIndexMappingErrorOnLoading(String filePath, String causeMessage, @Cause Exception cause,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 153,
			value = "There are some JSON syntax errors on the given custom index mapping file '%1$s': %2$s")
	SearchException customIndexMappingJsonSyntaxErrors(String filePath, String causeMessage, @Cause Exception cause,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 167,
			value = "Cannot use 'NO' in combination with other highlightable values. Applied values are: '%1$s'")
	SearchException unsupportedMixOfHighlightableValues(Set<Highlightable> highlightable);

	@Message(id = ID_OFFSET + 168,
			value = "The '%1$s' term vector storage strategy is not compatible with the fast vector highlighter. " +
					"Either change the strategy to one of `WITH_POSITIONS_PAYLOADS`/`WITH_POSITIONS_OFFSETS_PAYLOADS` or remove the requirement for the fast vector highlighter support.")
	SearchException termVectorDontAllowFastVectorHighlighter(TermVector termVector);

	@Message(id = ID_OFFSET + 169,
			value = "Setting the `highlightable` attribute to an empty array is not supported. " +
					"Set the value to `NO` if the field does not require the highlight projection.")
	SearchException noHighlightableProvided();

	@Message(id = ID_OFFSET + 178, value = "No built-in vector index field type for class: '%1$s'.")
	SearchException cannotGuessVectorFieldType(@FormatWith(ClassFormatter.class) Class<?> inputType,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 179, value = "Invalid index field type: missing vector dimension."
			+ " Define the vector dimension explicitly. %1$s")
	SearchException nullVectorDimension(String hint, @Param EventContext eventContext);

	@Message(id = ID_OFFSET + 180, value = "Fields of this type cannot be multivalued.")
	SearchException multiValuedFieldNotAllowed(@Param EventContext context);

	@Message(id = ID_OFFSET + 188,
			value = "The OpenSearch distribution does not allow using %1$s as a space type for a Lucene engine."
					+ " Try using a different similarity type and refer to the OpenSearch documentation for more details.")
	SearchException vectorSimilarityNotSupportedByOpenSearchBackend(VectorSimilarity vectorSimilarity);
}
