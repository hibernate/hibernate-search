/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.util.impl.test.logging.impl;

import java.lang.invoke.MethodHandles;
import java.util.Locale;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "HSEARCH")
public interface TestLog extends BasicLogger {
	String CATEGORY_NAME = "org.hibernate.search.test";

	TestLog TEST_LOGGER = Logger.getMessageLogger( MethodHandles.lookup(), TestLog.class, CATEGORY_NAME, Locale.ROOT );

}
