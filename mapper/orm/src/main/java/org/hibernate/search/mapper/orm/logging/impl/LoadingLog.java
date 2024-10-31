/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.logging.impl;


import static org.hibernate.search.mapper.orm.logging.impl.OrmLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = LoadingLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface LoadingLog extends BasicLogger {
	String CATEGORY_NAME = "org.hibernate.search.mapper.loading";

	LoadingLog INSTANCE = LoggerFactory.make( LoadingLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------

	@Message(id = ID_OFFSET + 40, value = "Multiple instances of entity type '%1$s' have their property '%2$s' set to '%3$s'."
			+ " '%2$s' is the document ID and must be assigned unique values.")
	SearchException foundMultipleEntitiesForDocumentId(String entityName, String documentIdSourcePropertyName,
			Object id);
}
