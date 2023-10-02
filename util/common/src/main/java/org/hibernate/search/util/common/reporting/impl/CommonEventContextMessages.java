/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.reporting.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Common message bundle related to event contexts.
 */
@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface CommonEventContextMessages {

	CommonEventContextMessages INSTANCE = Messages.getBundle( MethodHandles.lookup(), CommonEventContextMessages.class );

	@Message(value = "Context: ")
	String contextPrefix();

	@Message(value = ", ")
	String contextSeparator();

}
