/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.search.engine.cfg.EngineSettings;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertyChecker;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.EngineSpiSettings;
import org.hibernate.search.engine.environment.thread.impl.ThreadPoolProviderImpl;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.common.spi.SearchIntegrationBuilder;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.impl.ConfiguredBeanResolver;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.environment.bean.spi.ReflectionBeanProvider;
import org.hibernate.search.engine.environment.classpath.spi.AggregatedClassLoader;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.DefaultClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.DefaultResourceResolver;
import org.hibernate.search.engine.environment.classpath.spi.DefaultServiceResolver;
import org.hibernate.search.engine.environment.classpath.spi.ResourceResolver;
import org.hibernate.search.engine.environment.classpath.spi.ServiceResolver;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappedIndexManagerFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingAbortedException;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingInitiator;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataDiscoverer;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingKey;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingPartialBuildState;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.reporting.impl.FailSafeFailureHandlerWrapper;
import org.hibernate.search.engine.reporting.spi.RootFailureCollector;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.util.common.impl.SuppressingCloser;

public class SearchIntegrationBuilderImpl implements SearchIntegrationBuilder {

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

	private final ConfigurationPropertyChecker propertyChecker;
	private final ConfigurationPropertySource propertySource;
	private final Map<MappingKey<?, ?>, MappingInitiator<?, ?>> mappingInitiators = new LinkedHashMap<>();

	private ClassResolver classResolver;
	private ResourceResolver resourceResolver;
	private ServiceResolver serviceResolver;
	private BeanProvider beanProvider;
	private boolean frozen = false;

	public SearchIntegrationBuilderImpl(ConfigurationPropertySource propertySource,
			ConfigurationPropertyChecker propertyChecker) {
		this.propertyChecker = propertyChecker;
		this.propertySource = propertySource.withMask( "hibernate.search" );
		propertyChecker.beforeBoot();
	}

	@Override
	public ConfigurationPropertySource getMaskedPropertySource() {
		return propertySource;
	}

	@Override
	public SearchIntegrationBuilder setClassResolver(ClassResolver classResolver) {
		this.classResolver = classResolver;
		return this;
	}

	@Override
	public SearchIntegrationBuilder setResourceResolver(ResourceResolver resourceResolver) {
		this.resourceResolver = resourceResolver;
		return this;
	}

	@Override
	public SearchIntegrationBuilder setServiceResolver(ServiceResolver serviceResolver) {
		this.serviceResolver = serviceResolver;
		return this;
	}

	@Override
	public SearchIntegrationBuilder setBeanProvider(BeanProvider beanProvider) {
		this.beanProvider = beanProvider;
		return this;
	}

	@Override
	public <PBM extends MappingPartialBuildState> SearchIntegrationBuilder addMappingInitiator(
			MappingKey<PBM, ?> mappingKey, MappingInitiator<?, PBM> initiator) {
		if ( frozen ) {
			throw new AssertionFailure(
					"Attempt to add a mapping initiator"
					+ " after Hibernate Search has started to build the mappings."
					+ " There is a bug in the Hibernate Search integration."
			);
		}

		MappingInitiator<?, ?> existing = mappingInitiators.putIfAbsent( mappingKey, initiator );

		if ( existing != null ) {
			throw new AssertionFailure(
					"Mapping key '" + mappingKey + "' has multiple initiators: '"
							+ existing + "', '" + initiator + "'."
							+ " There is a bug in the mapper, please report it."
			);
		}
		return this;
	}

