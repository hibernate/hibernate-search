/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.mapper.pojo.session.spi.PojoSearchManagerDelegate;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchTargetDelegate;
import org.hibernate.search.mapper.pojo.session.context.spi.PojoSessionContextImplementor;
import org.hibernate.search.util.impl.common.LoggerFactory;

class PojoSearchManagerDelegateImpl implements PojoSearchManagerDelegate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoIndexedTypeManagerContainer indexedTypeManagers;
	private final PojoContainedTypeManagerContainer containedTypeManagers;
	private final PojoSessionContextImplementor sessionContext;

	PojoSearchManagerDelegateImpl(PojoIndexedTypeManagerContainer indexedTypeManagers,
			PojoContainedTypeManagerContainer containedTypeManagers,
			PojoSessionContextImplementor sessionContext) {
		this.indexedTypeManagers = indexedTypeManagers;
		this.containedTypeManagers = containedTypeManagers;
		this.sessionContext = sessionContext;
	}

	@Override
	public <E, O> PojoSearchTargetDelegate<E, O> createPojoSearchTarget(
			Collection<? extends Class<? extends E>> targetedTypes) {
		if ( targetedTypes.isEmpty() ) {
			throw log.cannotSearchOnEmptyTarget();
		}

		Set<PojoIndexedTypeManager<?, ? extends E, ?>> targetedTypeManagers = new LinkedHashSet<>();
		for ( Class<? extends E> targetedType : targetedTypes ) {
			targetedTypeManagers.addAll(
					indexedTypeManagers.getAllBySuperClass( targetedType )
							.orElseThrow( () -> log.notIndexedType( targetedType ) )
			);
		}

		return new PojoSearchTargetDelegateImpl<>( indexedTypeManagers, targetedTypeManagers, sessionContext );
	}

	@Override
	public PojoWorkPlan createWorkPlan() {
		return new PojoWorkPlanImpl( indexedTypeManagers, containedTypeManagers, sessionContext );
	}

}
