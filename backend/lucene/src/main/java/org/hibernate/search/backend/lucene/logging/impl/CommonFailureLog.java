/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.logging.impl;

import static org.hibernate.search.backend.lucene.logging.impl.LuceneLog.ID_OFFSET;
import static org.hibernate.search.backend.lucene.logging.impl.LuceneLog.ID_OFFSET_LEGACY_ENGINE;

import java.lang.invoke.MethodHandles;

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
		category = CommonFailureLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface CommonFailureLog {
	String CATEGORY_NAME = "org.hibernate.search.common.failures";

	CommonFailureLog INSTANCE = LoggerFactory.make( CommonFailureLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------

	@Message(id = ID_OFFSET_LEGACY_ENGINE + 114,
			value = "Resource does not exist in classpath: '%1$s'")
	SearchException unableToLoadResource(String fileName);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------

	@Message(id = ID_OFFSET + 5,
			value = "Invalid target for Lucene extension: '%1$s'."
					+ " This extension can only be applied to components created by a Lucene backend.")
	SearchException luceneExtensionOnUnknownType(Object context);

	@Message(id = ID_OFFSET + 33,
			value = "Invalid requested type for this backend: '%1$s'."
					+ " Lucene backends can only be unwrapped to '%2$s'.")
	SearchException backendUnwrappingWithUnknownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass,
			@Param EventContext context);

	@Message(id = ID_OFFSET + 51,
			value = "Invalid requested type for this index manager: '%1$s'."
					+ " Lucene index managers can only be unwrapped to '%2$s'.")
	SearchException indexManagerUnwrappingWithUnknownType(@FormatWith(ClassFormatter.class) Class<?> requestedClass,
			@FormatWith(ClassFormatter.class) Class<?> actualClass, @Param EventContext context);

	@Message(id = ID_OFFSET + 156, value = "Nonblocking operation submitter is not supported.")
	SearchException nonblockingOperationSubmitterNotSupported();
}
