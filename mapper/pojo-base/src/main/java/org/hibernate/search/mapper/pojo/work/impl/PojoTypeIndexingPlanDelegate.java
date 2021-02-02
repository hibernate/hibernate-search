/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
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

	void add(I identifier, Supplier<E> entitySupplier, DocumentRouteDescriptor route);

	void addOrUpdate(I identifier, DocumentRoutesDescriptor routes, Supplier<E> entitySupplier);

	void delete(I identifier, DocumentRoutesDescriptor routes, Supplier<E> entitySupplier);

	void discard();

	<R> CompletableFuture<MultiEntityOperationExecutionReport<R>> executeAndReport(
			EntityReferenceFactory<R> entityReferenceFactory);

}
