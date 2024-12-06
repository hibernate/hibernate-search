/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.logging.impl;

import static org.hibernate.search.backend.lucene.logging.impl.LuceneLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.ClassFormatter;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.FormatWith;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@CategorizedLogger(
		category = IndexingLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface IndexingLog {
	String CATEGORY_NAME = "org.hibernate.search.indexing.lucene";

	IndexingLog INSTANCE = LoggerFactory.make( IndexingLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------


	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	@Message(id = ID_OFFSET + 16,
			value = "Unable to index entity of type '%2$s' with identifier '%3$s' and tenant identifier '%1$s': %4$s")
	SearchException unableToIndexEntry(String tenantId, String entityTypeName, Object entityIdentifier,
			String causeMessage, @Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 17,
			value = "Unable to delete entity of type '%2$s' with identifier '%3$s' and tenant identifier '%1$s': %4$s")
	SearchException unableToDeleteEntryFromIndex(String tenantId, String entityTypeName, Object entityIdentifier,
			String causeMessage, @Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 19,
			value = "Unable to commit: %1$s")
	SearchException unableToCommitIndex(String causeMessage, @Param EventContext context, @Cause Exception cause);

	@Message(id = ID_OFFSET + 39,
			value = "Invalid field reference for this document element:"
					+ " this document element has path '%1$s', but the referenced field has a parent with path '%2$s'.")
	SearchException invalidFieldForDocumentElement(String expectedPath, String actualPath);

	@Message(id = ID_OFFSET + 49,
			value = "Invalid field path; expected path '%1$s', got '%2$s'.")
	SearchException invalidFieldPath(String expectedPath, String actualPath);

	@Message(id = ID_OFFSET + 50,
			value = "Unable to convert DSL argument: %1$s")
	SearchException cannotConvertDslParameter(String errorMessage, @Cause Exception cause, @Param EventContext context);

	@Message(id = ID_OFFSET + 74,
			value = "Multiple values assigned to field '%1$s': this field is single-valued."
					+ " Declare the field as multi-valued in order to allow this.")
	SearchException multipleValuesForSingleValuedField(String absoluteFieldPath);

	@Message(id = ID_OFFSET + 81,
			value = "Unable to encode value '%1$s': this field type only supports values ranging from '%2$s' to '%3$s'."
					+ " If you want to encode values that are outside this range, change the decimal scale for this field."
					+ " Do not forget to reindex all your data after changing the decimal scale.")
	SearchException scaledNumberTooLarge(Number value, Number min, Number max);

	@Message(id = ID_OFFSET + 126,
			value = "Invalid value type. This field's values are of type '%1$s', which is not assignable from '%2$s'.")
	SearchException invalidFieldValueType(@FormatWith(ClassFormatter.class) Class<?> fieldValueType,
			@FormatWith(ClassFormatter.class) Class<?> invalidValueType,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 127,
			value = "Unknown field '%1$s'.")
	SearchException unknownFieldForIndexing(String absoluteFieldPath, @Param EventContext context);

	@Message(id = ID_OFFSET + 184, value = "The cosine vector similarity cannot process vectors with 0 magnitude. "
			+ "The vector violating this constraint is %1$s.")
	SearchException vectorCosineZeroMagnitudeNotAcceptable(Object vector);

	@Message(id = ID_OFFSET + 185, value = "The dot product vector similarity cannot process non-unit magnitude vectors. "
			+ "The vector violating this constraint is %1$s.")
	SearchException vectorDotProductNonUnitMagnitudeNotAcceptable(Object vector);
}
