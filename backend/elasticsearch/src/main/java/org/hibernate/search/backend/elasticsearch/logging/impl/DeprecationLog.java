/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.logging.impl;

import static org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = DeprecationLog.CATEGORY_NAME,
		description = """
				Notifies about usage of deprecated configuration properties or configuration property values.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface DeprecationLog {
	String CATEGORY_NAME = "org.hibernate.search.deprecation";

	DeprecationLog INSTANCE = LoggerFactory.make( DeprecationLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@Message(id = ID_OFFSET + 98, value = "The lifecycle strategy cannot be set at the index level anymore."
			+ " Set the schema management strategy via the property 'hibernate.search.schema_management.strategy' instead.")
	SearchException lifecycleStrategyMovedToMapper();
}
