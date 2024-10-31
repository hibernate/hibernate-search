/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.logging.impl;

import static org.hibernate.search.mapper.pojo.logging.impl.PojoMapperLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;
import java.net.URL;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = CommonFailureLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface CommonFailureLog {
	String CATEGORY_NAME = "org.hibernate.search.common.failures";

	CommonFailureLog INSTANCE = LoggerFactory.make( CommonFailureLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@Message(id = ID_OFFSET + 104, value = "Param with name '%1$s' has not been defined for the binder.")
	SearchException paramNotDefined(String name);

	@Message(id = ID_OFFSET + 119,
			value = "Exception while retrieving the Jandex index for code source location '%1$s': %2$s; %3$s")
	SearchException errorDiscoveringJandexIndex(URL codeSourceLocation, String causeMessage, String hint,
			@Cause Exception cause);

}
