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
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.engine.common.spi.BeanProvider;
import org.hibernate.search.engine.common.spi.BeanResolver;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.common.spi.ReflectionBeanResolver;
import org.hibernate.search.engine.common.spi.ServiceManager;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingInitiator;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataDiscoverer;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.mapping.spi.MappingKey;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.common.LoggerFactory;
import org.hibernate.search.util.impl.common.SuppressingCloser;

public class SearchMappingRepositoryBuilderImpl implements SearchMappingRepositoryBuilder {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ConfigurationPropertySource mainPropertySource;
	private final Properties overriddenProperties = new Properties();
	private final List<MappingInitiator<?, ?>> mappingInitiators = new ArrayList<>();

	private BeanResolver beanResolver;
	private boolean frozen = false;
	private SearchMappingRepository builtResult;

	public SearchMappingRepositoryBuilderImpl(ConfigurationPropertySource mainPropertySource) {
		this.mainPropertySource = mainPropertySource;
	}

	@Override
	public SearchMappingRepositoryBuilder setBeanResolver(BeanResolver beanResolver) {
		this.beanResolver = beanResolver;
		return this;
	}

	@Override
	public SearchMappingRepositoryBuilder setProperty(String name, String value) {
		this.overriddenProperties.setProperty( name, value );
		return this;
	}

	@Override
	public SearchMappingRepositoryBuilder setProperties(Properties properties) {
		this.overriddenProperties.putAll( properties );
		return this;
	}

	@Override
	public SearchMappingRepositoryBuilder addMappingInitiator(MappingInitiator initiator) {
		if ( frozen ) {
			throw new AssertionFailure(
					"Attempt to add a mapping initiator"
					+ " after Hibernate Search has started to build the mappings."
					+ " There is a bug in the Hibernate Search integration."
			);
		}
		mappingInitiators.add( initiator );
		return this;
	}

	@Override
	public SearchMappingRepository build() {
		IndexManagerBuildingStateHolder indexManagerBuildingStateHolder = null;
		// Use a LinkedHashMap for deterministic iteration
		Map<MappingKey<?>, Mapper<?>> mappers = new LinkedHashMap<>();
		Map<MappingKey<?>, MappingImplementor<?>> mappings = new HashMap<>();

		try {
			frozen = true;

			if ( beanResolver == null ) {
				beanResolver = new ReflectionBeanResolver();
			}

			BeanProvider beanProvider = new BeanProviderImpl( beanResolver );
			ServiceManager serviceManager = new ServiceManagerImpl( beanProvider );
			BuildContext buildContext = new BuildContextImpl( serviceManager );

			ConfigurationPropertySource propertySource;
			if ( !overriddenProperties.isEmpty() ) {
				propertySource = ConfigurationPropertySource.fromProperties( overriddenProperties )
						.withFallback( mainPropertySource );
			}
			else {
				propertySource = mainPropertySource;
			}

			indexManagerBuildingStateHolder = new IndexManagerBuildingStateHolder( buildContext, propertySource );

			// First phase: collect configuration for all mappings
			List<MappingConfiguration<?, ?>> mappingConfigurations = new ArrayList<>();
			for ( MappingInitiator initiator : mappingInitiators ) {
				MappingConfiguration<?, ?> configuration =
						new MappingConfiguration<>( buildContext, propertySource, initiator );
				mappingConfigurations.add( configuration );
				configuration.collect();
			}

			// Second phase: create mappers and their backing index managers
			for ( MappingConfiguration<?, ?> configuration : mappingConfigurations ) {
				configuration.createAndAddMapper( mappers, indexManagerBuildingStateHolder );
			}

			// Third phase: create mappings
			mappers.forEach( (mappingKey, mapper) -> {
				MappingImplementor<?> mapping = mapper.build();
				mappings.put( mappingKey, mapping );
			} );

			builtResult = new SearchMappingRepositoryImpl(
					beanResolver,
					mappings,
					indexManagerBuildingStateHolder.getBackendsByName(),
					indexManagerBuildingStateHolder.getIndexManagersByName()
			);
		}
		catch (RuntimeException e) {
			SuppressingCloser closer = new SuppressingCloser( e );
			// Close the mappers and mappings created so far before aborting
			closer.pushAll( MappingImplementor::close, mappings.values() );
			closer.pushAll( Mapper::closeOnFailure, mappers.values() );
			// Close the resources contained in the index manager building state before aborting
			closer.pushAll( holder -> holder.closeOnFailure( closer ), indexManagerBuildingStateHolder );
			// Close the bean resolver before aborting
			closer.pushAll( BeanResolver::close, beanResolver );
			throw e;
		}

		return builtResult;
	}

