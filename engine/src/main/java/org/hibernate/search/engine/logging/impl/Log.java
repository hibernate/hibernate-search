/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.engine.logging.impl;

import java.util.List;

import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.util.SearchException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

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
	SearchException cannotMapAbstractTypeToIndex(MappableTypeModel typeModel, String indexName);

	@Message(id = 14, value = "Field name '%1$s' is invalid: field names cannot be null or empty." )
	SearchException relativeFieldNameCannotBeNullOrEmpty(String relativeFieldName);

	@Message(id = 15, value = "Field name '%1$s' is invalid: field names cannot contain a dot ('.')."
			+ " Remove the dot from your field name,"
			+ " or if you are declaring the field in a bridge and want a tree of fields,"
			+ " declare an object field using the objectField() method." )
	SearchException relativeFieldNameCannotContainDot(String relativeFieldName);
}
