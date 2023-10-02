/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.mapper.mapping.spi;

import java.util.Set;

import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.mapper.scope.spi.MappedIndexScopeBuilder;

/**
 * The object responsible for applying works and searches to a full-text index.
 * <p>
 * This is the interface provided to mappers to access the index manager.
 */
public interface MappedIndexManager {

	IndexManager toAPI();

	IndexSchemaManager schemaManager();

	IndexIndexingPlan createIndexingPlan(BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

	IndexIndexer createIndexer(BackendSessionContext sessionContext);

	IndexWorkspace createWorkspace(BackendMappingContext mappingContext, Set<String> tenantId);

	<R, E> MappedIndexScopeBuilder<R, E> createScopeBuilder(BackendMappingContext mappingContext);

	void addTo(MappedIndexScopeBuilder<?, ?> builder);
}
