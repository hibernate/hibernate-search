/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import org.hibernate.search.mapper.pojo.bridge.runtime.impl.DocumentRouter;
import org.hibernate.search.mapper.pojo.bridge.runtime.impl.NoOpDocumentRouter;
import org.hibernate.search.mapper.pojo.route.DocumentRoutesDescriptor;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;

/**
 * @param <I> The identifier type for the contained entity type.
 * @param <E> The contained entity type.
 */
public class PojoContainedTypeIndexingPlan<I, E>
		extends AbstractPojoTypeIndexingPlan<I, E, PojoContainedTypeIndexingPlan<I, E>.ContainedEntityState> {

	private final PojoWorkContainedTypeContext<I, E> typeContext;

	public PojoContainedTypeIndexingPlan(PojoWorkContainedTypeContext<I, E> typeContext,
			PojoWorkSessionContext sessionContext, PojoTypeIndexingPlanDelegate<I, E> delegate) {
		super( sessionContext, delegate );
		this.typeContext = typeContext;
	}

	@Override
	PojoWorkContainedTypeContext<I, E> typeContext() {
		return typeContext;
	}

	@Override
	DocumentRouter<? super E> router() {
		// The routes don't make sense for contained types, because they aren't indexed.
		return NoOpDocumentRouter.INSTANCE;
	}

	@Override
	protected ContainedEntityState createState(I identifier) {
		return new ContainedEntityState( identifier );
	}

	class ContainedEntityState
			extends AbstractPojoTypeIndexingPlan<I, E, ContainedEntityState>.AbstractEntityState {
		private ContainedEntityState(I identifier) {
			super( identifier );
		}

		@Override
		void providedRoutes(DocumentRoutesDescriptor routes) {
			// The routes don't make sense for contained types, because they aren't indexed.
			// Ignore non-null values, for backwards compatibility.
		}

		@Override
		DocumentRoutesDescriptor providedRoutes() {
			// The routes don't make sense for contained types, because they aren't indexed.
			return null;
		}

		@Override
		void delegateAdd(PojoLoadingPlanProvider loadingPlanProvider) {
			// No event when a contained entity is created:
			// it does not affect any other entity unless they are modified to refer to that contained entity,
			// in which case they get an event of their own.
		}

		@Override
		void delegateDelete() {
			// No event when a contained entity is deleted:
			// if other entities used to refer to that contained entity,
			// they should be updated to not refer to it anymore,
			// in which case they get an event of their own.
		}
	}

}
