/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.logging.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = DeprecationLog.CATEGORY_NAME,
		description = """
				Logs related to the usage of deprecated configuration properties
				or configuration property values specific to the Hibernate ORM mapper.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface DeprecationLog {
	String CATEGORY_NAME = "org.hibernate.search.deprecation.mapper.orm";

	DeprecationLog INSTANCE = LoggerFactory.make( DeprecationLog.class, CATEGORY_NAME, MethodHandles.lookup() );

}
