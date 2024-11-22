/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.logging.impl;

import static org.hibernate.search.mapper.orm.logging.impl.OrmLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.Transaction;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = IndexingLog.CATEGORY_NAME,
		description = """
				Logs related to the indexing process that are Hibernate ORM mapper specific.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface IndexingLog {
	String CATEGORY_NAME = "org.hibernate.search.indexing.mapper.orm";

	IndexingLog INSTANCE = LoggerFactory.make( IndexingLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@Message(id = ID_OFFSET + 22,
			value = "Indexing failure: %1$s.\nThe following entities may not have been updated correctly in the index: %2$s.")
	SearchException indexingFailure(String causeMessage, List<?> failingEntities, @Cause Throwable cause);

	@LogMessage(level = Logger.Level.TRACE)
	@Message(id = ID_OFFSET + 135, value = "Processing Transaction's beforeCompletion() phase for %s.")
	void beforeCompletion(Transaction transactionIdentifier);

	@LogMessage(level = Logger.Level.TRACE)
	@Message(id = ID_OFFSET + 136, value = "Processing Transaction's afterCompletion() phase for %s. Executing indexing plan.")
	void afterCompletionExecuting(Transaction transactionIdentifier);

	@LogMessage(level = Logger.Level.TRACE)
	@Message(id = ID_OFFSET + 137,
			value = "Processing Transaction's afterCompletion() phase for %s. Cancelling indexing plan due to transaction status %d")
	void afterCompletionCanceling(Transaction transactionIdentifier, int status);
}
