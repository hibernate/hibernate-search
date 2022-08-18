/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.common.resources.spi.SavedState;
import org.hibernate.search.engine.common.resources.impl.EngineThreads;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationEnvironment;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.environment.thread.impl.ThreadPoolProviderImpl;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.impl.EngineEventContextMessages;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.engine.reporting.spi.RootFailureCollector;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class SearchIntegrationImpl implements SearchIntegration {

	static final SavedState.Key<Map<String, SavedState>> INDEX_MANAGERS_KEY = SavedState.key( "index_managers" );

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
			throw log.noDefaultBackendRegistered( backends.keySet() );
		}
		return backend.toAPI();
	}

	@Override
	public Backend backend(String backendName) {
		BackendImplementor backend = backends.get( backendName );
		if ( backend == null ) {
			throw log.unknownNameForBackend( backendName,
					backends.keySet().stream().filter( Objects::nonNull ).collect( Collectors.toList() ),
					backends.containsKey( null ) ? log.defaultBackendAvailable()
							: log.defaultBackendUnavailable() );
		}
		return backend.toAPI();
	}

	@Override
	public IndexManager indexManager(String indexManagerName) {
		IndexManagerImplementor indexManager = indexManagers.get( indexManagerName );
		if ( indexManager == null ) {
			throw log.unknownNameForIndexManager( indexManagerName, indexManagers.keySet() );
		}
		return indexManager.toAPI();
	}

	SavedState saveForRestart() {
		HashMap<String, SavedState> states = new HashMap<>();
		for ( Map.Entry<String, IndexManagerImplementor> indexManager : indexManagers.entrySet() ) {
			states.put( indexManager.getKey(), indexManager.getValue().saveForRestart() );
		}
		return SavedState.builder().put( INDEX_MANAGERS_KEY, states ).build();
	}

	@Override
	public Builder restartBuilder(SearchIntegrationEnvironment environment) {
		return new SearchIntegrationBuilder( environment, Optional.of( this ) );
	}

	@Override
	public void close() {
		RootFailureCollector rootFailureCollector =
				new RootFailureCollector( EngineEventContextMessages.INSTANCE.shutdown() );

		// Stop mappings
		stopAllSafelyInParallel( mappings,
				(mapping, contextualFailureCollector) -> {
					MappingPreStopContextImpl preStopContext =
							new MappingPreStopContextImpl( contextualFailureCollector );
					return mapping.preStop( preStopContext );
				},
				rootFailureCollector,
				(failureCollector, mappingKey) -> failureCollector.withContext( mappingKey ) );
		stopAllSafely( mappings,
				(mapping, ignored) -> mapping.stop(),
				rootFailureCollector,
				(failureCollector, mappingKey) -> failureCollector.withContext( mappingKey ) );

		// Stop indexes
		stopAllSafelyInParallel( indexManagers,
				(indexManager, contextualFailureCollector) -> indexManager.preStop(),
				rootFailureCollector,
				(failureCollector, name) -> failureCollector.withContext( EventContexts.fromIndexName( name ) ) );
		stopAllSafely( indexManagers,
				(indexManager, contextualFailureCollector) -> indexManager.stop(),
				rootFailureCollector,
				(failureCollector, name) -> failureCollector.withContext( EventContexts.fromIndexName( name ) ) );

		// Stop backends
		stopAllSafelyInParallel( backends,
				(backend, contextualFailureCollector) -> backend.preStop(),
				rootFailureCollector,
				(failureCollector, name) -> failureCollector.withContext( EventContexts.fromBackendName( name ) ) );
		stopAllSafely( backends,
				(backend, contextualFailureCollector) -> backend.stop(),
				rootFailureCollector,
				(failureCollector, name) -> failureCollector.withContext( EventContexts.fromBackendName( name ) ) );

		// Stop engine
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( ThreadPoolProviderImpl::close, threadPoolProvider );
			closer.pushAll( BeanHolder::close, failureHandlerHolder );
			closer.pushAll( BeanProvider::close, beanProvider );
			closer.pushAll( EngineThreads::onStop, engineThreads );
			closer.pushAll( TimingSource::stop, timingSource );
		}
		catch (RuntimeException e) {
			rootFailureCollector.withContext( EventContexts.defaultContext() ).add( e );
		}

		rootFailureCollector.checkNoFailure();
	}

	private <K, V> void stopAllSafely(Map<K, V> map, BiConsumer<V, ContextualFailureCollector> stop,
			FailureCollector failureCollector,
			BiFunction<FailureCollector, K, ContextualFailureCollector> appendEventContext) {
		for ( Map.Entry<K, V> entry : map.entrySet() ) {
			ContextualFailureCollector contextualFailureCollector =
					appendEventContext.apply( failureCollector, entry.getKey() );
			try {
				stop.accept( entry.getValue(), contextualFailureCollector );
			}
			catch (RuntimeException e) {
				contextualFailureCollector.add( e );
			}
		}
	}

	private <K, V> void stopAllSafelyInParallel(Map<K, V> map,
			BiFunction<V, ContextualFailureCollector, CompletableFuture<?>> stop,
			FailureCollector failureCollector,
			BiFunction<FailureCollector, K, ContextualFailureCollector> appendEventContext) {
		CompletableFuture<?>[] futures = new CompletableFuture[map.size()];
		int i = 0;
		for ( Map.Entry<K, V> entry : map.entrySet() ) {
			ContextualFailureCollector contextualFailureCollector =
					appendEventContext.apply( failureCollector, entry.getKey() );
			futures[i] = Futures.create( () -> stop.apply( entry.getValue(), contextualFailureCollector ) )
					.exceptionally( Futures.handler( throwable -> {
						Exception exception = Throwables.expectException( throwable );
						contextualFailureCollector.add( exception );
						return null;
					} ) );
			i++;

		}
		Futures.unwrappedExceptionJoin( CompletableFuture.allOf( futures ) );
	}
}
