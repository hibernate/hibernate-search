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

import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.engine.common.spi.SearchIntegrationBuilder;
import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.engine.environment.bean.impl.ConfiguredBeanProvider;
import org.hibernate.search.engine.environment.bean.spi.BeanResolver;
import org.hibernate.search.engine.environment.bean.spi.ReflectionBeanResolver;
import org.hibernate.search.engine.environment.classpath.spi.ClassResolver;
import org.hibernate.search.engine.environment.classpath.spi.DefaultClassAndResourceResolver;
import org.hibernate.search.engine.environment.classpath.spi.ResourceResolver;
import org.hibernate.search.engine.environment.service.impl.ServiceManagerImpl;
import org.hibernate.search.engine.environment.service.spi.ServiceManager;
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
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingKey;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.impl.SuppressingCloser;

public class SearchIntegrationBuilderImpl implements SearchIntegrationBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final int FAILURE_LIMIT = 100;

	private final ConfigurationPropertySource mainPropertySource;
	private final Map<String, Object> overriddenProperties = new LinkedHashMap<>();
	private final Map<MappingKey<?>, MappingInitiator<?, ?>> mappingInitiators = new LinkedHashMap<>();

	private ClassResolver classResolver;
	private ResourceResolver resourceResolver;
	private BeanResolver beanResolver;
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
	public SearchIntegrationBuilder setBeanResolver(BeanResolver beanResolver) {
		this.beanResolver = beanResolver;
		return this;
	}

	@Override
	public SearchIntegrationBuilder setProperty(String name, Object value) {
		this.overriddenProperties.put( name, value );
		return this;
	}

	@Override
	public <M> SearchIntegrationBuilder addMappingInitiator(MappingKey<M> mappingKey,
			MappingInitiator<?, M> initiator) {
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
	public SearchIntegration build() {
		IndexManagerBuildingStateHolder indexManagerBuildingStateHolder = null;
		// Use a LinkedHashMap for deterministic iteration
		List<MappingBuildingState<?, ?>> mappingBuildingStates = new ArrayList<>();
		Map<MappingKey<?>, MappingImplementor<?>> mappings = new HashMap<>();
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

			if ( beanResolver == null ) {
				beanResolver = new ReflectionBeanResolver( classResolver );
			}

			ConfigurationPropertySource propertySource;
			if ( !overriddenProperties.isEmpty() ) {
				propertySource = mainPropertySource.withOverride(
						ConfigurationPropertySource.fromMap( overriddenProperties )
				);
			}
			else {
				propertySource = mainPropertySource;
			}

			BeanProvider beanProvider = new ConfiguredBeanProvider( classResolver, beanResolver, propertySource );
			ServiceManager serviceManager = new ServiceManagerImpl( classResolver, resourceResolver, beanProvider );
			RootBuildContext rootBuildContext = new RootBuildContext( serviceManager, failureCollector );

			indexManagerBuildingStateHolder = new IndexManagerBuildingStateHolder( beanProvider, propertySource, rootBuildContext );

			// First phase: collect configuration for all mappings
			for ( Map.Entry<MappingKey<?>, MappingInitiator<?, ?>> entry : mappingInitiators.entrySet() ) {
				// We know the key and initiator have compatible types, see how they are put into the map
				@SuppressWarnings({"rawtypes", "unchecked"})
				MappingBuildingState<?, ?> mappingBuildingState = new MappingBuildingState<>(
						rootBuildContext, propertySource,
						(MappingKey) entry.getKey(), entry.getValue()
				);
				mappingBuildingStates.add( mappingBuildingState );
				mappingBuildingState.collect();
			}
			checkingRootFailures = true;
			failureCollector.checkNoFailure();
			checkingRootFailures = false;

			// Second phase: create mappers and their backing index managers
			for ( MappingBuildingState<?, ?> mappingBuildingState : mappingBuildingStates ) {
				mappingBuildingState.createMapper( indexManagerBuildingStateHolder );
			}
			checkingRootFailures = true;
			failureCollector.checkNoFailure();
			checkingRootFailures = false;

			// Third phase: create mappings
			for ( MappingBuildingState<?, ?> mappingBuildingState : mappingBuildingStates ) {
				mappingBuildingState.createAndAddMapping( mappings );
			}
			checkingRootFailures = true;
			failureCollector.checkNoFailure();
			checkingRootFailures = false;

			// Fourth phase: start indexes
			for ( Map.Entry<String, IndexManagerImplementor<?>> entry :
					indexManagerBuildingStateHolder.getIndexManagersByName().entrySet() ) {
				String indexName = entry.getKey();
				IndexManagerImplementor<?> indexManager = entry.getValue();
				ContextualFailureCollector indexFailureCollector =
						failureCollector.withContext( EventContexts.fromIndexName( indexName ) );
				IndexManagerStartContextImpl startContext = new IndexManagerStartContextImpl( indexFailureCollector );
				// TODO HSEARCH-3084 perform index initialization in parallel for all indexes?
				try {
					indexManager.start( startContext );
				}
				catch (RuntimeException e) {
					indexFailureCollector.add( e );
				}
			}
			checkingRootFailures = true;
			failureCollector.checkNoFailure();
			checkingRootFailures = false;

			return new SearchIntegrationImpl(
					beanResolver,
					mappings,
					indexManagerBuildingStateHolder.getBackendsByName(),
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
			closer.pushAll( MappingImplementor::close, mappings.values() );
			closer.pushAll( MappingBuildingState::closeOnFailure, mappingBuildingStates );
			// Close the resources contained in the index manager building state before aborting
			closer.pushAll( holder -> holder.closeOnFailure( closer ), indexManagerBuildingStateHolder );
			// Close the bean resolver before aborting
			closer.pushAll( BeanResolver::close, beanResolver );

			throw rethrownException;
		}
	}

	private static class MappingBuildingState<C, M> {
		private final MappingBuildContext buildContext;
		private final ConfigurationPropertySource propertySource;

		private final MappingKey<M> mappingKey;
		private final MappingInitiator<C, M> mappingInitiator;

		// Use a LinkedHashMap for deterministic iteration
		private final Map<MappableTypeModel, TypeMappingContribution<C>> contributionByType = new LinkedHashMap<>();
		private final List<TypeMetadataDiscoverer<C>> metadataDiscoverers = new ArrayList<>();
		private boolean multiTenancyEnabled;

		private final Set<MappableTypeModel> typesSubmittedToDiscoverers = new HashSet<>();

		private Mapper<M> mapper; // Initially null, set in createMapper()

		MappingBuildingState(RootBuildContext rootBuildContext, ConfigurationPropertySource propertySource,
				MappingKey<M> mappingKey, MappingInitiator<C, M> mappingInitiator) {
			this.mappingKey = mappingKey;
			this.buildContext = new MappingBuildContextImpl( rootBuildContext, mappingKey );
			this.propertySource = propertySource;
			this.mappingInitiator = mappingInitiator;
		}

		void collect() {
			mappingInitiator.configure( buildContext, propertySource, new MappingConfigurationCollectorImpl() );
		}

		void createMapper(IndexManagerBuildingStateHolder indexManagerBuildingStateHolder) {

			TypeMetadataContributorProviderImpl contributorProvider = new TypeMetadataContributorProviderImpl();
			mapper = mappingInitiator.createMapper( buildContext, propertySource, contributorProvider );

			Set<MappableTypeModel> potentiallyMappedToIndexTypes = new LinkedHashSet<>(
					contributionByType.keySet() );
			for ( MappableTypeModel typeModel : potentiallyMappedToIndexTypes ) {
				TypeMappingContribution<C> contribution = contributionByType.get( typeModel );
				String indexName = contribution.getIndexName();
				if ( indexName != null ) {
					IndexManagerBuildingState<?> indexManagerBuildingState;
					try {
						indexManagerBuildingState = indexManagerBuildingStateHolder
								.startBuilding( indexName, multiTenancyEnabled );
					}
					catch (RuntimeException e) {
						buildContext.getFailureCollector()
								.withContext( EventContexts.fromType( typeModel ) )
								.withContext( EventContexts.fromIndexName( indexName ) )
								.add( e );
						continue;
					}
					mapper.addIndexed(
							typeModel,
							indexManagerBuildingState
					);
				}
			}
		}

		void createAndAddMapping(Map<MappingKey<?>, MappingImplementor<?>> mappings) {
			try {
				MappingImplementor<M> mapping = mapper.build();
				mappings.put( mappingKey, mapping );
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
			public void mapToIndex(MappableTypeModel typeModel, String indexName) {
				if ( typeModel.isAbstract() ) {
					throw log.cannotMapAbstractTypeToIndex( typeModel, indexName );
				}
				getOrCreateContribution( typeModel ).mapToIndex( indexName );
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
		private final List<C> contributors = new ArrayList<>();

		TypeMappingContribution(MappableTypeModel typeModel) {
			this.typeModel = typeModel;
		}

		public String getIndexName() {
			return indexName;
		}

		public void mapToIndex(String indexName) {
			if ( this.indexName != null ) {
				throw log.multipleIndexMapping( typeModel, this.indexName, indexName );
			}
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
