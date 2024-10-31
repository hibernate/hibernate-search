/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.logging.impl;

import static org.hibernate.search.mapper.pojo.logging.impl.PojoMapperLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = CommonFailureLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface CommonFailureLog {
	String CATEGORY_NAME = "org.hibernate.search.common.failures";

	CommonFailureLog INSTANCE = LoggerFactory.make( CommonFailureLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@LogMessage(level = Logger.Level.ERROR)
	@Message(id = ID_OFFSET + 35, value = "Unable to shut down Hibernate Search:")
	void shutdownFailed(@Cause Throwable cause);

}
