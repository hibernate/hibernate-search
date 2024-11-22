/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.logging.impl;

import static org.hibernate.search.mapper.orm.logging.impl.OrmLog.ID_OFFSET;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.SearchException;
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
				or configuration property values specific to the Hibernate ORM mapper.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface DeprecationLog {
	String CATEGORY_NAME = "org.hibernate.search.deprecation.mapper.orm";

	DeprecationLog INSTANCE = LoggerFactory.make( DeprecationLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET + 53, value = "Configuration property '%1$s' is deprecated; use '%2$s' instead.")
	void deprecatedPropertyUsedInsteadOfNew(String resolveOrRaw, String resolveOrRaw1);

	@Message(id = ID_OFFSET + 122,
			value = "Both '%1$s' and '%2$s' are configured. Use only '%1$s' to set the indexing plan synchronization strategy. ")
	SearchException bothNewAndOldConfigurationPropertiesForIndexingPlanSyncAreUsed(String key1, String key2);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET + 123, value = "Configuration property '%1$s' is deprecated; use '%2$s' instead.")
	void automaticIndexingSynchronizationStrategyIsDeprecated(String deprecatedProperty, String newProperty);

	@LogMessage(level = WARN)
	@Message(id = ID_OFFSET + 125, value = "Configuration property '%1$s' is deprecated. "
			+ "This setting will be removed in a future version. "
			+ "There will be no alternative provided to replace it. "
			+ "After the removal of this property in a future version, "
			+ "a dirty check will always be performed when considering whether to trigger reindexing.")
	void automaticIndexingEnableDirtyCheckIsDeprecated(String deprecatedProperty);

	@Message(id = ID_OFFSET + 126,
			value = "Both '%1$s' and '%2$s' are configured. Use only '%2$s' to enable indexing listeners. ")
	SearchException bothNewAndOldConfigurationPropertiesForIndexingListenersAreUsed(String key1, String key2);
}
