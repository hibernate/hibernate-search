/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.search.engine.common.spi.BeanResolver;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataContributor;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeModelCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.model.spi.PojoIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;

/**
 * @author Yoann Rodiere
 */
public class AnnotationMappingDefinitionImpl implements AnnotationMappingDefinition, MetadataContributor {

	private final PojoMapperFactory<?> mapperFactory;

	private final PojoIntrospector introspector;

	private final Set<Class<?>> annotatedTypes = new HashSet<>();

	public AnnotationMappingDefinitionImpl(PojoMapperFactory<?> mapperFactory, PojoIntrospector introspector) {
		this.mapperFactory = mapperFactory;
		this.introspector = introspector;
	}

	@Override
	public AnnotationMappingDefinition add(Class<?> annotatedType) {
		this.annotatedTypes.add( annotatedType );
		return this;
	}

	@Override
	public AnnotationMappingDefinition add(Set<Class<?>> annotatedTypes) {
		this.annotatedTypes.addAll( annotatedTypes );
		return this;
	}

	@Override
	public void contribute(BuildContext buildContext, MetadataCollector collector) {
		LazilyDiscoveringMetadataContributor contributor =
				new LazilyDiscoveringMetadataContributor( mapperFactory, buildContext, collector );
		annotatedTypes.stream()
				.map( introspector::getTypeModel )
				.forEach( typeModel -> contributor.contributeTypeAndSuperTypes( typeModel, true ) );
	}

	/**
	 * A metadata contributor that will lazily discover nested types and register the annotation mappings accordingly.
	 */
	private static class LazilyDiscoveringMetadataContributor {
		private final PojoMapperFactory<?> mapperFactory;
		private final BeanResolver beanResolver;
		private final MetadataCollector metadataCollector;
		private final Set<PojoRawTypeModel<?>> alreadyContributedTypes = new HashSet<>();

		LazilyDiscoveringMetadataContributor(PojoMapperFactory<?> mapperFactory,
				BuildContext buildContext, MetadataCollector metadataCollector) {
			this.mapperFactory = mapperFactory;
			this.beanResolver = buildContext.getServiceManager().getBeanResolver();
			this.metadataCollector = metadataCollector;
		}

		void contributeTypeAndSuperTypes(PojoRawTypeModel<?> subTypeModel, boolean mapToIndex) {
			/*
			 * Take super types into account
			 * Note: the order of super types (ascending or descending) does not matter here.
			 */
			subTypeModel.getAscendingSuperTypes()
					// Ignore types that were already contributed
					.filter( alreadyContributedTypes::add )
					// TODO filter out standard Java types, e.g. Object or standard Java interfaces such as Serializable?
					.forEach( typeModel -> {
						String indexName;
						if ( mapToIndex ) {
							indexName = typeModel.getAnnotationByType( Indexed.class )
									.map( Indexed::index ).orElse( null );
						}
						else {
							indexName = null;
						}

						PojoTypeNodeMetadataContributor contributor =
								new AnnotationPojoTypeNodeMetadataContributorImpl( beanResolver, typeModel );

						// Make sure to enable automatically discovering types whenever this contributor is called
						contributor = new LazilyDiscoveringTypeMetadataContributor( contributor );

						metadataCollector.collect( mapperFactory, typeModel, indexName, contributor );
					} );
		}

		private void discoverType(PojoTypeModel<?> typeModel) {
			PojoRawTypeModel<?> rawTypeModel = typeModel.getRawType();
			if ( !alreadyContributedTypes.contains( rawTypeModel ) ) {
				/*
				 * Do not map lazily discovered types to an index;
				 * only explicitly registered types should be mapped to an index,
				 * others should only be used in fields and indexed-embedded.
				 */
				contributeTypeAndSuperTypes( rawTypeModel, false );
			}
		}

		private class LazilyDiscoveringTypeMetadataContributor implements PojoTypeNodeMetadataContributor {
			private final PojoTypeNodeMetadataContributor delegate;

			private LazilyDiscoveringTypeMetadataContributor(PojoTypeNodeMetadataContributor delegate) {
				this.delegate = delegate;
			}

			@Override
			public void beforeNestedContributions(MappableTypeModel typeModel) {
				discoverType( (PojoTypeModel<?>) typeModel );
				delegate.beforeNestedContributions( typeModel );
			}

			@Override
			public void contributeModel(PojoTypeNodeModelCollector collector) {
				delegate.contributeModel( collector );
			}

			@Override
			public void contributeMapping(PojoTypeNodeMappingCollector collector) {
				delegate.contributeMapping( collector );
			}
		}
	}
}
