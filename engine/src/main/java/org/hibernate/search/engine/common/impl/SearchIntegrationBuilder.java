/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.common.resources.impl.EngineThreads;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationEnvironment;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.engine.common.timing.impl.DefaultTimingSource;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.thread.impl.ThreadPoolProviderImpl;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.BackendsInfo;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappedIndexManagerFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingAbortedException;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingInitiator;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingPartialBuildState;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.mapper.model.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.model.spi.TypeMetadataDiscoverer;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.impl.EngineEventContextMessages;
import org.hibernate.search.engine.reporting.impl.FailSafeFailureHandlerWrapper;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.RootFailureCollector;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.SuppressingCloser;

public class SearchIntegrationBuilder implements SearchIntegration.Builder {

	private static final ConfigurationProperty<BeanReference<? extends FailureHandler>> BACKGROUND_FAILURE_HANDLER =
			ConfigurationProperty.forKey( EngineSettings.Radicals.BACKGROUND_FAILURE_HANDLER )
					.asBeanReference( FailureHandler.class )
					.withDefault( EngineSettings.Defaults.BACKGROUND_FAILURE_HANDLER )
					.build();

	private static final ConfigurationProperty<BeanReference<? extends ThreadProvider>> THREAD_PROVIDER =
			ConfigurationProperty.forKey( EngineSpiSettings.Radicals.THREAD_PROVIDER )
					.asBeanReference( ThreadProvider.class )
					.withDefault( EngineSpiSettings.Defaults.THREAD_PROVIDER )
					.build();

	private final SearchIntegrationEnvironment environment;
	private final Optional<SearchIntegrationImpl> previousIntegration;
	private final Map<MappingKey<?, ?>, MappingInitiator<?, ?>> mappingInitiators = new LinkedHashMap<>();

	private boolean frozen = false;

	public SearchIntegrationBuilder(SearchIntegrationEnvironment environment,
			Optional<SearchIntegrationImpl> previousIntegration) {
		this.environment = environment;
		this.previousIntegration = previousIntegration;
		environment.propertyChecker().beforeBoot();
	}

	@Override
	public <PBM extends MappingPartialBuildState> SearchIntegration.Builder addMappingInitiator(
			MappingKey<PBM, ?> mappingKey, MappingInitiator<?, PBM> initiator) {
		if ( frozen ) {
			throw new AssertionFailure(
					"Attempt to add a mapping initiator"
							+ " after Hibernate Search has started to build the mappings."
			);
		}

		MappingInitiator<?, ?> existing = mappingInitiators.putIfAbsent( mappingKey, initiator );

		if ( existing != null ) {
			throw new AssertionFailure(
					"Mapping key '" + mappingKey + "' has multiple initiators: '"
							+ existing + "', '" + initiator + "'."
			);
		}
		return this;
	}

