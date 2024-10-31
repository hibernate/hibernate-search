/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.jakarta.batch.util.logging;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface TestLog extends BasicLogger {

	String CATEGORY_NAME = "org.hibernate.search.test";

	TestLog TEST_LOGGER = LoggerFactory.make( TestLog.class, CATEGORY_NAME, MethodHandles.lookup() );

}