	@Override
	public SearchMappingRepository getBuiltResult() {
		return builtResult;
	}

	private static class MappingConfiguration<C, M> {
		private final BuildContext buildContext;
		private final ConfigurationPropertySource propertySource;

		private final MappingInitiator<C, M> mappingInitiator;

		// Use a LinkedHashMap for deterministic iteration
		private final Map<MappableTypeModel, TypeMappingContribution<C>> contributionByType = new LinkedHashMap<>();
		private final List<TypeMetadataDiscoverer<C>> metadataDiscoverers = new ArrayList<>();

		private final Set<MappableTypeModel> typesSubmittedToDiscoverers = new HashSet<>();

		MappingConfiguration(BuildContext buildContext, ConfigurationPropertySource propertySource,
				MappingInitiator<C, M> mappingInitiator) {
			this.buildContext = buildContext;
			this.propertySource = propertySource;
			this.mappingInitiator = mappingInitiator;
		}

		void collect() {
			mappingInitiator.configure( buildContext, propertySource, new ConfigurationCollector() );
		}

		/*
		 * Note that the mapper map is passed as a parameter, instead of returning a mapper,
		 * so that even in case of failure, the caller can access the mappers built so far.
		 * Then the caller can close all mappers as necessary.
		 */
		void createAndAddMapper(Map<MappingKey<?>, Mapper<?>> mappers,
				IndexManagerBuildingStateHolder indexManagerBuildingStateHolder) {
			MappingKey<M> mappingKey = mappingInitiator.getMappingKey();
			if ( mappers.containsKey( mappingKey ) ) {
				throw new SearchException(
						"Found two mapping initiators using the same key."
						+ " There is a bug in the mapper integration."
				);
			}

			ContributorProvider contributorProvider = new ContributorProvider();
			Mapper<M> mapper = mappingInitiator.createMapper( buildContext, propertySource, contributorProvider );
			mappers.put( mappingInitiator.getMappingKey(), mapper );

			Set<MappableTypeModel> potentiallyMappedToIndexTypes = new LinkedHashSet<>(
					contributionByType.keySet() );
			for ( MappableTypeModel typeModel : potentiallyMappedToIndexTypes ) {
				TypeMappingContribution<C> contribution = contributionByType.get( typeModel );
				String indexName = contribution.getIndexName();
				if ( indexName != null ) {
					mapper.addIndexed(
							typeModel,
							indexManagerBuildingStateHolder
									.startBuilding( indexName, mapper.isMultiTenancyEnabled() )
					);
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

		private class ConfigurationCollector implements MappingConfigurationCollector<C> {
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
		}

		private class ContributorProvider implements TypeMetadataContributorProvider<C> {
			@Override
			public void forEach(MappableTypeModel typeModel, Consumer<C> contributorConsumer) {
				typeModel.getDescendingSuperTypes()
						.map( MappingConfiguration.this::getContributionIncludingAutomaticallyDiscovered )
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
				throw new SearchException( "Type '" + typeModel + "' mapped to multiple indexes: '"
						+ this.indexName + "', '" + indexName + "'." );
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