	@Override
	public SearchIntegrationPartialBuildState prepareBuild() {
		BeanHolder<? extends FailureHandler> failureHandlerHolder = null;
		BeanHolder<? extends ThreadProvider> threadProviderHolder = null;
		IndexManagerBuildingStateHolder indexManagerBuildingStateHolder = null;
		// Use a LinkedHashMap for deterministic iteration
		List<MappingBuildingState<?, ?>> mappingBuildingStates = new ArrayList<>();
		Map<MappingKey<?, ?>, MappingPartialBuildState> partiallyBuiltMappings = new HashMap<>();
		RootFailureCollector failureCollector = new RootFailureCollector();
		boolean checkingRootFailures = false;

		try {
			frozen = true;

			AggregatedClassLoader aggregatedClassLoader = null;

			if ( classResolver == null ) {
				aggregatedClassLoader = AggregatedClassLoader.createDefault();
				classResolver = DefaultClassResolver.create( aggregatedClassLoader );
			}

			if ( resourceResolver == null ) {
				if ( aggregatedClassLoader == null ) {
					aggregatedClassLoader = AggregatedClassLoader.createDefault();
				}
				resourceResolver = DefaultResourceResolver.create( aggregatedClassLoader );
			}

			if ( serviceResolver == null ) {
				if ( aggregatedClassLoader == null ) {
					aggregatedClassLoader = AggregatedClassLoader.createDefault();
				}
				serviceResolver = DefaultServiceResolver.create( aggregatedClassLoader );
			}

			if ( beanProvider == null ) {
				beanProvider = ReflectionBeanProvider.create( classResolver );
			}

			BeanResolver beanResolver = new ConfiguredBeanResolver( serviceResolver, beanProvider, propertySource );

			failureHandlerHolder = BACKGROUND_FAILURE_HANDLER.getAndTransform( propertySource, beanResolver::resolve );
			// Wrap the failure handler to prevent it from throwing exceptions
			failureHandlerHolder = BeanHolder.of( new FailSafeFailureHandlerWrapper( failureHandlerHolder.get() ) )
					.withDependencyAutoClosing( failureHandlerHolder );
			FailureHandler failureHandler = failureHandlerHolder.get();

			threadProviderHolder = THREAD_PROVIDER.getAndTransform( propertySource, beanResolver::resolve );
			ThreadPoolProviderImpl threadPoolProvider = new ThreadPoolProviderImpl( threadProviderHolder );

			RootBuildContext rootBuildContext = new RootBuildContext(
					propertySource,
					classResolver, resourceResolver, beanResolver,
					failureCollector, threadPoolProvider, failureHandler
			);

			indexManagerBuildingStateHolder = new IndexManagerBuildingStateHolder( beanResolver, propertySource, rootBuildContext );

			// Step #1: collect configuration for all mappings
			for ( Map.Entry<MappingKey<?, ?>, MappingInitiator<?, ?>> entry : mappingInitiators.entrySet() ) {
				// We know the key and initiator have compatible types, see how they are put into the map
				@SuppressWarnings({"rawtypes", "unchecked"})
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
			Set<Optional<String>> backendNames = new LinkedHashSet<>();
			for ( MappingBuildingState<?, ?> mappingBuildingState : mappingBuildingStates ) {
				mappingBuildingState.determineIndexedTypes( backendNames );
			}
			checkingRootFailures = true;
			failureCollector.checkNoFailure();
			checkingRootFailures = false;

			// Step #4: create backends that will be necessary for mappers
			indexManagerBuildingStateHolder.createBackends( backendNames );
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
					beanProvider, beanResolver,
					failureHandlerHolder,
					threadPoolProvider,
					partiallyBuiltMappings,
					indexManagerBuildingStateHolder.getBackendPartialBuildStates(),
					indexManagerBuildingStateHolder.getIndexManagersByName(),
					propertyChecker
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
			closer.pushAll( BeanProvider::close, beanProvider );

			throw rethrownException;
		}
	}

	private static class MappingBuildingState<C, PBM extends MappingPartialBuildState> {
		private final MappingBuildContext buildContext;

		private final MappingKey<PBM, ?> mappingKey;
		private final MappingInitiator<C, PBM> mappingInitiator;

		// Use a LinkedHashMap for deterministic iteration
		private final Map<MappableTypeModel, TypeMappingContribution<C>> contributionByType = new LinkedHashMap<>();
		private final List<TypeMetadataDiscoverer<C>> metadataDiscoverers = new ArrayList<>();

		private final Set<MappableTypeModel> typesSubmittedToDiscoverers = new HashSet<>();

		private Mapper<PBM> mapper; // Initially null, set in createMapper()

		MappingBuildingState(RootBuildContext rootBuildContext,
				MappingKey<PBM, ?> mappingKey, MappingInitiator<C, PBM> mappingInitiator) {
			this.mappingKey = mappingKey;
			this.buildContext = new MappingBuildContextImpl( rootBuildContext, mappingKey );
			this.mappingInitiator = mappingInitiator;
		}

		void collect() {
			mappingInitiator.configure( buildContext, new MappingConfigurationCollectorImpl() );
		}

		void createMapper() {
			TypeMetadataContributorProviderImpl contributorProvider = new TypeMetadataContributorProviderImpl();
			mapper = mappingInitiator.createMapper( buildContext, contributorProvider );
		}

		void determineIndexedTypes(Set<Optional<String>> backendNames) {
			mapper.prepareIndexedTypes( backendNames::add );
		}

		void mapIndexedTypes(MappedIndexManagerFactory indexManagerFactory) {
			mapper.mapIndexedTypes( indexManagerFactory );
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

		private TypeMappingContribution<C> getOrCreateContribution(MappableTypeModel typeModel) {
			TypeMappingContribution<C> contribution = contributionByType.get( typeModel );
			if ( contribution == null ) {
				contribution = new TypeMappingContribution<>();
				contributionByType.put( typeModel, contribution );
			}
			return contribution;
		}

		private TypeMappingContribution<C> getContributionIncludingAutomaticallyDiscovered(
				MappableTypeModel typeModel) {
			if ( !typesSubmittedToDiscoverers.contains( typeModel ) ) {
				// Allow automatic discovery of metadata the first time we encounter each type
				for ( TypeMetadataDiscoverer<C> metadataDiscoverer : metadataDiscoverers ) {
					Optional<C> discoveredContributor = metadataDiscoverer.discover( typeModel );
					if ( discoveredContributor.isPresent() ) {
						getOrCreateContribution( typeModel )
								.collectContributor( discoveredContributor.get() );
					}
				}
				typesSubmittedToDiscoverers.add( typeModel );
			}
			return contributionByType.get( typeModel );
		}

		public void closeOnFailure() {
			if ( mapper != null ) {
				mapper.closeOnFailure();
			}
		}

		private void handleMappingAborted(MappingAbortedException e) {
			ContextualFailureCollector failureCollector = buildContext.getFailureCollector();

			if ( !failureCollector.hasFailure() ) {
				throw new AssertionFailure(
						"Caught " + MappingAbortedException.class.getSimpleName()
								+ ", but the mapper did not collect any failure."
								+ " There is a bug in the mapper, please report it.",
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
			@Override
			public void collectContributor(MappableTypeModel typeModel, C contributor) {
				getOrCreateContribution( typeModel ).collectContributor( contributor );
			}

			@Override
			public void collectDiscoverer(TypeMetadataDiscoverer<C> metadataDiscoverer) {
				metadataDiscoverers.add( metadataDiscoverer );
			}
		}

		private class TypeMetadataContributorProviderImpl implements TypeMetadataContributorProvider<C> {

			@Override
			public Set<C> get(MappableTypeModel typeModel) {
				return typeModel.getDescendingSuperTypes()
						.map( MappingBuildingState.this::getContributionIncludingAutomaticallyDiscovered )
						.filter( Objects::nonNull )
						.flatMap( TypeMappingContribution::getContributors )

						// Using a LinkedHashSet because it seems the order matters.
						// Otherwise, AutomaticIndexingPolymorphicOriginalSideAssociationIT could fail
						// because of PojoTypeAdditionalMetadataProvider#createTypeAdditionalMetadata
						.collect( Collectors.toCollection( LinkedHashSet::new ) );
			}

			@Override
			public Set<? extends MappableTypeModel> getTypesContributedTo() {
				// Use a LinkedHashSet for deterministic iteration
				return Collections.unmodifiableSet( new LinkedHashSet<>( contributionByType.keySet() ) );
			}
		}
	}

	private static class TypeMappingContribution<C> {
		private final List<C> contributors = new ArrayList<>();

		TypeMappingContribution() {
		}

		void collectContributor(C contributor) {
			this.contributors.add( contributor );
		}

		Stream<C> getContributors() {
			return contributors.stream();
		}
	}
}
