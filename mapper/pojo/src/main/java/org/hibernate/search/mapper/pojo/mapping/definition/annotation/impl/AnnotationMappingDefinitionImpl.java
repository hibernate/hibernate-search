/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.spi.BeanProvider;
import org.hibernate.search.engine.common.spi.BuildContext;
import org.hibernate.search.engine.mapper.mapping.building.spi.MappingConfigurationCollector;
import org.hibernate.search.engine.mapper.mapping.building.spi.TypeMetadataDiscoverer;
import org.hibernate.search.engine.mapper.model.spi.MappableTypeModel;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationMappingDefinition;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContributor;
import org.hibernate.search.mapper.pojo.model.spi.PojoBootstrapIntrospector;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

public class AnnotationMappingDefinitionImpl implements AnnotationMappingDefinition,
		PojoMappingConfigurationContributor {

	private final PojoBootstrapIntrospector introspector;
	// Use a LinkedHashSet for deterministic iteration
	private final Set<Class<?>> annotatedTypes = new LinkedHashSet<>();
	private final boolean annotatedTypeDiscoveryEnabled;

	public AnnotationMappingDefinitionImpl(PojoBootstrapIntrospector introspector,
			boolean annotatedTypeDiscoveryEnabled) {
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
	public void configure(BuildContext buildContext, ConfigurationPropertySource propertySource,
			MappingConfigurationCollector<PojoTypeMetadataContributor> collector) {
		BeanProvider beanProvider = buildContext.getServiceManager().getBeanProvider();

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
						collector.mapToIndex( typeModel, indexedAnnotation.get().index() );
					}

					PojoTypeMetadataContributor contributor =
							new AnnotationPojoTypeMetadataContributorImpl( beanProvider, typeModel );

					collector.collectContributor( typeModel, contributor );
				} );

		/*
		 * If automatic discovery of annotated types is enabled,
		 * also add a discoverer for new types (e.g. types encountered in an @IndexedEmbedded).
		 */
		if ( annotatedTypeDiscoveryEnabled ) {
			AnnotationTypeMetadataDiscoverer discoverer =
					new AnnotationTypeMetadataDiscoverer( beanProvider, alreadyContributedTypes );
			collector.collectDiscoverer( discoverer );
		}
	}

	/**
	 * A type metadata discoverer that will provide annotation-based metadata
	 * for types that were not explicitly requested .
	 */
	private static class AnnotationTypeMetadataDiscoverer implements TypeMetadataDiscoverer<PojoTypeMetadataContributor> {
		private final BeanProvider beanProvider;
		private final Set<PojoRawTypeModel<?>> alreadyContributedTypes;

		AnnotationTypeMetadataDiscoverer(BeanProvider beanProvider, Set<PojoRawTypeModel<?>> alreadyContributedTypes) {
			this.beanProvider = beanProvider;
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
				return Optional.of( new AnnotationPojoTypeMetadataContributorImpl( beanProvider, pojoTypeModel ) );
			}
			else {
				return Optional.empty();
			}
		}
	}
}
