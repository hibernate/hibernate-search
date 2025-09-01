/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.backend.elasticsearch.client.common.logging.spi;

import static org.jboss.logging.Logger.Level.TRACE;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.BACKEND_ES_CLIENT_ID_RANGE_MIN,
				max = MessageConstants.BACKEND_ES_CLIENT_ID_RANGE_MAX),
		// Exceptions for legacy messages from Search 5 (engine module)
		@ValidIdRange(min = 35, max = 35),
})
public interface ElasticsearchClientCommonLog
		extends ElasticsearchRequestLog, ElasticsearchClientLog {

	// -----------------------------------
	// Pre-existing messages from Search 5 (ES module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_LEGACY_ES = MessageConstants.BACKEND_ES_ID_RANGE_MIN;

	// -----------------------------------
	// New (old) messages from Search 6 onwards up until Search 8.2
	// -----------------------------------
	int ID_BACKEND_OFFSET = MessageConstants.BACKEND_ES_ID_RANGE_MIN + 500;

	// -----------------------------------
	// New messages from Search 8.2 onwards
	// -----------------------------------
	int ID_OFFSET = MessageConstants.BACKEND_ES_CLIENT_ID_RANGE_MIN;

	/**
	 * Only here as a way to track the highest "already used id".
	 * When adding a new exception or log message use this id and bump the one
	 * here to the next value.
	 */
	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 1, value = "")
	void nextLoggerIdForConvenience();
}