	@Override
	public SearchIntegrationPartialBuildState prepareBuild() {
		ConfigurationPropertySource propertySource = environment.propertySource();
		BeanResolver beanResolver = environment.beanResolver();
		BeanHolder<? extends FailureHandler> failureHandlerHolder = null;
		BeanHolder<? extends ThreadProvider> threadProviderHolder = null;
		IndexManagerBuildingStateHolder indexManagerBuildingStateHolder = null;
		// Use a LinkedHashMap for deterministic iteration
		List<MappingBuildingState<?, ?>> mappingBuildingStates = new ArrayList<>();
		Map<MappingKey<?, ?>, MappingPartialBuildState> partiallyBuiltMappings = new HashMap<>();
		RootFailureCollector failureCollector = new RootFailureCollector( EngineEventContextMessages.INSTANCE.bootstrap() );
		boolean checkingRootFailures = false;
		EngineThreads engineThreads = null;
		TimingSource timingSource = null;

		try {
			frozen = true;

			failureHandlerHolder = BACKGROUND_FAILURE_HANDLER.getAndTransform( propertySource, beanResolver::resolve );
			// Wrap the failure handler to prevent it from throwing exceptions
			failureHandlerHolder = BeanHolder.of( new FailSafeFailureHandlerWrapper( failureHandlerHolder.get() ) )
					.withDependencyAutoClosing( failureHandlerHolder );
			FailureHandler failureHandler = failureHandlerHolder.get();

			threadProviderHolder = THREAD_PROVIDER.getAndTransform( propertySource, beanResolver::resolve );
			ThreadPoolProviderImpl threadPoolProvider = new ThreadPoolProviderImpl( threadProviderHolder );
			engineThreads = new EngineThreads( threadPoolProvider );
			timingSource = new DefaultTimingSource( engineThreads );

			RootBuildContext rootBuildContext = new RootBuildContext(
					propertySource,
					environment.classResolver(), environment.resourceResolver(), beanResolver,
					failureCollector, threadPoolProvider, failureHandler,
					engineThreads, timingSource
			);

			indexManagerBuildingStateHolder =
					new IndexManagerBuildingStateHolder( beanResolver, propertySource, rootBuildContext );

			// Step #1: collect configuration for all mappings
			for ( Map.Entry<MappingKey<?, ?>, MappingInitiator<?, ?>> entry : mappingInitiators.entrySet() ) {
				// We know the key and initiator have compatible types, see how they are put into the map
				@SuppressWarnings({ "rawtypes", "unchecked" })
				MappingBuildingState<?, ?> mappingBuildingState = new MappingBuildingState<>(
						rootBuildContext,
						(MappingKey) entry.getKey(), entry.getValue()
				);
				mappingBuildingStates.add( mappingBuildingState );
				mappingBuildingState.collect();
			}
			checkingRootFailures = true;
			failureCollector.checkNoFailure();
			checkingRootFailures = false;

			// Step #2: create mappers
			for ( MappingBuildingState<?, ?> mappingBuildingState : mappingBuildingStates ) {
				mappingBuildingState.createMapper();
			}
			checkingRootFailures = true;
			failureCollector.checkNoFailure();
			checkingRootFailures = false;

			// Step #3: determine indexed types and the necessary backends
			BackendsInfo backendsInfo = new BackendsInfo();
			for ( MappingBuildingState<?, ?> mappingBuildingState : mappingBuildingStates ) {
				mappingBuildingState.determineIndexedTypes( backendsInfo );
			}
			checkingRootFailures = true;
			failureCollector.checkNoFailure();
			checkingRootFailures = false;

			// Step #4: create backends that will be necessary for mappers
			indexManagerBuildingStateHolder.createBackends( backendsInfo );
			checkingRootFailures = true;
			failureCollector.checkNoFailure();
			checkingRootFailures = false;

			// Step #5: map indexed types and create the corresponding index managers
			MappedIndexManagerFactory mappedIndexManagerFactory =
					new MappedIndexManagerFactoryImpl( indexManagerBuildingStateHolder );
			for ( MappingBuildingState<?, ?> mappingBuildingState : mappingBuildingStates ) {
				mappingBuildingState.mapIndexedTypes( mappedIndexManagerFactory );
			}
			checkingRootFailures = true;
			failureCollector.checkNoFailure();
			checkingRootFailures = false;

			// Step #6: create mappings
			for ( MappingBuildingState<?, ?> mappingBuildingState : mappingBuildingStates ) {
				mappingBuildingState.partiallyBuildAndAddTo( partiallyBuiltMappings );
			}
			checkingRootFailures = true;
			failureCollector.checkNoFailure();
			checkingRootFailures = false;

			return new SearchIntegrationPartialBuildStateImpl(
					environment.beanProvider(), beanResolver,
					failureHandlerHolder,
					threadPoolProvider,
					partiallyBuiltMappings,
					indexManagerBuildingStateHolder.getBackendNonStartedStates(),
					indexManagerBuildingStateHolder.getIndexManagersNonStartedStates(),
					environment.propertyChecker(),
					engineThreads, timingSource, previousIntegration
			);
		}
		catch (RuntimeException e) {
			RuntimeException rethrownException;
			if ( checkingRootFailures ) {
				// The exception was thrown by one of the failure checks above. No need for an additional check.
				rethrownException = e;
			}
			else {
				/*
				 * The exception was thrown by something other than the failure checks above
				 * (a mapper, a backend, ...).
				 * We should check that no failure was collected before.
				 */
				try {
					failureCollector.checkNoFailure();
					// No other failure, just rethrow the exception.
					rethrownException = e;
				}
				catch (SearchException e2) {
					/*
					 * At least one failure was collected, most likely before "e" was even thrown.
					 * Let's throw "e2" (which mentions prior failures), only mentioning "e" as a suppressed exception.
					 */
					rethrownException = e2;
					rethrownException.addSuppressed( e );
				}
			}

			SuppressingCloser closer = new SuppressingCloser( rethrownException );
			// Release the failure handler before aborting
			closer.push( failureHandlerHolder );
			// Close the mappers and mappings created so far before aborting
			closer.pushAll( MappingPartialBuildState::closeOnFailure, partiallyBuiltMappings.values() );
			closer.pushAll( MappingBuildingState::closeOnFailure, mappingBuildingStates );
			// Close the resources contained in the index manager building state before aborting
			closer.pushAll( holder -> holder.closeOnFailure( closer ), indexManagerBuildingStateHolder );
			// Close environment resources before aborting
			closer.pushAll( BeanHolder::close, threadProviderHolder );
			closer.pushAll( SearchIntegrationEnvironment::close, environment );
			closer.push( EngineThreads::onStop, engineThreads );
			closer.push( TimingSource::stop, timingSource );

			throw rethrownException;
		}
	}

