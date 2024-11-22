/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.logging.impl;

import static org.hibernate.search.engine.logging.impl.EngineLog.ID_OFFSET;
import static org.hibernate.search.engine.logging.impl.EngineLog.ID_OFFSET_LEGACY;

import java.lang.invoke.MethodHandles;
import java.time.Duration;

import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.common.RewriteMethod;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.DurationInSecondsAndFractionsFormatter;
import org.hibernate.search.util.common.logging.impl.EventContextNoPrefixFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@CategorizedLogger(
		category = QueryLog.CATEGORY_NAME,
		description = "Logs related to the queries."
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface QueryLog {
	String CATEGORY_NAME = "org.hibernate.search.query";

	QueryLog INSTANCE = LoggerFactory.make( QueryLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------

	@Message(id = ID_OFFSET_LEGACY + 237,
			value = "Invalid range: at least one bound in range predicates must be non-null.")
	SearchException rangePredicateCannotMatchNullValue(@Param EventContext context);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	@Message(id = ID_OFFSET + 51,
			value = "Invalid use of per-field boost: the predicate score is constant."
					+ " Cannot assign a different boost to each field when the predicate score is constant.")
	SearchException perFieldBoostWithConstantScore();

	@Message(id = ID_OFFSET + 53,
			value = "Invalid slop: %1$d. The slop must be positive or zero.")
	SearchException invalidPhrasePredicateSlop(int slop);

	@Message(id = ID_OFFSET + 54,
			value = "Invalid maximum edit distance: %1$d. The value must be 0, 1 or 2.")
	SearchException invalidFuzzyMaximumEditDistance(int maximumEditDistance);

	@Message(id = ID_OFFSET + 55,
			value = "Invalid exact prefix length: %1$d. The value must be positive or zero.")
	SearchException invalidExactPrefixLength(int exactPrefixLength);

	@Message(id = ID_OFFSET + 61, value = "Multiple hits when a single hit was expected.")
	SearchException nonSingleHit();

	@Message(id = ID_OFFSET + 65,
			value = "Unknown aggregation key '%1$s'. This key was not used when building the search query.")
	SearchException unknownAggregationKey(AggregationKey<?> key);

	@Message(id = ID_OFFSET + 72,
			value = "Inconsistent index data: a supposedly single-valued field returned multiple values. Values: [%1$s, %2$s].")
	SearchException unexpectedMultiValuedField(Object value1, Object value2);

	@Message(id = ID_OFFSET + 83, value = "Invalid type for DSL arguments: '%1$s'. Expected '%2$s' or a subtype.")
	SearchException invalidDslArgumentType(@FormatWith(ClassFormatter.class) Class<?> type,
			@FormatWith(ClassFormatter.class) Class<?> correctType,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 84, value = "Invalid type for returned values: '%1$s'. Expected '%2$s' or a supertype.")
	SearchException invalidReturnType(@FormatWith(ClassFormatter.class) Class<?> type,
			@FormatWith(ClassFormatter.class) Class<?> correctType,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 86, value = "Operation exceeded the timeout of %1$s.")
	SearchTimeoutException timedOut(@FormatWith(DurationInSecondsAndFractionsFormatter.class) Duration timeout,
			@Cause Exception cause);

	@Message(id = ID_OFFSET + 87,
			value = "Unable to provide the exact total hit count: only a lower-bound approximation is available."
					+ " This is generally the result of setting query options such as a timeout or the total hit count threshold."
					+ " Either unset these options, or retrieve the lower-bound hit count approximation through '.total().hitCountLowerBound()'.")
	SearchException notExactTotalHitCount();

	@Message(id = ID_OFFSET + 101,
			value = "Inconsistent configuration for %1$s in a search query across multiple indexes: %2$s")
	SearchException inconsistentConfigurationInContextForSearch(
			@FormatWith(EventContextNoPrefixFormatter.class) EventContext elementContext, String causeMessage,
			@Param EventContext elementContextAsParam, @Cause SearchException cause);

	@Message(id = ID_OFFSET + 102,
			value = "Inconsistent support for '%1$s': %2$s")
	SearchException inconsistentSupportForQueryElement(SearchQueryElementTypeKey<?> key,
			String causeMessage, @Cause SearchException cause);

	@Message(id = ID_OFFSET + 103,
			value = "Attribute '%1$s' differs: '%2$s' vs. '%3$s'.")
	SearchException differentAttribute(String attributeName, Object component1, Object component2);

	@Message(id = ID_OFFSET + 104, value = "Cannot use '%2$s' on %1$s: %3$s")
	SearchException cannotUseQueryElementForIndexNode(
			@FormatWith(EventContextNoPrefixFormatter.class) EventContext elementContext,
			SearchQueryElementTypeKey<?> key, String hint, @Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 106,
			value = "'%1$s' can be used in some of the targeted indexes, but not all of them. %2$s")
	SearchException partialSupportForQueryElement(SearchQueryElementTypeKey<?> key, String hint);

	@Message(id = ID_OFFSET + 109,
			value = "This field is a value field in some indexes, but an object field in other indexes.")
	SearchException conflictingFieldModel();

	@Message(id = ID_OFFSET + 110,
			value = "Unknown field '%1$s'.")
	SearchException unknownFieldForSearch(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET + 111,
			value = "Invalid target fields:"
					+ " fields [%1$s, %3$s] are in different nested documents (%2$s vs. %4$s)."
					+ " All target fields must be in the same document.")
	SearchException targetFieldsSpanningMultipleNestedPaths(String fieldPath1,
			@FormatWith(EventContextNoPrefixFormatter.class) EventContext nestedPath1,
			String fieldPath2, @FormatWith(EventContextNoPrefixFormatter.class) EventContext nestedPath2);

	@Message(id = ID_OFFSET + 114,
			value = "Cannot project on entity type '%1$s': this type cannot be loaded from an external datasource,"
					+ " and the documents from the index cannot be projected to its Java class '%2$s'."
					+ " %3$s")
	SearchException cannotCreateEntityProjection(String name, @FormatWith(ClassFormatter.class) Class<?> javaClass,
			String hint);

	@Message(id = ID_OFFSET + 118,
			value = "Invalid type for entity projection on type '%1$s':"
					+ " the entity type's Java class '%2$s' does not extend the requested projection type '%3$s'.")
	SearchException invalidTypeForEntityProjection(String name, @FormatWith(ClassFormatter.class) Class<?> entityType,
			@FormatWith(ClassFormatter.class) Class<?> requestedEntityType);

	@Message(id = ID_OFFSET + 121,
			value = "Cannot use rewrite method '%1$s': this method requires parameter 'n', which was not specified."
					+ " Use another version of the rewrite(...) method that accepts parameter 'n'.")
	SearchException parameterizedRewriteMethodWithoutParameter(RewriteMethod rewriteMethod);

	@Message(id = ID_OFFSET + 122,
			value = "Cannot use rewrite method '%1$s': this method does not accept parameter 'n', but it was specified."
					+ " Use another version of the rewrite(...) method that does not accept parameter 'n'.")
	SearchException nonParameterizedRewriteMethodWithParameter(RewriteMethod rewriteMethod);

	@Message(id = ID_OFFSET + 124,
			value = "Query parameter '%1$s' is not set."
					+ " Use `.param(..)` methods on the query to set any parameters that the query requires.")
	SearchException cannotFindQueryParameter(String parameter);

	@Message(id = ID_OFFSET + 125,
			value = "Expecting value of query parameter '%1$s' to be of type %2$s,"
					+ " but instead got a value of type %3$s.")
	SearchException unexpectedQueryParameterType(String name, @FormatWith(ClassFormatter.class) Class<?> expected,
			@FormatWith(ClassFormatter.class) Class<?> actual);

	@Message(id = ID_OFFSET + 126, value = "Named value '%1$s' has not been defined.")
	SearchException namedValuesParameterNotDefined(String name);

	@Message(id = ID_OFFSET + 127,
			value = "Expecting value of named value '%1$s' to be of type %2$s,"
					+ " but instead got a value of type %3$s.")
	SearchException namedValuesParameterIncorrectType(String name, @FormatWith(ClassFormatter.class) Class<?> expected,
			@FormatWith(ClassFormatter.class) Class<?> actual);


}
