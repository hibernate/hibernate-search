/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
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
import org.hibernate.search.engine.common.spi.BeanResolver;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.common.spi.ReflectionBeanResolver;
import org.hibernate.search.engine.common.spi.ServiceManager;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MapperFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataContributor;
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
	private final Collection<MetadataContributor> contributors = new ArrayList<>();

	private BeanResolver beanResolver = new ReflectionBeanResolver();
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
	public SearchMappingRepositoryBuilder addMetadataContributor(MetadataContributor contributor) {
		contributors.add( contributor );
		return this;
	}

	@Override
	public SearchMappingRepository build() {
		ServiceManager serviceManager = new ServiceManagerImpl( beanResolver );
		BuildContext buildContext = new BuildContextImpl( serviceManager );

		ConfigurationPropertySource propertySource;
		if ( !overriddenProperties.isEmpty() ) {
			propertySource = ConfigurationPropertySource.fromProperties( overriddenProperties )
					.withFallback( mainPropertySource );
		}
		else {
			propertySource = mainPropertySource;
		}

		IndexManagerBuildingStateHolder indexManagerBuildingStateHolder =
				new IndexManagerBuildingStateHolder( buildContext, propertySource );
		// Use a LinkedHashMap for deterministic iteration
		Map<MappingKey<?>, Mapper<?>> mappers = new LinkedHashMap<>();
		Map<MappingKey<?>, MappingImplementor<?>> mappings = new HashMap<>();
		try {
			MetadataCollectorImpl metadataCollector = new MetadataCollectorImpl();
			contributors.forEach( c -> c.contribute( buildContext, metadataCollector ) );

			metadataCollector.createMappers(
					mappers, buildContext, propertySource, indexManagerBuildingStateHolder
			);
			mappers.forEach( (mappingKey, mapper) -> {
				MappingImplementor<?> mapping = mapper.build();
				mappings.put( mappingKey, mapping );
			} );

			builtResult = new SearchMappingRepositoryImpl(
					mappings,
					indexManagerBuildingStateHolder.getBackendsByName(),
					indexManagerBuildingStateHolder.getIndexManagersByName()
			);
		}
		catch (RuntimeException e) {
			// Close the mappers and mappings created so far before aborting
			SuppressingCloser closer = new SuppressingCloser( e );
			closer.pushAll( MappingImplementor::close, mappings.values() );
			closer.pushAll( Mapper::closeOnFailure, mappers.values() );
			// Close the resources contained in the index manager building state before aborting
			indexManagerBuildingStateHolder.closeOnFailure( closer );
			throw e;
		}

		return builtResult;
	}

	@Override
	public SearchMappingRepository getBuiltResult() {
		return builtResult;
	}

	private static class MetadataCollectorImpl implements MetadataCollector {
		// Use a LinkedHashMap for deterministic iteration
		private final Map<MappingKey<?>, MapperContribution<?, ?>> contributionByMappingKey = new LinkedHashMap<>();
		private boolean frozen = false;

		@Override
		public void mapToIndex(MapperFactory<?, ?> mapperFactory, MappableTypeModel typeModel, String indexName) {
			checkNotFrozen( mapperFactory, typeModel );
			getOrCreateContribution( mapperFactory ).mapToIndex( typeModel, indexName );
		}

		@Override
		public <C> void collectContributor(MapperFactory<C, ?> mapperFactory,
				MappableTypeModel typeModel, C contributor) {
			checkNotFrozen( mapperFactory, typeModel );
			getOrCreateContribution( mapperFactory ).collectContributor( typeModel, contributor );
		}

		@Override
		public <C> void collectDiscoverer(MapperFactory<C, ?> mapperFactory,
				TypeMetadataDiscoverer<C> metadataDiscoverer) {
			checkNotFrozen( mapperFactory, null );
			getOrCreateContribution( mapperFactory ).collectDiscoverer( metadataDiscoverer );
		}

		/*
		 * Note that the mapper map is passed as a parameter, not returned,
		 * so that even in case of failure, the caller can access the mappers built so far.
		 * Then the caller can close all mappers as necessary.
		 */
		void createMappers(Map<MappingKey<?>, Mapper<?>> mappers,
				BuildContext buildContext, ConfigurationPropertySource propertySource,
				IndexManagerBuildingStateHolder indexManagerBuildingStateProvider) {
			frozen = true;
			contributionByMappingKey.forEach( (mappingKey, contribution) -> {
				Mapper<?> mapper = contribution.preBuild(
						buildContext, propertySource, indexManagerBuildingStateProvider
				);
				mappers.put( mappingKey, mapper );
			} );
		}

		@SuppressWarnings("unchecked")
		private <C> MapperContribution<C, ?> getOrCreateContribution(
				MapperFactory<C, ?> mapperFactory) {
			return (MapperContribution<C, ?>) contributionByMappingKey.computeIfAbsent(
					mapperFactory.getMappingKey(), ignored -> new MapperContribution<>( mapperFactory )
			);
		}

		private void checkNotFrozen(MapperFactory<?, ?> mapperFactory, MappableTypeModel typeModel) {
			if ( frozen ) {
				throw new AssertionFailure(
						"Attempt to add a mapping contribution"
						+ " after Hibernate Search has started to build the mappings."
						+ " There is a bug in the mapper factory implementation."
						+ " Mapper factory: " + mapperFactory + "."
						+ (
								typeModel == null ? ""
								: " Type model for the unexpected contribution: " + typeModel + "."
						)
				);
			}
		}
	}

	private static class MapperContribution<C, M> {

		private final MapperFactory<C, M> mapperFactory;
		// Use a LinkedHashMap for deterministic iteration
		private final Map<MappableTypeModel, TypeMappingContribution<C>> contributionByType = new LinkedHashMap<>();
		private final List<TypeMetadataDiscoverer<C>> metadataDiscoverers = new ArrayList<>();
		private final Set<MappableTypeModel> typesSubmittedToDiscoverers = new HashSet<>();

		MapperContribution(MapperFactory<C, M> mapperFactory) {
			this.mapperFactory = mapperFactory;
		}

		public void mapToIndex(MappableTypeModel typeModel, String indexName) {
			if ( typeModel.isAbstract() ) {
				throw log.cannotMapAbstractTypeToIndex( typeModel, indexName );
			}
			getOrCreateContribution( typeModel ).mapToIndex( indexName );
		}

		public void collectContributor(MappableTypeModel typeModel, C contributor) {
			getOrCreateContribution( typeModel ).collectContributor( contributor );
		}

		public void collectDiscoverer(TypeMetadataDiscoverer<C> metadataDiscoverer) {
			metadataDiscoverers.add( metadataDiscoverer );
		}

		public Mapper<M> preBuild(BuildContext buildContext, ConfigurationPropertySource propertySource,
				IndexManagerBuildingStateHolder indexManagerBuildingStateHolder) {
			ContributorProvider contributorProvider = new ContributorProvider();
			Mapper<M> mapper = mapperFactory.createMapper( buildContext, propertySource, contributorProvider );

			try {
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
			catch (RuntimeException e) {
				// Close the mapper before aborting
				new SuppressingCloser( e ).push( mapper::closeOnFailure );
				throw e;
			}

			return mapper;
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

		private class ContributorProvider implements TypeMetadataContributorProvider<C> {
			@Override
			public void forEach(MappableTypeModel typeModel, Consumer<C> contributorConsumer) {
				typeModel.getDescendingSuperTypes()
						.map( MapperContribution.this::getContributionIncludingAutomaticallyDiscovered )
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
