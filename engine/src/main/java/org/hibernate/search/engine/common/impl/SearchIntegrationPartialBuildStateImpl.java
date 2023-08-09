/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import static org.hibernate.search.engine.common.impl.SearchIntegrationImpl.INDEX_MANAGERS_KEY;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.engine.common.resources.impl.EngineThreads;
import org.hibernate.search.engine.common.resources.spi.SavedState;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationEnvironment;
import org.hibernate.search.engine.common.spi.SearchIntegrationFinalizer;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.environment.thread.impl.ThreadPoolProviderImpl;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingFinalizationContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingFinalizer;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingPartialBuildState;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.impl.EngineEventContextMessages;
import org.hibernate.search.engine.reporting.spi.RootFailureCollector;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.Futures;

class SearchIntegrationPartialBuildStateImpl implements SearchIntegrationPartialBuildState {

	private final BeanProvider beanProvider;
	private final BeanResolver beanResolver;
	private final BeanHolder<? extends FailureHandler> failureHandlerHolder;
	private final ThreadPoolProviderImpl threadPoolProvider;

	private final Map<MappingKey<?, ?>, MappingPartialBuildState> partiallyBuiltMappings;
	private final Map<String, BackendNonStartedState> nonStartedBackends;
	private final Map<String, IndexManagerNonStartedState> nonStartedIndexManagers;

	private final ConfigurationPropertyChecker partialConfigurationPropertyChecker;

	private final Map<MappingKey<?, ?>, MappingNonStartedState> fullyBuiltNonStartedMappings = new LinkedHashMap<>();

	private final Map<String, BackendImplementor> startedBackends = new LinkedHashMap<>();
	private final Map<String, IndexManagerImplementor> startedIndexManagers = new LinkedHashMap<>();
	private final Map<MappingKey<?, ?>, MappingImplementor<?>> fullyBuiltStartedMappings = new LinkedHashMap<>();

	private final EngineThreads engineThreads;
	private final TimingSource timingSource;
	private final Optional<SearchIntegrationImpl> previousIntegration;

	SearchIntegrationPartialBuildStateImpl(
			BeanProvider beanProvider, BeanResolver beanResolver,
			BeanHolder<? extends FailureHandler> failureHandlerHolder,
			ThreadPoolProviderImpl threadPoolProvider,
			Map<MappingKey<?, ?>, MappingPartialBuildState> partiallyBuiltMappings,
			Map<String, BackendNonStartedState> nonStartedBackends,
			Map<String, IndexManagerNonStartedState> nonStartedIndexManagers,
			ConfigurationPropertyChecker partialConfigurationPropertyChecker,
			EngineThreads engineThreads, TimingSource timingSource,
			Optional<SearchIntegrationImpl> previousIntegration) {
		this.beanProvider = beanProvider;
		this.beanResolver = beanResolver;
		this.failureHandlerHolder = failureHandlerHolder;
		this.threadPoolProvider = threadPoolProvider;
		this.partiallyBuiltMappings = partiallyBuiltMappings;
		this.nonStartedBackends = nonStartedBackends;
		this.nonStartedIndexManagers = nonStartedIndexManagers;
		this.partialConfigurationPropertyChecker = partialConfigurationPropertyChecker;
		this.engineThreads = engineThreads;
		this.timingSource = timingSource;
		this.previousIntegration = previousIntegration;
	}

	@Override
	public void closeOnFailure() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( MappingPartialBuildState::closeOnFailure, partiallyBuiltMappings.values() );
			closer.pushAll( MappingNonStartedState::closeOnFailure, fullyBuiltNonStartedMappings.values() );
			closer.pushAll( MappingImplementor::stop, fullyBuiltStartedMappings.values() );
			closer.pushAll( IndexManagerNonStartedState::closeOnFailure, nonStartedIndexManagers.values() );
			closer.pushAll( IndexManagerImplementor::stop, startedIndexManagers.values() );
			closer.pushAll( BackendNonStartedState::closeOnFailure, nonStartedBackends.values() );
			closer.pushAll( BackendImplementor::stop, startedBackends.values() );
			closer.pushAll( ThreadPoolProviderImpl::close, threadPoolProvider );
			closer.pushAll( BeanHolder::close, failureHandlerHolder );
			closer.pushAll( BeanProvider::close, beanProvider );
			closer.pushAll( EngineThreads::onStop, engineThreads );
			closer.pushAll( TimingSource::stop, timingSource );

