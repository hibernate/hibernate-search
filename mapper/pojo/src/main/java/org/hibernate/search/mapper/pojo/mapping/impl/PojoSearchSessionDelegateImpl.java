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
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeDelegateImpl;
import org.hibernate.search.mapper.pojo.work.impl.PojoSessionWorkExecutorImpl;
import org.hibernate.search.mapper.pojo.work.impl.PojoWorkPlanImpl;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.mapper.pojo.session.spi.PojoSearchSessionDelegate;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoSessionContextImplementor;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class PojoSearchSessionDelegateImpl implements PojoSearchSessionDelegate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoIndexedTypeManagerContainer indexedTypeManagers;
	private final PojoContainedTypeManagerContainer containedTypeManagers;
	private final AbstractPojoSessionContextImplementor sessionContext;

	PojoSearchSessionDelegateImpl(PojoIndexedTypeManagerContainer indexedTypeManagers,
			PojoContainedTypeManagerContainer containedTypeManagers,
			AbstractPojoSessionContextImplementor sessionContext) {
		this.indexedTypeManagers = indexedTypeManagers;
		this.containedTypeManagers = containedTypeManagers;
		this.sessionContext = sessionContext;
	}

	@Override
	public AbstractPojoSessionContextImplementor getSessionContext() {
		return sessionContext;
	}

	@Override
	public <E, E2> PojoScopeDelegate<E, E2> createPojoScope(
			Collection<? extends Class<? extends E>> targetedTypes) {
		if ( targetedTypes.isEmpty() ) {
			throw log.invalidEmptyTargetForScope();
		}

		Set<PojoIndexedTypeManager<?, ? extends E, ?>> targetedTypeManagers = new LinkedHashSet<>();
		Set<Class<?>> nonIndexedTypes = new LinkedHashSet<>();
		Set<Class<?>> nonIndexedButContainedTypes = new LinkedHashSet<>();
		for ( Class<? extends E> targetedType : targetedTypes ) {
			Optional<? extends Set<? extends PojoIndexedTypeManager<?, ? extends E, ?>>> targetedTypeManagersForType =
					indexedTypeManagers.getAllBySuperClass( targetedType );
			if ( targetedTypeManagersForType.isPresent() ) {
				targetedTypeManagers.addAll( targetedTypeManagersForType.get() );
			}
			else {
				// Remember this to produce a clear error message
				nonIndexedTypes.add( targetedType );
				if ( containedTypeManagers.getByExactClass( targetedType ).isPresent() ) {
					nonIndexedButContainedTypes.add( targetedType );
				}
			}
		}

		if ( !nonIndexedTypes.isEmpty() || !nonIndexedButContainedTypes.isEmpty() ) {
			throw log.invalidScopeTarget( nonIndexedTypes, nonIndexedButContainedTypes );
		}

		return new PojoScopeDelegateImpl<>( indexedTypeManagers, targetedTypeManagers, sessionContext );
	}

	@Override
	public PojoWorkPlan createWorkPlan(DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return new PojoWorkPlanImpl(
				indexedTypeManagers, containedTypeManagers, sessionContext,
				commitStrategy, refreshStrategy
		);
	}

	@Override
	public PojoSessionWorkExecutor createSessionWorkExecutor(DocumentCommitStrategy commitStrategy) {
		return new PojoSessionWorkExecutorImpl( indexedTypeManagers, sessionContext, commitStrategy );
	}

}
