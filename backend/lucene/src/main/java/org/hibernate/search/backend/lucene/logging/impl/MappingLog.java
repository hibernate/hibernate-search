/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.logging.impl;

import static org.hibernate.search.backend.lucene.logging.impl.LuceneLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.hibernate.search.engine.backend.types.Highlightable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@CategorizedLogger(
		category = MappingLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface MappingLog {
	String CATEGORY_NAME = "org.hibernate.search.mapping";

	MappingLog INSTANCE = LoggerFactory.make( MappingLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------


	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	@Message(id = ID_OFFSET + 34,
			value = "Duplicate index field definition: '%1$s'."
					+ " Index field names must be unique."
					+ " Look for two property mappings with the same field name,"
					+ " or two indexed-embeddeds with prefixes that lead to conflicting index field names,"
					+ " or two custom bridges declaring index fields with the same name.")
	SearchException indexSchemaNodeNameConflict(String relativeFieldName,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 62, value = "No built-in index field type for class: '%1$s'.")
	SearchException cannotGuessFieldType(@FormatWith(ClassFormatter.class) Class<?> inputType, @Param EventContext context);

	@Message(id = ID_OFFSET + 71,
			value = "Incomplete field definition."
					+ " You must call toReference() to complete the field definition.")
	SearchException incompleteFieldDefinition(@Param EventContext context);

	@Message(id = ID_OFFSET + 72,
			value = "Multiple calls to toReference() for the same field definition."
					+ " You must call toReference() exactly once.")
	SearchException cannotCreateReferenceMultipleTimes(@Param EventContext context);

	@Message(id = ID_OFFSET + 80, value = "Invalid index field type: missing decimal scale."
			+ " Define the decimal scale explicitly. %1$s")
	SearchException nullDecimalScale(String hint, @Param EventContext eventContext);

	@Message(id = ID_OFFSET + 82,
			value = "Invalid index field type: decimal scale '%1$s' is positive."
					+ " The decimal scale of BigInteger fields must be zero or negative.")
	SearchException invalidDecimalScale(Integer decimalScale, @Param EventContext eventContext);

	@Message(id = ID_OFFSET + 125,
			value = "Duplicate index field template definition: '%1$s'."
					+ " Multiple bridges may be trying to access the same index field template, "
					+ " or two indexed-embeddeds may have prefixes that lead to conflicting field names,"
					+ " or you may have declared multiple conflicting mappings."
					+ " In any case, there is something wrong with your mapping and you should fix it.")
	SearchException indexSchemaFieldTemplateNameConflict(String name, @Param EventContext context);

	@Message(id = ID_OFFSET + 143,
			value = "The index schema named predicate '%1$s' was added twice.")
	SearchException indexSchemaNamedPredicateNameConflict(String relativeFilterName, @Param EventContext context);

	@Message(id = ID_OFFSET + 166,
			value = "Cannot use 'NO' in combination with other highlightable values. Applied values are: '%1$s'")
	SearchException unsupportedMixOfHighlightableValues(Set<Highlightable> highlightable);

	@Message(id = ID_OFFSET + 167,
			value = "The '%1$s' term vector storage strategy is not compatible with the fast vector highlighter. " +
					"Either change the strategy to one of `WITH_POSITIONS_PAYLOADS`/`WITH_POSITIONS_OFFSETS_PAYLOADS` or remove the requirement for the fast vector highlighter support.")
	SearchException termVectorDontAllowFastVectorHighlighter(TermVector termVector);

	@Message(id = ID_OFFSET + 168,
			value = "Setting the `highlightable` attribute to an empty array is not supported. " +
					"Set the value to `NO` if the field does not require the highlight projection.")
	SearchException noHighlightableProvided();

	@Message(id = ID_OFFSET + 174,
			value = "Vector '%1$s' cannot be equal to '%2$s'. It must be a positive integer value lesser than or equal to %3$s.")
	SearchException vectorPropertyUnsupportedValue(String property, Integer value, int max);

	@Message(id = ID_OFFSET + 175, value = "No built-in vector index field type for class: '%1$s'.")
	SearchException cannotGuessVectorFieldType(@FormatWith(ClassFormatter.class) Class<?> inputType,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 177, value = "Fields of this type cannot be multivalued.")
	SearchException multiValuedFieldNotAllowed(@Param EventContext context);

	@Message(id = ID_OFFSET + 179, value = "Invalid index field type: missing vector dimension."
			+ " Define the vector dimension explicitly. %1$s")
	SearchException nullVectorDimension(String hint, @Param EventContext eventContext);
}
