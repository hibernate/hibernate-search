/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.logging.impl;

import static org.hibernate.search.metamodel.processor.logging.impl.ProcessorLog.ID_OFFSET;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.CategorizedLogger;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@CategorizedLogger(
		category = MappingLog.CATEGORY_NAME,
		description = """
				Logs related to creating Hibernate Search mapping.
				"""
)
@MessageLogger(projectCode = MessageConstants.PROJECT_CODE)
public interface MappingLog {
	String CATEGORY_NAME = "org.hibernate.search.mapping.mapper";

	MappingLog INSTANCE = LoggerFactory.make( MappingLog.class, CATEGORY_NAME, MethodHandles.lookup() );

	@Message(id = ID_OFFSET + 1,
			value = "Unexpected extractor references:"
					+ " extractors cannot be defined explicitly when extract = ContainerExtract.NO."
					+ " Either leave 'extract' to its default value to define extractors explicitly"
					+ " or leave the 'extractor' list to its default, empty value to disable extraction."
	)
	SearchException cannotReferenceExtractorsWhenExtractionDisabled();

}
