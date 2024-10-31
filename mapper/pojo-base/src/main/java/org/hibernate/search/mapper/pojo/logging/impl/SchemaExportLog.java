/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.logging.impl;

import static org.hibernate.search.mapper.pojo.logging.impl.PojoMapperLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;
import java.nio.file.Path;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;
import org.hibernate.search.util.common.reporting.EventContext;

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.Param;

@CategorizedLogger(
		category = SchemaExportLog.CATEGORY_NAME
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface SchemaExportLog {
	String CATEGORY_NAME = "org.hibernate.search.mapper.export";

	SchemaExportLog INSTANCE = LoggerFactory.make( SchemaExportLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@Message(id = ID_OFFSET + 126,
			value = "Target path '%1$s' already exists and is not an empty directory. Use a path to an empty or non-existing directory.")
	SearchException schemaExporterTargetIsNotEmptyDirectory(Path targetDirectory);

	@Message(id = ID_OFFSET + 127, value = "Unable to export the schema: %1$s")
	SearchException unableToExportSchema(String cause, @Cause Exception e, @Param EventContext context);
}
