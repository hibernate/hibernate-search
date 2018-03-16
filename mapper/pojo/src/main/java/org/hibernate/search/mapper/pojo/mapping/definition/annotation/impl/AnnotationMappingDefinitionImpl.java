/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.common.spi.BeanResolver;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.MetadataContributor;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataDiscoverer;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoMapperFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

/**
 * @author Yoann Rodiere
 */
public class AnnotationMappingDefinitionImpl implements AnnotationMappingDefinition, MetadataContributor {

	private final PojoMapperFactory<?> mapperFactory;
	private final PojoBootstrapIntrospector introspector;
	private final Set<Class<?>> annotatedTypes = new HashSet<>();
	private final boolean annotatedTypeDiscoveryEnabled;

	public AnnotationMappingDefinitionImpl(PojoMapperFactory<?> mapperFactory, PojoBootstrapIntrospector introspector,
			boolean annotatedTypeDiscoveryEnabled) {
		this.mapperFactory = mapperFactory;
		this.introspector = introspector;
		this.annotatedTypeDiscoveryEnabled = annotatedTypeDiscoveryEnabled;
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
		BeanResolver beanResolver = buildContext.getServiceManager().getBeanResolver();

		/*
		 * For types that were explicitly requested for annotation scanning and their supertypes,
		 * map the types to indexes if necessary, and add a metadata contributor.
		 */
		Set<PojoRawTypeModel<?>> alreadyContributedTypes = new HashSet<>();
		annotatedTypes.stream()
				.map( introspector::getTypeModel )
				/*
				 * Take super types into account
				 * Note: the order of super types (ascending or descending) does not matter here.
				 */
				.flatMap( PojoRawTypeModel::getAscendingSuperTypes )
				// Ignore types that were already contributed
				.filter( alreadyContributedTypes::add )
				// TODO filter out standard Java types, e.g. Object or standard Java interfaces such as Serializable?
				.forEach( typeModel -> {
					Optional<Indexed> indexedAnnotation = typeModel.getAnnotationByType( Indexed.class );
					if ( indexedAnnotation.isPresent() ) {
						collector.mapToIndex( mapperFactory, typeModel, indexedAnnotation.get().index() );
					}

					PojoTypeMetadataContributor contributor =
							new AnnotationPojoTypeMetadataContributorImpl( beanResolver, typeModel );

					collector.collectContributor( mapperFactory, typeModel, contributor );
				} );

		/*
		 * If automatic discovery of annotated types is enabled,
		 * also add a discoverer for new types (e.g. types encountered in an @IndexedEmbedded).
		 */
		if ( annotatedTypeDiscoveryEnabled ) {
			AnnotationTypeMetadataDiscoverer discoverer =
					new AnnotationTypeMetadataDiscoverer( beanResolver, alreadyContributedTypes );
			collector.collectDiscoverer( mapperFactory, discoverer );
		}
	}

	/**
	 * A type metadata discoverer that will provide annotation-based metadata
	 * for types that were not explicitly requested .
	 */
	private static class AnnotationTypeMetadataDiscoverer implements TypeMetadataDiscoverer<PojoTypeMetadataContributor> {
		private final BeanResolver beanResolver;
		private final Set<PojoRawTypeModel<?>> alreadyContributedTypes;

		AnnotationTypeMetadataDiscoverer(BeanResolver beanResolver, Set<PojoRawTypeModel<?>> alreadyContributedTypes) {
			this.beanResolver = beanResolver;
			this.alreadyContributedTypes = alreadyContributedTypes;
		}

		@Override
		public Optional<PojoTypeMetadataContributor> discover(MappableTypeModel typeModel) {
			PojoRawTypeModel<?> pojoTypeModel = (PojoRawTypeModel<?>) typeModel;
			/*
			 * Take care of not adding duplicate contributors: this could lead to mapping errors,
			 * for instance a field being declared twice.
			 */
			boolean neverContributed = alreadyContributedTypes.add( pojoTypeModel );
			if ( neverContributed ) {
				// TODO filter out standard Java types, e.g. Object or standard Java interfaces such as Serializable?
				return Optional.of( new AnnotationPojoTypeMetadataContributorImpl( beanResolver, pojoTypeModel ) );
			}
			else {
				return Optional.empty();
			}
		}
	}
}
