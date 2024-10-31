/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.integrationtest.mapper.orm.logging.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.util.common.SearchException;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "HSEARCH")
public interface TestLog extends BasicLogger {
	String CATEGORY_NAME = "org.hibernate.search.test";

	TestLog TEST_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), TestLog.class, CATEGORY_NAME );

	@Message("Indexing failure: %1$s.\nThe following entities may not have been updated correctly in the index: %2$s.")
	SearchException indexingFailure(String causeMessage, List<?> failingEntities, @Cause Throwable cause);
}
