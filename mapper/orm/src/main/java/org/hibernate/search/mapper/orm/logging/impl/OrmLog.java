/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.mapper.orm.logging.impl;

import static org.jboss.logging.Logger.Level.TRACE;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.MAPPER_ORM_ID_RANGE_MIN, max = MessageConstants.MAPPER_ORM_ID_RANGE_MAX),
		// Exceptions for legacy messages from Search 5 (engine module)
		@ValidIdRange(min = 34, max = 34),
		@ValidIdRange(min = 36, max = 36),
		@ValidIdRange(min = 39, max = 39),
		@ValidIdRange(min = 62, max = 62),
		@ValidIdRange(min = 116, max = 116),
		@ValidIdRange(min = 183, max = 183),
		@ValidIdRange(min = 211, max = 212),
		@ValidIdRange(min = 276, max = 276),
		@ValidIdRange(min = 348, max = 349)
})
public interface OrmLog extends ConfigurationLog, DeprecationLog, IndexingLog, LoadingLog, MappingLog, OrmMiscLog, VersionLog {

	// -----------------------------------
	// Pre-existing messages from Search 5
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_LEGACY_ENGINE = MessageConstants.ENGINE_ID_RANGE_MIN;

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET = MessageConstants.MAPPER_ORM_ID_RANGE_MIN;

	/**
	 * Only here as a way to track the highest "already used id".
	 * When adding a new exception or log message use this id and bump the one
	 * here to the next value.
	 */
	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 143, value = "")
	void nextLoggerIdForConvenience();
}
