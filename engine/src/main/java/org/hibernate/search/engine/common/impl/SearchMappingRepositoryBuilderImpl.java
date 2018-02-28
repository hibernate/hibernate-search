/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataContributorProvider;
import org.hibernate.search.engine.mapper.mapping.spi.MappingKey;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MapperFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataCollector;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.util.AssertionFailure;
import org.hibernate.search.util.SearchException;


/**
 * @author Yoann Rodiere
 */
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

		Map<MappingKey<?>, Mapper<?, ?>> mappers =
				metadataCollector.createMappers( buildContext, propertySource, indexManagerBuildingStateProvider );

		Map<MappingKey<?>, MappingImplementor> mappings = new HashMap<>();
		// TODO close the mappings created so far if anything fails after this
		mappers.forEach( (mappingKey, mapper) -> {
			MappingImplementor mapping = mapper.build();
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
		public <C extends TypeMetadataContributor> void collect(MapperFactory<C, ?> mapperFactory,
				MappableTypeModel typeModel, String indexName, C contributor) {
			// Adding new contributors to already known mappings is fine, see MapperContribution
			if ( frozen && !contributionByMappingKey.containsKey( mapperFactory ) ) {
				throw new AssertionFailure(
						"Attempt to add a mapping contribution for a new mapper factory"
								+ " after Hibernate Search has started to build the mappings."
								+ " There is a bug in the mapper factory implementation."
								+ " Mapper factory: " + mapperFactory + ". Type model: " + typeModel + "."
				);
			}
			@SuppressWarnings("unchecked")
			MapperContribution<C, ?> contribution = (MapperContribution<C, ?>)
					contributionByMappingKey.computeIfAbsent( mapperFactory, ignored -> new MapperContribution<>( mapperFactory ));
			contribution.update( typeModel, indexName, contributor );
		}

		Map<MappingKey<?>, Mapper<?, ?>> createMappers(
				BuildContext buildContext, ConfigurationPropertySource propertySource,
				IndexManagerBuildingStateHolder indexManagerBuildingStateProvider) {
			frozen = true;
			Map<MappingKey<?>, Mapper<?, ?>> mappers = new HashMap<>();
			contributionByMappingKey.forEach( (mappingKey, contribution) -> {
				Mapper<?, ?> mapper = contribution.preBuild( buildContext, propertySource, indexManagerBuildingStateProvider );
				mappers.put( mappingKey, mapper );
			} );
			return mappers;
		}
	}

	private static class MapperContribution<C extends TypeMetadataContributor, M extends MappingImplementor> {

		private final MapperFactory<C, M> mapperFactory;
		private final Set<MappableTypeModel> typesLeftToProcess = new LinkedHashSet<>();
		private final Set<MappableTypeModel> typesBeingProcessed = new LinkedHashSet<>();
		private final Set<MappableTypeModel> freezedTypes = new HashSet<>();
		private final Map<MappableTypeModel, TypeMappingContribution<C>> contributionByType = new HashMap<>();

		MapperContribution(MapperFactory<C, M> mapperFactory) {
			this.mapperFactory = mapperFactory;
		}

		public void update(MappableTypeModel typeModel, String indexName, C contributor) {
			if ( freezedTypes.contains( typeModel ) ) {
				throw new AssertionFailure(
						"Attempt to add a mapping contribution for a type that has already been mapped"
								+ " or is being mapped. There is a bug in the mapper implementation."
								+ " Mapper factory: " + mapperFactory + ". Type model: " + typeModel + "."
				);
			}
			typesLeftToProcess.add( typeModel );
			contributionByType.computeIfAbsent( typeModel, TypeMappingContribution::new )
					.update( indexName, contributor );
		}

		public Mapper<C, M> preBuild(BuildContext buildContext, ConfigurationPropertySource propertySource,
				IndexManagerBuildingStateHolder indexManagerBuildingStateHolder) {
			ContributorProvider contributorProvider = new ContributorProvider();
			Mapper<C, M> mapper = mapperFactory.createMapper( buildContext, propertySource );

			/*
			 * We have to loop, and copy the set of types before starting processing,
			 * because we allow mappers to add new contributions to new types
			 * when we call org.hibernate.search.engine.mapper.mapping.building.spi.Mapper.addIndexed.
			 * This is necessary to allow annotation-based mapping,
			 * which may discover new embedded types while mapping a type.
			 *
			 * We do not, however, allow new contributions to types already encountered in this method
			 * (the "freezed" types).
			 */
			while ( !typesLeftToProcess.isEmpty() ) {
				typesBeingProcessed.clear();
				typesBeingProcessed.addAll( typesLeftToProcess );
				typesLeftToProcess.clear();
				freezedTypes.addAll( typesBeingProcessed );

				for ( MappableTypeModel typeModel : typesBeingProcessed ) {
					Optional<String> indexNameOptional = typeModel.getAscendingSuperTypes()
							.map( contributionByType::get )
							.filter( Objects::nonNull )
							.map( TypeMappingContribution::getIndexName )
							.filter( Objects::nonNull )
							.findFirst();
					if ( indexNameOptional.isPresent() ) {
						String indexName = indexNameOptional.get();
						mapper.addIndexed(
								typeModel,
								indexManagerBuildingStateHolder.startBuilding( indexName ),
								contributorProvider
						);
					}
				}
			}
			return mapper;
		}

		private class ContributorProvider implements TypeMetadataContributorProvider<C> {
			private C currentContributor = null;

			@Override
			public void forEach(MappableTypeModel typeModel, Consumer<C> contributorConsumer) {
				if ( currentContributor != null ) {
					currentContributor.beforeNestedContributions( typeModel );
				}
				C previousContributor = currentContributor;
				typeModel.getDescendingSuperTypes()
						.map( contributionByType::get )
						.filter( Objects::nonNull )
						.flatMap( TypeMappingContribution::getContributors )
						.forEach( contributor -> {
							currentContributor = contributor;
							try {
								contributorConsumer.accept( contributor );
							}
							finally {
								currentContributor = previousContributor;
							}
						} );
			}
		}
	}

	private static class TypeMappingContribution<C> {
		private final MappableTypeModel typeModel;
		private String indexName;
		private final List<C> contributors = new ArrayList<>();

		public TypeMappingContribution(MappableTypeModel typeModel) {
			this.typeModel = typeModel;
		}

		public String getIndexName() {
			return indexName;
		}

		public void update(String indexName, C contributor) {
			if ( indexName != null && !indexName.isEmpty() ) {
				if ( this.indexName != null ) {
					throw new SearchException( "Type '" + typeModel + "' mapped to multiple indexes: '"
							+ this.indexName + "', '" + indexName + "'." );
				}
				this.indexName = indexName;
			}
			this.contributors.add( contributor );
		}

		public Stream<C> getContributors() {
			return contributors.stream();
		}
	}
}
