/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.DocumentRouter;
import org.hibernate.search.mapper.pojo.model.path.spi.PojoPathFilter;
import org.hibernate.search.mapper.pojo.processing.spi.PojoIndexingProcessorRootContext;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to the index.
 */
public interface PojoWorkIndexedTypeContext<I, E> extends PojoWorkTypeContext<I, E> {

	DocumentRouter<? super E> router();

	PojoDocumentContributor<E> toDocumentContributor(PojoWorkSessionContext sessionContext,
			PojoIndexingProcessorRootContext processorContext,
			I identifier, Supplier<E> entitySupplier);

	PojoPathFilter dirtySelfFilter();

	IndexIndexingPlan createIndexingPlan(PojoWorkSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy);

	IndexIndexer createIndexer(PojoWorkSessionContext sessionContext);

	IndexWorkspace createWorkspace(BackendMappingContext mappingContext, Set<String> tenantIds);

}
