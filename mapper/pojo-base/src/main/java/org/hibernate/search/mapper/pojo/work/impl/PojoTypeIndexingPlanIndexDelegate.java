/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * A {@link PojoTypeIndexingPlanDelegate} that sends indexing events to the index,
 * to process them locally.
 *
 * @param <I> The type of identifiers of entities in this plan.
 * @param <E> The type of entities in this plan.
 */
final class PojoTypeIndexingPlanIndexDelegate<I, E> implements PojoTypeIndexingPlanDelegate<I, E> {

	private final PojoWorkIndexedTypeContext<I, E> typeContext;
	private final PojoWorkSessionContext sessionContext;
	private final IndexIndexingPlan indexPlan;

	PojoTypeIndexingPlanIndexDelegate(PojoWorkIndexedTypeContext<I, E> typeContext,
			PojoWorkSessionContext sessionContext, IndexIndexingPlan indexPlan) {
		this.typeContext = typeContext;
		this.sessionContext = sessionContext;
		this.indexPlan = indexPlan;
	}

	@Override
	public void add(I identifier, DocumentRouteDescriptor route, Supplier<E> entitySupplier) {
		String documentIdentifier = typeContext.toDocumentIdentifier( sessionContext, identifier );
		DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
				route.routingKey(), identifier );
		indexPlan.add( referenceProvider,
				typeContext.toDocumentContributor( sessionContext, identifier, entitySupplier ) );
	}

	@Override
	public void addOrUpdate(I identifier, DocumentRoutesDescriptor routes, Supplier<E> entitySupplier) {
		String documentIdentifier = typeContext.toDocumentIdentifier( sessionContext, identifier );
		delegateDeletePrevious( identifier, documentIdentifier, routes.previousRoutes() );
		if ( routes.currentRoute() == null ) {
			// The routing bridge decided the entity should not be indexed.
			// We should have deleted it using the "previous routes" (if it was actually indexed previously),
			// and we don't have anything else to do.
			return;
		}
		DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
				routes.currentRoute().routingKey(), identifier );
		indexPlan.addOrUpdate( referenceProvider,
				typeContext.toDocumentContributor( sessionContext, identifier, entitySupplier ) );
	}

	@Override
	public void delete(I identifier, DocumentRoutesDescriptor routes, Supplier<E> entitySupplier) {
		String documentIdentifier = typeContext.toDocumentIdentifier( sessionContext, identifier );
		delegateDeletePrevious( identifier, documentIdentifier, routes.previousRoutes() );
		if ( routes.currentRoute() == null ) {
			// The routing bridge decided the entity should not be indexed.
			// We should have deleted it using the "previous routes" (if it was actually indexed previously),
			// and we don't have anything else to do.
			return;
		}
		DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
				routes.currentRoute().routingKey(), identifier );
		indexPlan.delete( referenceProvider );
	}

	@Override
	public void discard() {
		indexPlan.discard();
	}

	@Override
	public <R> CompletableFuture<MultiEntityOperationExecutionReport<R>> executeAndReport(
			EntityReferenceFactory<R> entityReferenceFactory) {
		return indexPlan.executeAndReport( entityReferenceFactory );
	}

	private void delegateDeletePrevious(I identifier, String documentIdentifier,
			Collection<DocumentRouteDescriptor> previousRoutes) {
		for ( DocumentRouteDescriptor route : previousRoutes ) {
			DocumentReferenceProvider referenceProvider = new PojoDocumentReferenceProvider( documentIdentifier,
					route.routingKey(), identifier );
			indexPlan.delete( referenceProvider );
		}
	}

}
