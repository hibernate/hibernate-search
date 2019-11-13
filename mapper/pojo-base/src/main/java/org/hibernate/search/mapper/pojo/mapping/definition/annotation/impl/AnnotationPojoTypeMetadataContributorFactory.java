/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl;

import java.util.Optional;

import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.AnnotationProcessorProvider;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.PropertyAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.TypeAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl.TypeMappingStepImpl;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;

class AnnotationPojoTypeMetadataContributorFactory {

	private final AnnotationProcessorProvider annotationProcessorProvider;

	AnnotationPojoTypeMetadataContributorFactory(AnnotationProcessorProvider annotationProcessorProvider) {
		this.annotationProcessorProvider = annotationProcessorProvider;
	}

	public Optional<PojoTypeMetadataContributor> createIfAnnotated(PojoRawTypeModel<?> typeModel) {
		// Create a programmatic type mapping object
		TypeMappingStepImpl typeMappingContext = new TypeMappingStepImpl( typeModel );

		// Process annotations and add metadata to the type mapping
		boolean processedTypeLevelAnnotation = processTypeLevelAnnotations( typeMappingContext, typeModel );
		boolean processedPropertyLevelAnnotation = typeModel.getDeclaredProperties()
				.map( propertyModel -> processPropertyLevelAnnotations( typeMappingContext, typeModel, propertyModel ) )
				.reduce( (processedAnnotationHere, processedAnnotationThere) -> processedAnnotationHere || processedAnnotationThere )
				.orElse( false );

		if ( !processedTypeLevelAnnotation && !processedPropertyLevelAnnotation ) {
			// No annotation was processed, this type mapping is pointless.
			return Optional.empty();
		}

		// Return the resulting mapping, which includes all the metadata extracted from annotations
		return Optional.of( typeMappingContext );
	}

	private boolean processTypeLevelAnnotations(TypeMappingStepImpl typeMappingContext, PojoRawTypeModel<?> typeModel) {
		boolean processedAtLeastOneAnnotation = false;
		for ( TypeAnnotationProcessor<?> processor : annotationProcessorProvider.getTypeAnnotationProcessors() ) {
			if ( processor.process( typeMappingContext, typeModel ) ) {
				processedAtLeastOneAnnotation = true;
			}
		}
		return processedAtLeastOneAnnotation;
	}

	private boolean processPropertyLevelAnnotations(TypeMappingStepImpl typeMappingContext,
			PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel) {
		String propertyName = propertyModel.getName();
		PropertyMappingStep mappingContext = typeMappingContext.property( propertyName );
		boolean processedAtLeastOneAnnotation = false;
		for ( PropertyAnnotationProcessor<?> processor : annotationProcessorProvider.getPropertyAnnotationProcessors() ) {
			if ( processor.process( mappingContext, typeModel, propertyModel ) ) {
				processedAtLeastOneAnnotation = true;
			}
		}
		return processedAtLeastOneAnnotation;
	}

}
