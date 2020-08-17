/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.common.resources.impl.EngineThreads;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.environment.thread.impl.ThreadPoolProviderImpl;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.impl.EngineEventContextMessages;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.reporting.spi.RootFailureCollector;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class SearchIntegrationImpl implements SearchIntegration {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanProvider beanProvider;
	private final BeanHolder<? extends FailureHandler> failureHandlerHolder;
	private final ThreadPoolProviderImpl threadPoolProvider;

	private final Map<MappingKey<?, ?>, MappingImplementor<?>> mappings;
	private final Map<String, BackendImplementor> backends;
	private final Map<String, IndexManagerImplementor> indexManagers;

	private final EngineThreads engineThreads;
	private final TimingSource timingSource;

	SearchIntegrationImpl(BeanProvider beanProvider,
			BeanHolder<? extends FailureHandler> failureHandlerHolder,
			ThreadPoolProviderImpl threadPoolProvider,
			Map<MappingKey<?, ?>, MappingImplementor<?>> mappings,
			Map<String, BackendImplementor> backends,
			Map<String, IndexManagerImplementor> indexManagers,
			EngineThreads engineThreads, TimingSource timingSource) {
		this.beanProvider = beanProvider;
		this.failureHandlerHolder = failureHandlerHolder;
		this.threadPoolProvider = threadPoolProvider;
		this.mappings = mappings;
		this.backends = backends;
		this.indexManagers = indexManagers;
		this.engineThreads = engineThreads;
		this.timingSource = timingSource;
	}

	@Override
	public Backend backend() {
		BackendImplementor backend = backends.get( null );
		if ( backend == null ) {
			throw log.noDefaultBackendRegistered();
		}
		return backend.toAPI();
	}

	@Override
	public Backend backend(String backendName) {
		BackendImplementor backend = backends.get( backendName );
		if ( backend == null ) {
			throw log.noBackendRegistered( backendName );
		}
		return backend.toAPI();
	}

	@Override
	public IndexManager indexManager(String indexManagerName) {
		IndexManagerImplementor indexManager = indexManagers.get( indexManagerName );
		if ( indexManager == null ) {
			throw log.noIndexManagerRegistered( indexManagerName );
		}
		return indexManager.toAPI();
	}

	@Override
	public void close() {
		RootFailureCollector failureCollector = new RootFailureCollector( EngineEventContextMessages.INSTANCE.shutdown() );
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( this::preStopMappings, failureCollector );
			closer.pushAll( MappingImplementor::stop, mappings.values() );
			closer.push( SearchIntegrationImpl::preStopIndexManagers, this );
			closer.pushAll( IndexManagerImplementor::stop, indexManagers.values() );
			closer.push( SearchIntegrationImpl::preStopBackends, this );
			closer.pushAll( BackendImplementor::stop, backends.values() );
			closer.pushAll( ThreadPoolProviderImpl::close, threadPoolProvider );
			closer.pushAll( BeanHolder::close, failureHandlerHolder );
			closer.pushAll( BeanProvider::close, beanProvider );
			closer.pushAll( EngineThreads::onStop, engineThreads );
			closer.pushAll( TimingSource::stop, timingSource );
		}
		catch (RuntimeException e) {
			failureCollector.withContext( EventContexts.defaultContext() ).add( e );
		}

		failureCollector.checkNoFailure();
	}

	private void preStopMappings(RootFailureCollector failureCollector) {
		CompletableFuture<?>[] futures = new CompletableFuture[mappings.size()];
		int i = 0;
		for ( Map.Entry<MappingKey<?, ?>, MappingImplementor<?>> entry : mappings.entrySet() ) {
			MappingPreStopContextImpl preStopContext = new MappingPreStopContextImpl(
					failureCollector.withContext( entry.getKey() )
			);
			futures[i] = entry.getValue().preStop( preStopContext );
			i++;
		}
		Futures.unwrappedExceptionJoin( CompletableFuture.allOf( futures ) );
	}

	private void preStopIndexManagers() {
		CompletableFuture<?>[] futures = new CompletableFuture[indexManagers.size()];
		int i = 0;
		for ( IndexManagerImplementor indexManager : indexManagers.values() ) {
			futures[i] = indexManager.preStop();
			i++;
		}
		Futures.unwrappedExceptionJoin( CompletableFuture.allOf( futures ) );
	}

	private void preStopBackends() {
		CompletableFuture<?>[] futures = new CompletableFuture[backends.size()];
		int i = 0;
		for ( BackendImplementor backend : backends.values() ) {
			futures[i] = backend.preStop();
			i++;
		}
		Futures.unwrappedExceptionJoin( CompletableFuture.allOf( futures ) );
	}
}
