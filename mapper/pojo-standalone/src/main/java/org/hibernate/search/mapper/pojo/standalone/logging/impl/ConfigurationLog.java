/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.logging.impl;

import static org.hibernate.search.mapper.pojo.standalone.logging.impl.StandaloneMapperLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;
import java.util.List;

import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = ConfigurationLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface ConfigurationLog {
	String CATEGORY_NAME = "org.hibernate.search.configuration.mapper.standalone";

	ConfigurationLog INSTANCE = LoggerFactory.make( ConfigurationLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@Message(id = ID_OFFSET + 11,
			value = "Invalid String value for the bean provider: '%s'. The bean provider must be an instance of '%s'.")
	SearchException invalidStringForBeanProvider(String value, Class<BeanProvider> expectedType);

	@Message(id = ID_OFFSET + 15, value = "Invalid schema management strategy name: '%1$s'."
			+ " Valid names are: %2$s.")
	SearchException invalidSchemaManagementStrategyName(String invalidRepresentation,
			List<String> validRepresentations);
}
