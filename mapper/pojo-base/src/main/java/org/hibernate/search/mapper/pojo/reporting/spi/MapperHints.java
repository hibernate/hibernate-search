/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.reporting.spi;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface MapperHints {

	MapperHints NONE = Messages.getBundle( MethodHandles.lookup(), MapperHints.class );

	@Message("")
	String cannotReadJandexRootMapping();

}
