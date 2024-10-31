/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.reporting.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.util.common.logging.impl.MessageConstants;

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;

/**
 * Message bundle for mass indexer in the POJO mapper.
 */
@MessageBundle(projectCode = MessageConstants.PROJECT_CODE)
public interface PojoMassIndexerMessages {

	PojoMassIndexerMessages INSTANCE = Messages.getBundle( MethodHandles.lookup(), PojoMassIndexerMessages.class );

	@Message(value = "MassIndexer operation")
	String massIndexerOperation();

	@Message(value = "Indexing instance of entity '%s' during mass indexing")
	String massIndexerIndexingInstance(String entityName);

	@Message(value = "Fetching identifiers of entities to index for entity '%s' during mass indexing")
	String massIndexerFetchingIds(String entityName);

	@Message(value = "Loading and extracting entity data for entity '%s' during mass indexing")
	String massIndexingLoadingAndExtractingEntityData(String entityName);

	@Message(value = "Background indexing of entities")
	String backgroundIndexing();
}
