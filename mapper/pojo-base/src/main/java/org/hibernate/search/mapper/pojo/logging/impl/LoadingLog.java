/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.logging.impl;

import static org.hibernate.search.mapper.pojo.logging.impl.PojoMapperLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;
import java.util.Collection;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = LoadingLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface LoadingLog {
	String CATEGORY_NAME = "org.hibernate.search.loading.mapper";

	LoadingLog INSTANCE = LoggerFactory.make( LoadingLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------

	@Message(id = ID_OFFSET + 86,
			value = "Unexpected entity name for entity loading: '%1$s'. Expected one of %2$s.")
	SearchException unexpectedEntityNameForEntityLoading(String entityName, Collection<String> expectedNames);
}
