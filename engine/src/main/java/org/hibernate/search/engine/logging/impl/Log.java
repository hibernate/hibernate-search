/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.engine.logging.impl;

import java.util.List;

import org.hibernate.search.engine.logging.spi.MappableTypeModelFormatter;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.MessageConstants;
import org.hibernate.search.util.impl.common.logging.ClassFormatter;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.ENGINE_ID_RANGE_MIN, max = MessageConstants.ENGINE_ID_RANGE_MAX),
		// Exceptions for legacy messages from Search 5
		// TODO HSEARCH-3308 add exceptions here for legacy messages from Search 5. See the Lucene logger for examples.
})
public interface Log extends BasicLogger {

	// -----------------------------------
	// Pre-existing messages from Search 5
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_1 = MessageConstants.ENGINE_ID_RANGE_MIN;

	@Message(id = ID_OFFSET_1 + 242,
			value = "Hibernate Search failed to initialize component '%1$s' as class '%2$s' doesn't have a public no-arguments constructor")
	SearchException noPublicNoArgConstructor(String componentName, @FormatWith(ClassFormatter.class) Class<?> clazz);

	// TODO HSEARCH-3308 migrate relevant messages from Search 5 here

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET_2 = MessageConstants.ENGINE_ID_RANGE_MIN + 500;

	@Message(id = ID_OFFSET_2 + 1,
			value = "Unable to convert configuration property '%1$s' with value '%2$s': %3$s")
	SearchException unableToConvertConfigurationProperty(String key, Object rawValue, String errorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 2,
			value = "Invalid value: expected either an instance of '%1$s' or a parsable String.")
	SearchException invalidPropertyValue(Class<?> expectedType, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 3,
			value = "Invalid boolean value: expected either a Boolean, the String 'true' or the String 'false'.")
	SearchException invalidBooleanPropertyValue(@Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 4,
			value = "%1$s")
	SearchException invalidIntegerPropertyValue(String errorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 5,
			value = "%1$s")
	SearchException invalidLongPropertyValue(String errorMessage, @Cause Exception cause);

	@Message(id = ID_OFFSET_2 + 6,
			value = "Invalid multi value: expected either a Collection or a String.")
	SearchException invalidMultiPropertyValue();

	@Message(id = ID_OFFSET_2 + 7,
			value = "Cannot add multiple predicates to the query root; use an explicit boolean predicate instead.")
	SearchException cannotAddMultiplePredicatesToQueryRoot();

	@Message(id = ID_OFFSET_2 + 9,
			value = "Cannot add multiple predicates to a nested predicate; use an explicit boolean predicate instead.")
	SearchException cannotAddMultiplePredicatesToNestedPredicate();

	@Message(id = ID_OFFSET_2 + 11,
			value = "Invalid value: the value to match in match predicates must be non-null."
					+ " Null value was passed to match predicate on fields %1$s")
	SearchException matchPredicateCannotMatchNullValue(List<String> strings);

	@Message(id = ID_OFFSET_2 + 12,
			value = "Invalid value: at least one bound in range predicates must be non-null."
					+ " Null bounds were passed to range predicate on fields %1$s")
	SearchException rangePredicateCannotMatchNullValue(List<String> strings);

	@Message(id = ID_OFFSET_2 + 13,
			value = "Cannot map type '%1$s' to index '%2$s', because this type is abstract."
					+ " Index mappings are not inherited: they apply to exact instances of a given type."
					+ " As a result, mapping an abstract type to an index does not make sense,"
					+ " since the index would always be empty.")
	SearchException cannotMapAbstractTypeToIndex(
			@FormatWith(MappableTypeModelFormatter.class) MappableTypeModel typeModel, String indexName);

	@Message(id = ID_OFFSET_2 + 14,
			value = "Field name '%1$s' is invalid: field names cannot be null or empty.")
	SearchException relativeFieldNameCannotBeNullOrEmpty(String relativeFieldName,
			@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 15,
			value = "Field name '%1$s' is invalid: field names cannot contain a dot ('.')."
					+ " Remove the dot from your field name,"
					+ " or if you are declaring the field in a bridge and want a tree of fields,"
					+ " declare an object field using the objectField() method.")
	SearchException relativeFieldNameCannotContainDot(String relativeFieldName,
			@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 16,
			value = "Invalid polygon: the first point '%1$s' should be identical to the last point '%2$s' to properly close the polygon." )
	IllegalArgumentException invalidGeoPolygonFirstPointNotIdenticalToLastPoint(GeoPoint firstPoint, GeoPoint lastPoint);

	@Message(id = ID_OFFSET_2 + 17,
			value = "Cannot add a predicate to this DSL context anymore, because the DSL context was already closed."
					+ " If you want to re-use predicates, do not re-use the predicate DSL context objects,"
					+ " but rather build SearchPredicate objects.")
	SearchException cannotAddPredicateToUsedContext();

	@Message(id = ID_OFFSET_2 + 18,
			value = "Cannot add a sort to this DSL context anymore, because the DSL context was already closed."
					+ " If you want to re-use sorts, do not re-use the sort DSL context objects,"
					+ " but rather build SearchSort objects.")
	SearchException cannotAddSortToUsedContext();

	@Message(id = ID_OFFSET_2 + 19,
			value = "Hibernate Search bootstrap failed; stopped collecting failures after '%2$s' failures."
					+ " Failures:\n%1$s")
	SearchException boostrapCollectedFailureLimitReached(String renderedFailures, int failureCount);

	@Message(id = ID_OFFSET_2 + 20,
			value = "Hibernate Search bootstrap failed."
					+ " Failures:\n%1$s")
	SearchException bootstrapCollectedFailures(String renderedFailures);

	@LogMessage(level = Logger.Level.ERROR)
	@Message(id = ID_OFFSET_2 + 21,
			value = "Hibernate Search bootstrap encountered a non-fatal failure;"
					+ " continuing bootstrap for now to list all mapping problems,"
					+ " but the bootstrap process will ultimately be aborted.\n"
					+ "Context: %1$s\n"
					+ "Failure:" // The stack trace follows
	)
	void newBootstrapCollectedFailure(String context, @Cause Throwable failure);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = ID_OFFSET_2 + 22,
			value = "Unexpected empty event context; there is a bug in Hibernate Search, please report it")
	void unexpectedEmptyEventContext(@Cause SearchException exceptionForStackTrace);

	@Message(id = ID_OFFSET_2 + 23,
			value = "Incomplete field definition."
					+ " You must call createAccessor() to complete the field definition.")
	SearchException incompleteFieldDefinition(@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 24,
			value = "Multiple calls to createAccessor() for the same field definition."
					+ " You must call createAccessor() exactly once.")
	SearchException cannotCreateAccessorMultipleTimes(@Param EventContext context);

	@Message(id = ID_OFFSET_2 + 25,
			value = "Cannot call ifSupported(...) after orElse(...)."
					+ " Use a separate extension() context, or move the orElse(...) call last."
	)
	SearchException cannotCallDslExtensionIfSupportedAfterOrElse();

	@Message(id = ID_OFFSET_2 + 26,
			value = "None of the provided extensions can be applied to the current context. "
					+ " Attempted extensions: %1$s."
					+ " If you want to ignore this, use .extension().ifSupported(...).orElse(ignored -> { })."
	)
	SearchException dslExtensionNoMatch(List<?> attemptedExtensions);
}
