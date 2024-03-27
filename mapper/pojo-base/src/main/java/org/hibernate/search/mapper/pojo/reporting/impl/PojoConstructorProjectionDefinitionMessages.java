/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.reporting.impl;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Message bundle for constructor projections in the POJO mapper.
 */
@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface PojoConstructorProjectionDefinitionMessages {

	PojoConstructorProjectionDefinitionMessages INSTANCE = Messages.getBundle(
			PojoConstructorProjectionDefinitionMessages.class
	);

	@Message(value = "Executed constructor path:")
	String executedConstructorPath();

}
