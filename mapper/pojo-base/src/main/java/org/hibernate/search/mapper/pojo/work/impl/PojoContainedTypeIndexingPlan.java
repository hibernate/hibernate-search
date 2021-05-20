/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

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
			PojoWorkSessionContext sessionContext) {
		super( sessionContext );
		this.typeContext = typeContext;
	}

	@Override
	PojoWorkContainedTypeContext<I, E> typeContext() {
		return typeContext;
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
			// The routes don't make sense for contained types.
			// Ignore non-null values, for backwards compatibility.
		}
	}

}
