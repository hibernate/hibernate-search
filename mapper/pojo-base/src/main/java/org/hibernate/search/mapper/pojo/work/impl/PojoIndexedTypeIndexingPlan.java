/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.bridge.runtime.impl.DocumentRouter;
import org.hibernate.search.mapper.pojo.route.DocumentRouteDescriptor;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * @param <I> The identifier type for the mapped entity type.
 * @param <E> The entity type mapped to the index.
 */
public class PojoIndexedTypeIndexingPlan<I, E>
		extends AbstractPojoTypeIndexingPlan<I, E, PojoIndexedTypeIndexingPlan<I, E>.IndexedEntityState> {

	private final PojoWorkIndexedTypeContext<I, E> typeContext;

	public PojoIndexedTypeIndexingPlan(PojoWorkIndexedTypeContext<I, E> typeContext,
			PojoWorkSessionContext sessionContext, PojoIndexingPlanImpl root,
			PojoTypeIndexingPlanDelegate<I, E> delegate) {
		super( sessionContext, root, delegate );
		this.typeContext = typeContext;
	}

	@Override
	void updateBecauseOfContained(Object entity) {
		Supplier<E> entitySupplier = typeContext.toEntitySupplier( sessionContext, entity );
		I identifier = typeContext.identifierMapping().getIdentifier( null, entitySupplier );
		getState( identifier ).updateBecauseOfContained( entitySupplier );
	}

	@Override
	void resolveDirty(boolean deleteOnly) {
		// We need to iterate on a "frozen snapshot" of the states because of HSEARCH-3857
		List<IndexedEntityState> frozenIndexingPlansPerId = new ArrayList<>( statesPerId.values() );
		for ( IndexedEntityState state : frozenIndexingPlansPerId ) {
			state.resolveDirty( deleteOnly );
		}
	}

	@Override
	PojoWorkIndexedTypeContext<I, E> typeContext() {
		return typeContext;
	}

	@Override
	DocumentRouter<? super E> router() {
		return typeContext.router();
	}

	@Override
	protected IndexedEntityState createState(I identifier) {
		return new IndexedEntityState( identifier );
	}

	class IndexedEntityState
			extends AbstractPojoTypeIndexingPlan<I, E, IndexedEntityState>.AbstractEntityState {

		private DocumentRoutesDescriptor providedRoutes;

		private IndexedEntityState(I identifier) {
			super( identifier );
		}

		@Override
		void providedRoutes(DocumentRoutesDescriptor routes) {
			if ( routes == null ) {
				return;
			}
			if ( this.providedRoutes == null ) {
				this.providedRoutes = routes;
			}
			else {
				Set<DocumentRouteDescriptor> mergedPrevious = new LinkedHashSet<>(
						this.providedRoutes.previousRoutes() );
				mergedPrevious.addAll( routes.previousRoutes() );
				this.providedRoutes = DocumentRoutesDescriptor.of( routes.currentRoute(), mergedPrevious );
			}
		}

		@Override
		DocumentRoutesDescriptor providedRoutes() {
			return providedRoutes;
		}
	}

}
