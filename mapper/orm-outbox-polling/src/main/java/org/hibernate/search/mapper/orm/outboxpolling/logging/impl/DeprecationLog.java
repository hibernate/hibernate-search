/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.outboxpolling.logging.impl;

import static org.hibernate.search.mapper.orm.outboxpolling.logging.impl.OutboxPollingLog.ID_OFFSET;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = DeprecationLog.CATEGORY_NAME,
		description = """
				Logs related to the usage of deprecated configuration properties
				or configuration property values specific to the outbox polling.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface DeprecationLog {
	String CATEGORY_NAME = "org.hibernate.search.deprecation.mapper.orm.outboxpolling";

	DeprecationLog INSTANCE = LoggerFactory.make( DeprecationLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET + 37,
			value = "Configuration property '%1$s' is configured with a deprecated value '%2$s'. "
					+ "Use '%3$s' instead.")
	void usingDeprecatedPropertyValue(String property, String value, String correctValue);
}