	private static class MappingBuildingState<C, PBM extends MappingPartialBuildState> {
		private final MappingBuildContext buildContext;

		private final MappingKey<PBM, ?> mappingKey;
		private final MappingInitiator<C, PBM> mappingInitiator;

		private TypeMetadataContributorProvider<C> metadataContributorProvider;

		private Mapper<PBM> mapper; // Initially null, set in createMapper()

		MappingBuildingState(RootBuildContext rootBuildContext,
				MappingKey<PBM, ?> mappingKey, MappingInitiator<C, PBM> mappingInitiator) {
			this.mappingKey = mappingKey;
			this.buildContext = new MappingBuildContextImpl( rootBuildContext, mappingKey );
			this.mappingInitiator = mappingInitiator;
		}

		void collect() {
			TypeMetadataContributorProvider.Builder<C> builder = TypeMetadataContributorProvider.builder();
			mappingInitiator.configure( buildContext, new MappingConfigurationCollectorImpl( builder ) );
			metadataContributorProvider = builder.build();
		}

		void createMapper() {
			mapper = mappingInitiator.createMapper( buildContext, metadataContributorProvider );
		}

		void determineIndexedTypes(BackendsInfo backendsInfo) {
			mapper.prepareMappedTypes( backendsInfo );
		}

		void mapIndexedTypes(MappedIndexManagerFactory indexManagerFactory) {
			mapper.mapTypes( indexManagerFactory );
		}

		void partiallyBuildAndAddTo(Map<MappingKey<?, ?>, MappingPartialBuildState> mappings) {
			try {
				PBM partiallyBuiltMapping = mapper.prepareBuild();
				mappings.put( mappingKey, partiallyBuiltMapping );
			}
			catch (MappingAbortedException e) {
				handleMappingAborted( e );
			}
		}

		public void closeOnFailure() {
			if ( mapper != null ) {
				mapper.closeOnFailure();
			}
		}

		private void handleMappingAborted(MappingAbortedException e) {
			ContextualFailureCollector failureCollector = buildContext.failureCollector();

			if ( !failureCollector.hasFailure() ) {
				throw new AssertionFailure(
						"Caught " + MappingAbortedException.class.getSimpleName()
								+ ", but the mapper did not collect any failure.",
						e
				);
			}

			/*
			 * This generally shouldn't do anything, because we don't expect a cause nor suppressed exceptions
			 * in the MappingAbortedException, but ignoring exceptions can lead to
			 * spending some really annoying hours debugging.
			 * So let's be extra cautious not to lose these.
			 */
			Throwable cause = e.getCause();
			if ( cause != null ) {
				failureCollector.add( cause );
			}
			Throwable[] suppressed = e.getSuppressed();
			for ( Throwable throwable : suppressed ) {
				failureCollector.add( throwable );
			}
		}

		private class MappingConfigurationCollectorImpl implements MappingConfigurationCollector<C> {
			private final TypeMetadataContributorProvider.Builder<C> builder;

			private MappingConfigurationCollectorImpl(TypeMetadataContributorProvider.Builder<C> builder) {
				this.builder = builder;
			}

			@Override
			public void collectContributor(MappableTypeModel typeModel, C contributor) {
				builder.contributor( typeModel, contributor );
			}

			@Override
			public void collectDiscoverer(TypeMetadataDiscoverer<C> metadataDiscoverer) {
				builder.discoverer( metadataDiscoverer );
			}
		}

	}
}
