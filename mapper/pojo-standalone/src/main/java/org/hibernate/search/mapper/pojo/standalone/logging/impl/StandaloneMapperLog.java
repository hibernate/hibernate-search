/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.logging.impl;

import static org.jboss.logging.Logger.Level.TRACE;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
@ValidIdRange(min = MessageConstants.MAPPER_POJO_STANDALONE_ID_RANGE_MIN,
		max = MessageConstants.MAPPER_POJO_STANDALONE_ID_RANGE_MAX)
public interface StandaloneMapperLog extends ConfigurationLog, MappingLog, SessionLog {

	int ID_OFFSET = MessageConstants.MAPPER_POJO_STANDALONE_ID_RANGE_MIN;

	/**
	 * Only here as a way to track the highest "already used id".
	 * When adding a new exception or log message use this id and bump the one
	 * here to the next value.
	 */
	@LogMessage(level = TRACE)
	@Message(id = ID_OFFSET + 19, value = "")
	void nextLoggerIdForConvenience();

}