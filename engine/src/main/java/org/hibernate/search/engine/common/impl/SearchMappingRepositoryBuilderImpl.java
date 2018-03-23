/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

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
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.SearchException;

public class SearchMappingRepositoryBuilderImpl implements SearchMappingRepositoryBuilder {

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
	public SearchMappingRepositoryBuilder addMapping(MetadataContributor mappingContributor) {
		contributors.add( mappingContributor );
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

		IndexManagerBuildingStateHolder indexManagerBuildingStateProvider =
				new IndexManagerBuildingStateHolder( buildContext, propertySource );
		// TODO close the holder (which will close the backends) if anything fails after this

		MetadataCollectorImpl metadataCollector = new MetadataCollectorImpl();
		contributors.forEach( c -> c.contribute( buildContext, metadataCollector ) );

		Map<MappingKey<?>, Mapper<?>> mappers =
				metadataCollector.createMappers( buildContext, propertySource, indexManagerBuildingStateProvider );

		Map<MappingKey<?>, MappingImplementor<?>> mappings = new HashMap<>();
		// TODO close the mappings created so far if anything fails after this
		mappers.forEach( (mappingKey, mapper) -> {
			MappingImplementor<?> mapping = mapper.build();
			mappings.put( mappingKey, mapping );
		} );

		builtResult = new SearchMappingRepositoryImpl(
				mappings, indexManagerBuildingStateProvider.getBackendsByName()
		);
		return builtResult;
	}

	@Override
	public SearchMappingRepository getBuiltResult() {
		return builtResult;
	}

	private static class MetadataCollectorImpl implements MetadataCollector {
		private final Map<MappingKey<?>, MapperContribution<?, ?>> contributionByMappingKey = new HashMap<>();
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

		Map<MappingKey<?>, Mapper<?>> createMappers(
				BuildContext buildContext, ConfigurationPropertySource propertySource,
				IndexManagerBuildingStateHolder indexManagerBuildingStateProvider) {
			frozen = true;
			Map<MappingKey<?>, Mapper<?>> mappers = new HashMap<>();
			contributionByMappingKey.forEach( (mappingKey, contribution) -> {
				Mapper<?> mapper = contribution.preBuild( buildContext, propertySource, indexManagerBuildingStateProvider );
				mappers.put( mappingKey, mapper );
			} );
			return mappers;
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
		private final Map<MappableTypeModel, TypeMappingContribution<C>> contributionByType = new LinkedHashMap<>();
		private final List<TypeMetadataDiscoverer<C>> metadataDiscoverers = new ArrayList<>();
		private final Set<MappableTypeModel> typesSubmittedToDiscoverers = new HashSet<>();

		MapperContribution(MapperFactory<C, M> mapperFactory) {
			this.mapperFactory = mapperFactory;
		}

		public void mapToIndex(MappableTypeModel typeModel, String indexName) {
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

			Set<MappableTypeModel> potentiallyMappedToIndexTypes = new LinkedHashSet<>( contributionByType.keySet() );
			for ( MappableTypeModel typeModel : potentiallyMappedToIndexTypes ) {
				TypeMappingContribution<C> contribution = contributionByType.get( typeModel );
				String indexName = contribution.getIndexName();
				if ( indexName != null ) {
					mapper.addIndexed(
							typeModel,
							indexManagerBuildingStateHolder.startBuilding( indexName )
					);
				}
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
				return Collections.unmodifiableSet( new HashSet<>( contributionByType.keySet() ) );
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
