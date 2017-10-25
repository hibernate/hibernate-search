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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.hibernate.search.engine.bridge.impl.BridgeFactory;
import org.hibernate.search.engine.bridge.impl.BridgeReferenceResolver;
import org.hibernate.search.engine.cfg.spi.ConfigurationPropertySource;
import org.hibernate.search.engine.common.SearchMappingRepository;
import org.hibernate.search.engine.common.SearchMappingRepositoryBuilder;
import org.hibernate.search.engine.common.spi.BeanResolver;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.common.spi.ServiceManager;
import org.hibernate.search.engine.mapper.mapping.MappingKey;
import org.hibernate.search.engine.mapper.mapping.building.spi.Mapper;
import org.hibernate.search.engine.mapper.mapping.building.spi.MapperFactory;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataCollector;
import org.hibernate.search.engine.mapper.mapping.spi.MappingImplementor;
import org.hibernate.search.engine.mapper.model.spi.IndexableTypeOrdering;
import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;
import org.hibernate.search.util.SearchException;


/**
 * @author Yoann Rodiere
 */
public class SearchMappingRepositoryBuilderImpl implements SearchMappingRepositoryBuilder {

	private final ConfigurationPropertySource mainPropertySource;
	private final Properties overriddenProperties = new Properties();
	private final Collection<MetadataContributor> contributors = new ArrayList<>();

	private SearchMappingRepository builtResult;

	public SearchMappingRepositoryBuilderImpl(ConfigurationPropertySource mainPropertySource) {
		this.mainPropertySource = mainPropertySource;
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
		BeanResolver beanResolver = new ReflectionBeanResolver();
		ServiceManager serviceManager = new ServiceManagerImpl( beanResolver );
		BuildContext buildContext = new BuildContextImpl( serviceManager );
		BridgeFactory bridgeFactory = new BridgeFactory( buildContext, beanResolver );
		BridgeReferenceResolver bridgeReferenceResolver = new BridgeReferenceResolver();

		ConfigurationPropertySource propertySource;
		if ( !overriddenProperties.isEmpty() ) {
			propertySource = ConfigurationPropertySource.fromProperties( overriddenProperties )
					.withFallback( mainPropertySource );
		}
		else {
			propertySource = mainPropertySource;
		}

		IndexManagerBuildingStateHolder indexManagerBuildingStateProvider =
				new IndexManagerBuildingStateHolder( buildContext, propertySource,
						bridgeFactory, bridgeReferenceResolver );
		// TODO close the holder (which will close the backends) if anything fails after this

		TypeMetadataCollectorImpl metadataCollector = new TypeMetadataCollectorImpl();
		contributors.forEach( c -> c.contribute( metadataCollector ) );

		Map<MappingKey<?>, Mapper<?, ?>> mappers =
				metadataCollector.createMappers( indexManagerBuildingStateProvider );

		Map<MappingKey<?>, MappingImplementor> mappings = new HashMap<>();
		// TODO close the mappings created so far if anything fails after this
		mappers.forEach( (mappingKey, mapper) -> {
			MappingImplementor mapping = mapper.build();
			mappings.put( mappingKey, mapping );
		} );

		builtResult = new SearchMappingRepositoryImpl( mappings );
		return builtResult;
	}

	@Override
	public SearchMappingRepository getBuiltResult() {
		return builtResult;
	}

	private static class TypeMetadataCollectorImpl implements TypeMetadataCollector {
		private final Map<MappingKey<?>, MapperContribution<?, ?>> contributionByMappingKey = new HashMap<>();

		@Override
		public <C> void collect(MapperFactory<C, ?> mapperFactory, IndexedTypeIdentifier typeId,
				String indexName, C contributor) {
			@SuppressWarnings("unchecked")
			MapperContribution<C, ?> contribution = (MapperContribution<C, ?>)
					contributionByMappingKey.computeIfAbsent( mapperFactory, ignored -> new MapperContribution<>( mapperFactory ));
			contribution.update( typeId, indexName, contributor );
		}

		public Map<MappingKey<?>, Mapper<?, ?>> createMappers(
				IndexManagerBuildingStateHolder indexManagerBuildingStateProvider) {
			Map<MappingKey<?>, Mapper<?, ?>> mappers = new HashMap<>();
			contributionByMappingKey.forEach( (mappingKey, contribution) -> {
				Mapper<?, ?> mapper = contribution.preBuild( indexManagerBuildingStateProvider );
				mappers.put( mappingKey, mapper );
			} );
			return mappers;
		}
	}

	private static class MapperContribution<C, M extends MappingImplementor> {

		private final MapperFactory<C, M> mapperFactory;
		private final Map<IndexedTypeIdentifier, TypeMappingContribution<C>> contributionByType = new HashMap<>();

		public MapperContribution(MapperFactory<C, M> mapperFactory) {
			this.mapperFactory = mapperFactory;
		}

		public void update(IndexedTypeIdentifier typeId, String indexName, C contributor) {
			contributionByType.computeIfAbsent( typeId, TypeMappingContribution::new )
					.update( indexName, contributor );
		}

		public Mapper<C, M> preBuild(IndexManagerBuildingStateHolder indexManagerBuildingStateHolder) {
			Mapper<C, M> mapper = mapperFactory.createMapper();
			IndexableTypeOrdering typeOrdering = mapperFactory.getTypeOrdering();
			for ( IndexedTypeIdentifier mappedType : contributionByType.keySet() ) {
				Optional<String> indexNameOptional = typeOrdering.getAscendingSuperTypes( mappedType )
						.map( contributionByType::get )
						.filter( Objects::nonNull )
						.map( TypeMappingContribution::getIndexName )
						.filter( Objects::nonNull )
						.findFirst();
				if ( indexNameOptional.isPresent() ) {
					String indexName = indexNameOptional.get();
					mapper.addIndexed(
							mappedType,
							indexManagerBuildingStateHolder.startBuilding( indexName, typeOrdering ),
							type2 -> getContributors( typeOrdering, type2 )
							);
				}
			}
			return mapper;
		}

		private Stream<C> getContributors(IndexableTypeOrdering typeOrdering, IndexedTypeIdentifier typeId) {
			return typeOrdering.getDescendingSuperTypes( typeId )
					.map( contributionByType::get )
					.filter( Objects::nonNull )
					.flatMap( TypeMappingContribution::getContributors );
		}
	}

	private static class TypeMappingContribution<C> {
		private final IndexedTypeIdentifier typeId;
		private String indexName;
		private final List<C> contributors = new ArrayList<>();

		public TypeMappingContribution(IndexedTypeIdentifier typeId) {
			super();
			this.typeId = typeId;
		}

		public String getIndexName() {
			return indexName;
		}

		public void update(String indexName, C contributor) {
			if ( indexName != null && !indexName.isEmpty() ) {
				if ( this.indexName != null ) {
					throw new SearchException( "Type '" + typeId + "' mapped to multiple indexes: '"
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
