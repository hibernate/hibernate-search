/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.logging.impl;

import static org.jboss.logging.Logger.Level.TRACE;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.MAPPER_POJO_ID_RANGE_MIN, max = MessageConstants.MAPPER_POJO_ID_RANGE_MAX),
		// Exceptions for legacy messages from Search 5 (engine module)
		@ValidIdRange(min = 27, max = 28),
		@ValidIdRange(min = 30, max = 30),
		@ValidIdRange(min = 31, max = 31),
		@ValidIdRange(min = 62, max = 62),
		@ValidIdRange(min = 135, max = 135),
		@ValidIdRange(min = 159, max = 159),
		@ValidIdRange(min = 160, max = 160),
		@ValidIdRange(min = 177, max = 177),
		@ValidIdRange(min = 216, max = 216),
		@ValidIdRange(min = 221, max = 221),
		@ValidIdRange(min = 234, max = 234),
		@ValidIdRange(min = 235, max = 235),
		@ValidIdRange(min = 295, max = 295),
		@ValidIdRange(min = 297, max = 297),
		@ValidIdRange(min = 337, max = 337)
})
public interface PojoMapperLog
		extends CommonFailureLog, FormattingLog, IndexingLog, LoadingLog, MappingLog, MassIndexingLog, ProjectionLog,
		SchemaExportLog {

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_LEGACY_ENGINE = MessageConstants.ENGINE_ID_RANGE_MIN;

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET = MessageConstants.MAPPER_POJO_ID_RANGE_MIN;


	/**
	 * Only here as a way to track the highest "already used id".
	 * When adding a new exception or log message use this id and bump the one
	 * here to the next value.
	 */
	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 179, value = "")
	void nextLoggerIdForConvenience();

}
