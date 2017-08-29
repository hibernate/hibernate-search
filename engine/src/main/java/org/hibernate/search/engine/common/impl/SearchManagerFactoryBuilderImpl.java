/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.bridge.impl.BridgeFactory;
import org.hibernate.search.engine.bridge.impl.BridgeReferenceResolver;
import org.hibernate.search.engine.common.SearchManagerBuilder;
import org.hibernate.search.engine.common.SearchManagerFactory;
import org.hibernate.search.engine.common.SearchManagerFactoryBuilder;
import org.hibernate.search.engine.common.spi.BeanResolver;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.common.spi.ServiceManager;
import org.hibernate.search.engine.mapper.mapping.MappingType;
import org.hibernate.search.engine.mapper.mapping.building.spi.MapperImplementor;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingBuilder;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataContributor;
import org.hibernate.search.engine.mapper.mapping.spi.Mapping;
import org.hibernate.search.engine.mapper.model.spi.IndexableTypeOrdering;
import org.hibernate.search.engine.mapper.model.spi.IndexedTypeIdentifier;
import org.hibernate.search.util.SearchException;


/**
 * @author Yoann Rodiere
 */
public class SearchManagerFactoryBuilderImpl implements SearchManagerFactoryBuilder {

	private final Properties properties = new Properties();
	private final Map<MappingType<?, ?>, MapperContribution<?, ?>> contributionByMapper = new HashMap<>();

	@Override
	public SearchManagerFactoryBuilder setProperty(String name, String value) {
		this.properties.setProperty( name, value );
		return this;
	}

	@Override
	public SearchManagerFactoryBuilder setProperties(Properties properties) {
		this.properties.putAll( properties );
		return this;
	}

	@Override
	public SearchManagerFactoryBuilder addMapping(MetadataContributor mappingContributor) {
		mappingContributor.contribute( this::collectMappingContribution );
		return this;
	}

	private <C> void collectMappingContribution(MapperImplementor<C, ?, ?> mapperImpl, IndexedTypeIdentifier typeId, String indexName,
			C contributor) {
		@SuppressWarnings("unchecked")
		MapperContribution<C, ?> collector = (MapperContribution<C, ?>)
				contributionByMapper.computeIfAbsent( mapperImpl, ignored -> new MapperContribution<>( mapperImpl ));
		collector.update( typeId, indexName, contributor );
	}

	@Override
	public SearchManagerFactory build() {
		BeanResolver beanResolver = new ReflectionBeanResolver();
		ServiceManager serviceManager = new ServiceManagerImpl( beanResolver );
		BuildContext buildContext = new BuildContextImpl( serviceManager );
		BridgeFactory bridgeFactory = new BridgeFactory( buildContext, beanResolver );
		BridgeReferenceResolver bridgeReferenceResolver = new BridgeReferenceResolver();
		/*
		 * TODO add an option in the builder to mask properties, but don't enable it by default
		 * Rationale: we can easily switch this option on in the Hibernate ORM integration,
		 * and other integrations may not want it to be enabled, or may want a custom mask
		 * (see Infinispan for instance).
		 */
		// Properties maskedProperties = new MaskedProperty( properties, "hibernate.search" );
		IndexManagerBuildingStateHolder indexManagerBuildingStateProvider =
				new IndexManagerBuildingStateHolder( buildContext, properties,
						bridgeFactory, bridgeReferenceResolver );
		// TODO close the holder (which will close the backends if anything fails after this

		Map<MappingType<?, ?>, MappingBuilder<?, ?>> mappingBuilders = new HashMap<>();
		contributionByMapper.forEach( (mapper, contribution) -> {
			MappingBuilder<?, ?> builder = contribution.preBuild( indexManagerBuildingStateProvider );
			mappingBuilders.put( mapper, builder );
		} );

		Map<String, IndexManager<?>> indexManagers = indexManagerBuildingStateProvider.build();
		// TODO close the index managers if anything fails after this

		Map<MappingType<?, ?>, Mapping<?>> mappings = new HashMap<>();
		// TODO close the mappings created so far if anything fails after this
		mappingBuilders.forEach( (mapper, builder) -> {
			Mapping<?> mapping = builder.build();
			mappings.put( mapper, mapping );
		} );

		return new SearchManagerFactoryImpl( mappings );
	}

	// Note we need to delay type mapping contributions so as to only start creating backends etc. when build() is called
	private static class MapperContribution<C, B extends SearchManagerBuilder<?>> {

		private final MapperImplementor<C, ?, B> mapper;
		private final Map<IndexedTypeIdentifier, TypeMappingContribution<C>> contributionByType = new HashMap<>();

		public MapperContribution(MapperImplementor<C, ?, B> mapper) {
			this.mapper = mapper;
		}

		public void update(IndexedTypeIdentifier typeId, String indexName, C contributor) {
			contributionByType.computeIfAbsent( typeId, TypeMappingContribution::new )
					.update( indexName, contributor );
		}

		public MappingBuilder<C, B> preBuild(IndexManagerBuildingStateHolder indexManagerBuildingStateHolder) {
			MappingBuilder<C, B> builder = mapper.createBuilder();
			IndexableTypeOrdering typeOrdering = mapper.getTypeOrdering();
			for ( IndexedTypeIdentifier mappedType : contributionByType.keySet() ) {
				Optional<String> indexNameOptional = typeOrdering.getAscendingSuperTypes( mappedType )
						.stream()
						.map( contributionByType::get )
						.filter( Objects::nonNull )
						.map( TypeMappingContribution::getIndexName )
						.filter( Objects::nonNull )
						.findFirst();
				if ( indexNameOptional.isPresent() ) {
					String indexName = indexNameOptional.get();
					builder.addIndexed(
							mappedType,
							indexManagerBuildingStateHolder.startBuilding( indexName, typeOrdering ),
							type2 -> getContributors( typeOrdering, type2 )
							);
				}
			}
			return builder;
		}

		private Stream<C> getContributors(IndexableTypeOrdering typeOrdering, IndexedTypeIdentifier typeId) {
			return typeOrdering.getDescendingSuperTypes( typeId )
					.stream()
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
