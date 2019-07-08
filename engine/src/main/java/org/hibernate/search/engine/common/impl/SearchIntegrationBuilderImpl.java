/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.lang.invoke.MethodHandles;
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
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.SearchIntegrationPartialBuildState;
import org.hibernate.search.engine.common.spi.SearchIntegrationBuilder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.environment.bean.impl.ConfiguredBeanResolver;
import org.hibernate.search.engine.environment.bean.spi.BeanProvider;
import org.hibernate.search.engine.environment.bean.spi.ReflectionBeanProvider;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.DefaultClassAndResourceResolver;
import org.hibernate.search.engine.environment.classpath.spi.ResourceResolver;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.reporting.impl.RootFailureCollector;
import org.hibernate.search.engine.reporting.spi.ContextualFailureCollector;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexManagerBuildingState;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingAbortedException;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingInitiator;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataDiscoverer;
import org.hibernate.search.engine.mapper.mapping.spi.MappingBuildContext;
import org.hibernate.search.engine.mapper.mapping.spi.MappingKey;
import org.hibernate.search.engine.mapper.mapping.spi.MappingPartialBuildState;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.impl.SuppressingCloser;

public class SearchIntegrationBuilderImpl implements SearchIntegrationBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final int FAILURE_LIMIT = 100;

	private final ConfigurationPropertySource mainPropertySource;
	private final Map<MappingKey<?, ?>, MappingInitiator<?, ?>> mappingInitiators = new LinkedHashMap<>();

	private ClassResolver classResolver;
	private ResourceResolver resourceResolver;
	private BeanProvider beanProvider;
	private boolean frozen = false;

	public SearchIntegrationBuilderImpl(ConfigurationPropertySource mainPropertySource) {
		this.mainPropertySource = mainPropertySource;
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
		IndexManagerBuildingStateHolder indexManagerBuildingStateHolder = null;
		// Use a LinkedHashMap for deterministic iteration
		List<MappingBuildingState<?, ?>> mappingBuildingStates = new ArrayList<>();
		Map<MappingKey<?, ?>, MappingPartialBuildState> partiallyBuiltMappings = new HashMap<>();
		RootFailureCollector failureCollector = new RootFailureCollector( FAILURE_LIMIT );
		boolean checkingRootFailures = false;

		try {
			frozen = true;

			DefaultClassAndResourceResolver defaultClassAndResourceResolver = null;

			if ( classResolver == null ) {
				defaultClassAndResourceResolver = new DefaultClassAndResourceResolver();
				classResolver = defaultClassAndResourceResolver;
			}

			if ( resourceResolver == null ) {
				if ( defaultClassAndResourceResolver == null ) {
					defaultClassAndResourceResolver = new DefaultClassAndResourceResolver();
				}
				resourceResolver = defaultClassAndResourceResolver;
			}

			if ( beanProvider == null ) {
				beanProvider = new ReflectionBeanProvider( classResolver );
			}

			ConfigurationPropertySource propertySource = mainPropertySource;

			BeanResolver beanResolver = new ConfiguredBeanResolver( classResolver, beanProvider, propertySource );
			RootBuildContext rootBuildContext = new RootBuildContext( classResolver, resourceResolver, beanResolver, failureCollector );

			indexManagerBuildingStateHolder = new IndexManagerBuildingStateHolder( beanResolver, propertySource, rootBuildContext );

			// First step: collect configuration for all mappings
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

			// Second step: create mappers and their backing index managers
			for ( MappingBuildingState<?, ?> mappingBuildingState : mappingBuildingStates ) {
				mappingBuildingState.createMapper( indexManagerBuildingStateHolder );
			}
			checkingRootFailures = true;
			failureCollector.checkNoFailure();
			checkingRootFailures = false;

			// Third step: create mappings
			for ( MappingBuildingState<?, ?> mappingBuildingState : mappingBuildingStates ) {
				mappingBuildingState.partiallyBuildAndAddTo( partiallyBuiltMappings );
			}
			checkingRootFailures = true;
			failureCollector.checkNoFailure();
			checkingRootFailures = false;

			return new SearchIntegrationPartialBuildStateImpl(
					beanProvider,
					partiallyBuiltMappings,
					indexManagerBuildingStateHolder.getBackendPartialBuildStates(),
					indexManagerBuildingStateHolder.getIndexManagersByName()
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
			// Close the mappers and mappings created so far before aborting
			closer.pushAll( MappingPartialBuildState::closeOnFailure, partiallyBuiltMappings.values() );
			closer.pushAll( MappingBuildingState::closeOnFailure, mappingBuildingStates );
			// Close the resources contained in the index manager building state before aborting
			closer.pushAll( holder -> holder.closeOnFailure( closer ), indexManagerBuildingStateHolder );
			// Close the bean resolver before aborting
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
		private boolean multiTenancyEnabled;

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

		void createMapper(IndexManagerBuildingStateHolder indexManagerBuildingStateHolder) {
			TypeMetadataContributorProviderImpl contributorProvider = new TypeMetadataContributorProviderImpl();
			mapper = mappingInitiator.createMapper( buildContext, contributorProvider );

			// We need to create a separate Set because calls to mapper.addIndexed() might add more contributions
			Set<MappableTypeModel> potentiallyMappedToIndexTypes = new LinkedHashSet<>( contributionByType.keySet() );
			for ( MappableTypeModel typeModel : potentiallyMappedToIndexTypes ) {
				TypeMappingContribution<C> contribution = contributionByType.get( typeModel );
				String indexName = contribution.getIndexName();
				if ( indexName != null ) {
					String backendName = contribution.getBackendName();
					IndexManagerBuildingState<?> indexManagerBuildingState;
					try {
						indexManagerBuildingState = indexManagerBuildingStateHolder
								.getBackend( backendName )
								.getIndexManagerBuildingState( indexName, multiTenancyEnabled );
					}
					catch (RuntimeException e) {
						buildContext.getFailureCollector()
								.withContext( EventContexts.fromType( typeModel ) )
								.withContext( EventContexts.fromIndexName( indexName ) )
								.add( e );
						continue;
					}
					try {
						mapper.addIndexed(
								typeModel,
								indexManagerBuildingState
						);
					}
					catch (RuntimeException e) {
						buildContext.getFailureCollector()
								.withContext( EventContexts.fromType( typeModel ) )
								.add( e );
					}
				}
			}
		}

		void partiallyBuildAndAddTo(Map<MappingKey<?, ?>, MappingPartialBuildState> mappings) {
			try {
				PBM partiallyBuiltMapping = mapper.prepareBuild();
				mappings.put( mappingKey, partiallyBuiltMapping );
			}
			catch (MappingAbortedException e) {
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
		}

		private TypeMappingContribution<C> getOrCreateContribution(MappableTypeModel typeModel) {
			TypeMappingContribution<C> contribution = contributionByType.get( typeModel );
			if ( contribution == null ) {
				contribution = new TypeMappingContribution<>( typeModel );
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

		private class MappingConfigurationCollectorImpl implements MappingConfigurationCollector<C> {
			@Override
			public void mapToIndex(MappableTypeModel typeModel, String backendName, String indexName) {
				if ( typeModel.isAbstract() ) {
					throw log.cannotMapAbstractTypeToIndex( typeModel, indexName );
				}
				getOrCreateContribution( typeModel ).mapToIndex( backendName, indexName );
			}

			@Override
			public void collectContributor(MappableTypeModel typeModel, C contributor) {
				getOrCreateContribution( typeModel ).collectContributor( contributor );
			}

			@Override
			public void collectDiscoverer(TypeMetadataDiscoverer<C> metadataDiscoverer) {
				metadataDiscoverers.add( metadataDiscoverer );
			}

			@Override
			public void enableMultiTenancy() {
				multiTenancyEnabled = true;
			}
		}

		private class TypeMetadataContributorProviderImpl implements TypeMetadataContributorProvider<C> {
			@Override
			public void forEach(MappableTypeModel typeModel, Consumer<C> contributorConsumer) {
				typeModel.getDescendingSuperTypes()
						.map( MappingBuildingState.this::getContributionIncludingAutomaticallyDiscovered )
						.filter( Objects::nonNull )
						.flatMap( TypeMappingContribution::getContributors )
						.forEach( contributorConsumer );
			}

			@Override
			public Set<? extends MappableTypeModel> getTypesContributedTo() {
				// Use a LinkedHashSet for deterministic iteration
				return Collections.unmodifiableSet( new LinkedHashSet<>( contributionByType.keySet() ) );
			}
		}
	}

	private static class TypeMappingContribution<C> {
		private final MappableTypeModel typeModel;
		private String indexName;
		private String backendName;
		private final List<C> contributors = new ArrayList<>();

		TypeMappingContribution(MappableTypeModel typeModel) {
			this.typeModel = typeModel;
		}

		public String getIndexName() {
			return indexName;
		}

		public String getBackendName() {
			return backendName;
		}

		public void mapToIndex(String backendName, String indexName) {
			if ( this.indexName != null ) {
				throw log.multipleIndexMapping( typeModel, this.indexName, indexName );
			}
			this.backendName = backendName;
			this.indexName = indexName;
		}

		public void collectContributor(C contributor) {
			this.contributors.add( contributor );
		}

		public Stream<C> getContributors() {
			return contributors.stream();
		}
	}
}
