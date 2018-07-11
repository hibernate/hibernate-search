/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.engine.logging.impl;

import java.util.List;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.util.FailureContext;
import org.hibernate.search.engine.logging.spi.MappableTypeModelFormatter;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@MessageLogger(projectCode = "HSEARCH")
public interface Log extends BasicLogger {

	@Message(id = 1, value = "Unable to convert configuration property '%1$s' with value '%2$s': %3$s")
	SearchException unableToConvertConfigurationProperty(String key, Object rawValue, String errorMessage, @Cause Exception cause);

	@Message(id = 2, value = "Invalid value: expected either an instance of '%1$s' or a parsable String.")
	SearchException invalidPropertyValue(Class<?> expectedType, @Cause Exception cause);

	@Message(id = 3, value = "Invalid boolean value: expected either a Boolean, the String 'true' or the String 'false'.")
	SearchException invalidBooleanPropertyValue(@Cause Exception cause);

	@Message(id = 4, value = "%1$s")
	SearchException invalidIntegerPropertyValue(String errorMessage, @Cause Exception cause);

	@Message(id = 5, value = "%1$s")
	SearchException invalidLongPropertyValue(String errorMessage, @Cause Exception cause);

	@Message(id = 6, value = "Invalid multi value: expected either a Collection or a String.")
	SearchException invalidMultiPropertyValue();

	@Message(id = 7, value = "Cannot add multiple predicates to the query root; use an explicit boolean predicate instead.")
	SearchException cannotAddMultiplePredicatesToQueryRoot();

	@Message(id = 9, value = "Cannot add multiple predicates to a nested predicate; use an explicit boolean predicate instead.")
	SearchException cannotAddMultiplePredicatesToNestedPredicate();

	@Message(id = 11, value = "Invalid value: the value to match in match predicates must be non-null." +
			" Null value was passed to match predicate on fields %1$s")
	SearchException matchPredicateCannotMatchNullValue(List<String> strings);

	@Message(id = 12, value = "Invalid value: at least one bound in range predicates must be non-null." +
			" Null bounds were passed to range predicate on fields %1$s")
	SearchException rangePredicateCannotMatchNullValue(List<String> strings);

	@Message(id = 13, value = "Cannot map type '%1$s' to index '%2$s', because this type is abstract."
			+ " Index mappings are not inherited: they apply to exact instances of a given type."
			+ " As a result, mapping an abstract type to an index does not make sense,"
			+ " since the index would always be empty.")
	SearchException cannotMapAbstractTypeToIndex(
			@FormatWith(MappableTypeModelFormatter.class) MappableTypeModel typeModel, String indexName);

	@Message(id = 14, value = "Field name '%1$s' is invalid: field names cannot be null or empty." )
	SearchException relativeFieldNameCannotBeNullOrEmpty(String relativeFieldName,
			@Param FailureContext context);

	@Message(id = 15, value = "Field name '%1$s' is invalid: field names cannot contain a dot ('.')."
			+ " Remove the dot from your field name,"
			+ " or if you are declaring the field in a bridge and want a tree of fields,"
			+ " declare an object field using the objectField() method." )
	SearchException relativeFieldNameCannotContainDot(String relativeFieldName,
			@Param FailureContext context);

	@Message(id = 16, value = "Invalid polygon: the first point '%1$s' should be identical to the last point '%2$s' to properly close the polygon." )
	IllegalArgumentException invalidGeoPolygonFirstPointNotIdenticalToLastPoint(GeoPoint firstPoint, GeoPoint lastPoint);

	@Message(id = 17, value = "Cannot add a predicate to this DSL context anymore, because the DSL context was already closed."
			+ " If you want to re-use predicates, do not re-use the predicate DSL context objects,"
			+ " but rather build SearchPredicate objects."
	)
	SearchException cannotAddPredicateToUsedContext();

	@Message(id = 18, value = "Cannot add a sort to this DSL context anymore, because the DSL context was already closed."
			+ " If you want to re-use sorts, do not re-use the sort DSL context objects,"
			+ " but rather build SearchSort objects."
	)
	SearchException cannotAddSortToUsedContext();

	@Message(id = 19, value = "Hibernate Search bootstrap failed; stopped collecting failures after '%2$s' failures."
			+ " Failures:\n%1$s"
	)
	SearchException boostrapCollectedFailureLimitReached(String renderedFailures, int failureCount);

	@Message(id = 20, value = "Hibernate Search bootstrap failed."
			+ " Failures:\n%1$s"
	)
	SearchException bootstrapCollectedFailures(String renderedFailures);

	@LogMessage(level = Logger.Level.ERROR)
	@Message(id = 21, value = "Hibernate Search bootstrap encountered a non-fatal failure;"
			+ " continuing bootstrap for now to list all mapping problems,"
			+ " but the bootstrap process will ultimately be aborted.\n"
			+ "Context: %1$s\n"
			+ "Failure:" // The stack trace follows
	)
	void newBootstrapCollectedFailure(String context, @Cause Throwable failure);

	@LogMessage(level = Logger.Level.DEBUG)
	@Message(id = 22, value = "Unexpected empty failure context; there is a bug in Hibernate Search, please report it")
	void unexpectedEmptyFailureContext(@Cause SearchException exceptionForStackTrace);
}
