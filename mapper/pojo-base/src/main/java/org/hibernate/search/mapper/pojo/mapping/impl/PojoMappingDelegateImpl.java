/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.impl;

import java.util.Collection;

import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingDelegate;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.scope.impl.PojoScopeDelegateImpl;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeMappingContext;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeDelegate;
import org.hibernate.search.mapper.pojo.scope.spi.PojoScopeTypeExtendedContextProvider;
import org.hibernate.search.mapper.pojo.session.impl.PojoSearchSessionDelegateImpl;
import org.hibernate.search.mapper.pojo.session.spi.PojoSearchSessionDelegate;
import org.hibernate.search.mapper.pojo.session.context.spi.AbstractPojoBackendSessionContext;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.util.common.impl.Closer;


public class PojoMappingDelegateImpl implements PojoMappingDelegate {

	private final ThreadPoolProvider threadPoolProvider;
	private final FailureHandler failureHandler;
	private final PojoIndexedTypeManagerContainer indexedTypeManagers;
	private final PojoContainedTypeManagerContainer containedTypeManagers;

	public PojoMappingDelegateImpl(ThreadPoolProvider threadPoolProvider,
			FailureHandler failureHandler,
			PojoIndexedTypeManagerContainer indexedTypeManagers,
			PojoContainedTypeManagerContainer containedTypeManagers) {
		this.threadPoolProvider = threadPoolProvider;
		this.failureHandler = failureHandler;
		this.indexedTypeManagers = indexedTypeManagers;
		this.containedTypeManagers = containedTypeManagers;
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( PojoIndexedTypeManager::close, indexedTypeManagers.getAll() );
			closer.pushAll( PojoContainedTypeManager::close, containedTypeManagers.getAll() );
		}
	}

	@Override
	public ThreadPoolProvider getThreadPoolProvider() {
		return threadPoolProvider;
	}

	@Override
	public FailureHandler getFailureHandler() {
		return failureHandler;
	}

	@Override
	public <R, E, E2, C> PojoScopeDelegate<R, E2, C> createPojoScope(
			PojoScopeMappingContext mappingContext,
			Collection<? extends PojoRawTypeIdentifier<? extends E>> targetedTypes,
			PojoScopeTypeExtendedContextProvider<E, C> indexedTypeExtendedContextProvider) {
		return PojoScopeDelegateImpl.create(
				mappingContext,
				indexedTypeManagers,
				containedTypeManagers,
				targetedTypes,
				indexedTypeExtendedContextProvider
		);
	}

	@Override
	public PojoSearchSessionDelegate createSearchSessionDelegate(
			AbstractPojoBackendSessionContext backendSessionContext) {
		return new PojoSearchSessionDelegateImpl(
				indexedTypeManagers, containedTypeManagers,
				backendSessionContext
		);
	}
}
