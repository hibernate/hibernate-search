/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.logging.impl;

import static org.hibernate.search.mapper.orm.logging.impl.OrmLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = IndexingLog.CATEGORY_NAME,
		description = """
				Logged messages may notify when the particular indexing plan synchronization stage occurs (e.g. before/after transaction).
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface IndexingLog extends BasicLogger {
	String CATEGORY_NAME = "org.hibernate.search.mapper.indexing";

	IndexingLog INSTANCE = LoggerFactory.make( IndexingLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@Message(id = ID_OFFSET + 22,
			value = "Indexing failure: %1$s.\nThe following entities may not have been updated correctly in the index: %2$s.")
	SearchException indexingFailure(String causeMessage, List<?> failingEntities, @Cause Throwable cause);
}
