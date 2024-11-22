/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.logging.impl;

import static org.hibernate.search.mapper.pojo.standalone.logging.impl.StandaloneMapperLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = SessionLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface SessionLog {
	String CATEGORY_NAME = "org.hibernate.search.mapper.standalone.session";

	SessionLog INSTANCE = LoggerFactory.make( SessionLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@Message(id = ID_OFFSET + 14, value = "Unable to access search session: %1$s")
	SearchException hibernateSessionAccessError(String causeMessage);

}
