/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.engine.logging.impl;

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

	@Message(id = 7, value = "Cannot add multiple predicates to the query root.")
	SearchException cannotAddMultiplePredicatesToQueryRoot();

	@Message(id = 8, value = "Invalid parent object for this field accessor; expected path '%1$s', got '%2$s'.")
	SearchException invalidParentDocumentObjectState(String expectedPath, String actualPath);
}
