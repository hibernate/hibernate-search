/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.reporting.spi;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface BackendMappingHints {

	BackendMappingHints NONE = Messages.getBundle( BackendMappingHints.class );

	@Message(value = "")
	String noEntityProjectionAvailable();

	@Message("")
	String missingDecimalScale();

	@Message("")
	String missingVectorDimension();

}
