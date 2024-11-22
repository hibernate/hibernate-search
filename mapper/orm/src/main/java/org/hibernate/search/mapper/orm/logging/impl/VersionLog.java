/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.logging.impl;

import static org.hibernate.search.mapper.orm.logging.impl.OrmLog.ID_OFFSET_LEGACY_ENGINE;
import static org.jboss.logging.Logger.Level.INFO;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = VersionLog.CATEGORY_NAME,
		description = """
				Logs the version of Hibernate Search, when the Hibernate ORM mapper is used.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface VersionLog {
	String CATEGORY_NAME = "org.hibernate.search.version.orm";

	VersionLog INSTANCE = LoggerFactory.make( VersionLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	// -----------------------------------
	// Pre-existing messages from Search 5
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	@LogMessage(level = INFO)
	@Message(id = ID_OFFSET_LEGACY_ENGINE + 34, value = "Hibernate Search version %1$s")
	void version(String versionString);

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
}