			if ( previousIntegration.isPresent() ) {
				closer.pushAll( SearchIntegration::close, previousIntegration.get() );
			}
		}
	}

	@Override
	public BeanResolver beanResolver() {
		return beanResolver;
	}

	@Override
	public SearchIntegrationFinalizer finalizer(ConfigurationPropertySource propertySource,
			ConfigurationPropertyChecker configurationPropertyChecker) {
		return new SearchIntegrationFinalizerImpl(
				SearchIntegrationEnvironment.rootPropertySource( propertySource, beanResolver ),
				configurationPropertyChecker
		);
	}

	private class SearchIntegrationFinalizerImpl implements SearchIntegrationFinalizer {

		private final ConfigurationPropertySource propertySource;
		private final ConfigurationPropertyChecker propertyChecker;

		private SearchIntegrationFinalizerImpl(ConfigurationPropertySource propertySource,
				ConfigurationPropertyChecker propertyChecker) {
			this.propertySource = propertySource;
			this.propertyChecker = propertyChecker;
		}

		@Override
		public <PBM, M> M finalizeMapping(MappingKey<PBM, M> mappingKey,
				MappingFinalizer<PBM, M> finalizer) {
			// We know this cast will work because of how
			@SuppressWarnings("unchecked")
			PBM partiallyBuiltMapping = (PBM) partiallyBuiltMappings.get( mappingKey );
			if ( partiallyBuiltMapping == null ) {
				throw new AssertionFailure(
						"Some partially built mapping could not be found during bootstrap. Key: " + mappingKey
				);
			}

			MappingFinalizationContext mappingFinalizationContext =
					new MappingFinalizationContextImpl( propertySource, beanResolver );

			MappingImplementor<M> mapping = finalizer.finalizeMapping( mappingFinalizationContext, partiallyBuiltMapping );
			fullyBuiltNonStartedMappings.put( mappingKey, new MappingNonStartedState( mappingKey, mapping ) );
			partiallyBuiltMappings.remove( mappingKey );

			return mapping.toConcreteType();
		}

		@Override
		public SearchIntegration finalizeIntegration() {
			if ( !partiallyBuiltMappings.isEmpty() ) {
				throw new AssertionFailure(
						"Some mappings were not fully built. Partially built mappings: " + partiallyBuiltMappings
				);
			}

			RootFailureCollector failureCollector =
					new RootFailureCollector( EngineEventContextMessages.INSTANCE.bootstrap() );

			// Start backends
			for ( Map.Entry<String, BackendNonStartedState> entry : nonStartedBackends.entrySet() ) {
				startedBackends.put(
						entry.getKey(),
						entry.getValue().start( failureCollector, beanResolver, propertySource, threadPoolProvider )
				);
			}
			failureCollector.checkNoFailure();

			// Pre-Start indexes
			try ( SavedState previousIntegrationSavedState =
					previousIntegration.map( SearchIntegrationImpl::saveForRestart ).orElse( SavedState.empty() ) ) {
				for ( Map.Entry<String, IndexManagerNonStartedState> entry : nonStartedIndexManagers.entrySet() ) {
					SavedState savedState = previousIntegrationSavedState.get( INDEX_MANAGERS_KEY )
							.orElse( Collections.emptyMap() ).getOrDefault( entry.getKey(), SavedState.empty() );
					entry.getValue().preStart( failureCollector, beanResolver, propertySource, savedState );
				}
			}
			failureCollector.checkNoFailure();

			if ( previousIntegration.isPresent() ) {
				previousIntegration.get().close();
			}

			// Start indexes
			for ( Map.Entry<String, IndexManagerNonStartedState> entry : nonStartedIndexManagers.entrySet() ) {
				startedIndexManagers.put(
						entry.getKey(),
						entry.getValue().start()
				);
			}
			failureCollector.checkNoFailure();

			// Start mappings
			SearchIntegrationHandle integrationHandle = new SearchIntegrationHandle();
			CompletableFuture<?>[] mappingFutures = new CompletableFuture<?>[fullyBuiltNonStartedMappings.size()];
			int mappingIndex = 0;
			// Start
			for ( MappingNonStartedState state : fullyBuiltNonStartedMappings.values() ) {
				mappingFutures[mappingIndex] = state.start( failureCollector, beanResolver, propertySource,
						threadPoolProvider, integrationHandle );
				++mappingIndex;
			}
			// Wait for the starting operation to finish
			Futures.unwrappedExceptionJoin( CompletableFuture.allOf( mappingFutures ) );
			failureCollector.checkNoFailure();
			// Everything went well: register the mappings
			for ( Map.Entry<MappingKey<?, ?>, MappingNonStartedState> entry : fullyBuiltNonStartedMappings.entrySet() ) {
				fullyBuiltStartedMappings.put( entry.getKey(), entry.getValue().getMapping() );
			}

			propertyChecker.afterBoot( partialConfigurationPropertyChecker );

			SearchIntegrationImpl integration = new SearchIntegrationImpl(
					beanProvider,
					failureHandlerHolder,
					threadPoolProvider,
					fullyBuiltStartedMappings,
					startedBackends,
					startedIndexManagers,
					engineThreads, timingSource
			);
			integrationHandle.initialize( integration );

			return integration;
		}
	}

}
