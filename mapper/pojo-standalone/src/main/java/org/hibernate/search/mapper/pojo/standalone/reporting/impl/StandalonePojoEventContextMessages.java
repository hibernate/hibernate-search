/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.reporting.impl;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Message bundle for event contexts in the Standalone POJO mapper.
 */
@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface StandalonePojoEventContextMessages {

	StandalonePojoEventContextMessages INSTANCE = Messages.getBundle( StandalonePojoEventContextMessages.class );

	@Message(value = "Standalone POJO mapping")
	String mapping();

	@Message(value = "Schema management")
	String schemaManagement();

}
