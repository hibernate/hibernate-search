/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.BitSet;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;

/**
 * A delegate for {@link AbstractPojoTypeIndexingPlan},
 * abstracting the processing of indexing events.
 *
 * @param <I> The type of identifiers of entities in this plan.
 * @param <E> The type of entities in this plan.
 *
 * @see PojoTypeIndexingPlanEventQueueDelegate
 * @see PojoTypeIndexingPlanIndexDelegate
 */
interface PojoTypeIndexingPlanDelegate<I, E> {

	boolean isDirtyForAddOrUpdate(boolean forceSelfDirty, boolean forceContainingDirty, BitSet dirtyPathsOrNull);

	void add(I identifier, DocumentRouteDescriptor route, Supplier<E> entitySupplier);

	void addOrUpdate(I identifier, DocumentRoutesDescriptor routes, Supplier<E> entitySupplier,
			boolean forceSelfDirty, boolean forceContainingDirty, BitSet dirtyPaths,
			boolean updatedBecauseOfContained, boolean updateBecauseOfDirty);

	void delete(I identifier, DocumentRoutesDescriptor routes, Supplier<E> entitySupplier);

	void discard();

	CompletableFuture<MultiEntityOperationExecutionReport> executeAndReport(OperationSubmitter operationSubmitter);

}
