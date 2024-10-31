/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.logging.impl;


import static org.jboss.logging.Logger.Level.TRACE;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;
import org.jboss.logging.annotations.ValidIdRanges;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRanges({
		@ValidIdRange(min = MessageConstants.ENGINE_ID_RANGE_MIN, max = MessageConstants.ENGINE_ID_RANGE_MAX),
})
public interface EngineLog
		extends BackendLog, BeanLog, CommonFailureLog, ConfigurationLog, ExecutorLog, FormattingLog, MappingLog,
		QueryLog {

	// -----------------------------------
	// Pre-existing messages from Search 5 (engine module)
	// DO NOT ADD ANY NEW MESSAGES HERE
	// -----------------------------------
	int ID_OFFSET_LEGACY = MessageConstants.ENGINE_ID_RANGE_MIN;

	// -----------------------------------
	// New messages from Search 6 onwards
	// -----------------------------------
	int ID_OFFSET = MessageConstants.ENGINE_ID_RANGE_MIN + 500;

	//	@Message(id = ID_OFFSET + 117,
	//			value = "Parameter with name '%1$s' was not defined on projection definition '%2$s'.")
	//	SearchException paramNotDefined(String paramName, ProjectionDefinition<?> definition);


	/**
	 * Only here as a way to track the highest "already used id".
	 * When adding a new exception or log message use this id and bump the one
	 * here to the next value.
	 */
	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 130, value = "")
	void nextLoggerIdForConvenience();
}
