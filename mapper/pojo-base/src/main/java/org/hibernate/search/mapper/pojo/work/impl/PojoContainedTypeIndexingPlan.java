/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.work.impl;

import java.lang.invoke.MethodHandles;
import java.util.function.Supplier;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkSessionContext;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * @param <E> The contained entity type.
 */
public class PojoContainedTypeIndexingPlan<E>
		extends AbstractPojoTypeIndexingPlan<Object, E, PojoContainedTypeIndexingPlan<E>.ContainedEntityState> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoWorkContainedTypeContext<E> typeContext;

	public PojoContainedTypeIndexingPlan(PojoWorkContainedTypeContext<E> typeContext,
			PojoWorkSessionContext<?> sessionContext) {
		super( sessionContext );
		this.typeContext = typeContext;
	}

	@Override
	void purge(Object providedId, String providedRoutingKey) {
		throw log.cannotPurgeNonIndexedContainedType( typeContext.typeIdentifier(), providedId );
	}

	@Override
	PojoWorkContainedTypeContext<E> typeContext() {
		return typeContext;
	}

	@Override
	Object toIdentifier(Object providedId, Supplier<E> entitySupplier) {
		return providedId;
	}

	@Override
	protected ContainedEntityState createState(Object identifier) {
		return new ContainedEntityState( identifier );
	}

	class ContainedEntityState
			extends AbstractPojoTypeIndexingPlan<Object, E, ContainedEntityState>.AbstractEntityState {
		private ContainedEntityState(Object identifier) {
			super( identifier );
		}
	}

}
