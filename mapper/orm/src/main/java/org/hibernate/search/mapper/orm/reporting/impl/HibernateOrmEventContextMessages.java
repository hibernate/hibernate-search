/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.reporting.impl;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Message bundle for event contexts in the Hibernate ORM mapper.
 */
@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface HibernateOrmEventContextMessages {

	HibernateOrmEventContextMessages INSTANCE = Messages.getBundle( HibernateOrmEventContextMessages.class );

	@Message(value = "Hibernate ORM mapping")
	String mapping();

	@Message(value = "Schema management")
	String schemaManagement();
}
