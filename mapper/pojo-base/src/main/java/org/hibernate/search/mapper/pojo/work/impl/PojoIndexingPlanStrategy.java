/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * A strategy implementing the behavior of {@link org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan}.
 */
interface PojoIndexingPlanStrategy {

	boolean shouldResolveDirtyForDeleteOnly();

	CompletableFuture<MultiEntityOperationExecutionReport> doExecuteAndReport(
			Collection<PojoIndexedTypeIndexingPlan<?, ?>> indexedTypeDelegates,
			PojoLoadingPlanProvider loadingPlanProvider,
			OperationSubmitter operationSubmitter);

	void doDiscard(Collection<PojoIndexedTypeIndexingPlan<?, ?>> indexedTypeDelegates);

	<I, E> PojoIndexedTypeIndexingPlan<I, E> createIndexedDelegate(PojoWorkIndexedTypeContext<I, E> typeContext,
			PojoWorkSessionContext sessionContext, PojoIndexingPlanImpl root);

	<I, E> PojoContainedTypeIndexingPlan<I, E> createDelegate(PojoWorkContainedTypeContext<I, E> typeContext,
			PojoWorkSessionContext sessionContext, PojoIndexingPlanImpl root);
}
