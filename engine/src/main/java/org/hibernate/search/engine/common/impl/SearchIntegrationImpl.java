/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class SearchIntegrationImpl implements SearchIntegration {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanProvider beanProvider;
	private final BeanHolder<? extends FailureHandler> failureHandlerHolder;
	private final BeanHolder<? extends ThreadProvider> threadProviderHolder;

	private final List<MappingImplementor<?>> mappings;
	private final Map<String, BackendImplementor<?>> backends;
	private final Map<String, IndexManagerImplementor<?>> indexManagers;

	SearchIntegrationImpl(BeanProvider beanProvider,
			BeanHolder<? extends FailureHandler> failureHandlerHolder,
			BeanHolder<? extends ThreadProvider> threadProviderHolder,
			List<MappingImplementor<?>> mappings,
			Map<String, BackendImplementor<?>> backends,
			Map<String, IndexManagerImplementor<?>> indexManagers) {
		this.beanProvider = beanProvider;
		this.failureHandlerHolder = failureHandlerHolder;
		this.threadProviderHolder = threadProviderHolder;
		this.mappings = mappings;
		this.backends = backends;
		this.indexManagers = indexManagers;
	}

	@Override
	public Backend getBackend(String backendName) {
		BackendImplementor<?> backend = backends.get( backendName );
		if ( backend == null ) {
			throw log.noBackendRegistered( backendName );
		}
		return backend.toAPI();
	}

	@Override
	public IndexManager getIndexManager(String indexManagerName) {
		IndexManagerImplementor<?> indexManager = indexManagers.get( indexManagerName );
		if ( indexManager == null ) {
			throw log.noIndexManagerRegistered( indexManagerName );
		}
		return indexManager.toAPI();
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( MappingImplementor::close, mappings );
			closer.push( SearchIntegrationImpl::preStopIndexManagers, this );
			closer.pushAll( IndexManagerImplementor::stop, indexManagers.values() );
			closer.push( SearchIntegrationImpl::preStopBackends, this );
			closer.pushAll( BackendImplementor::stop, backends.values() );
			closer.pushAll( BeanHolder::close, threadProviderHolder );
			closer.pushAll( BeanHolder::close, failureHandlerHolder );
			closer.pushAll( BeanProvider::close, beanProvider );
		}
	}

	private void preStopIndexManagers() {
		CompletableFuture<?>[] futures = new CompletableFuture[indexManagers.size()];
		int i = 0;
		for ( IndexManagerImplementor<?> indexManager : indexManagers.values() ) {
			futures[i] = indexManager.preStop();
			i++;
		}
		Futures.unwrappedExceptionJoin( CompletableFuture.allOf( futures ) );
	}

	private void preStopBackends() {
		CompletableFuture<?>[] futures = new CompletableFuture[backends.size()];
		int i = 0;
		for ( BackendImplementor<?> backend : backends.values() ) {
			futures[i] = backend.preStop();
			i++;
		}
		Futures.unwrappedExceptionJoin( CompletableFuture.allOf( futures ) );
	}
}
