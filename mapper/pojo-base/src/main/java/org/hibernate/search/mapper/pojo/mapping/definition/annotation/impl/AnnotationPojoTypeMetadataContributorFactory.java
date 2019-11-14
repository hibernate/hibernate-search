/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.MappingAnnotationProcessorContextImpl;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.AnnotationProcessorProvider;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.PropertyAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.TypeAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl.TypeMappingStepImpl;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.reporting.impl.PojoEventContexts;
import org.hibernate.search.util.common.reflect.spi.AnnotationHelper;

class AnnotationPojoTypeMetadataContributorFactory {

	private final FailureCollector rootFailureCollector;
	private final AnnotationHelper annotationHelper;
	private final AnnotationProcessorProvider annotationProcessorProvider;
	private final MappingAnnotationProcessorContextImpl context = new MappingAnnotationProcessorContextImpl();

	AnnotationPojoTypeMetadataContributorFactory(FailureCollector rootFailureCollector, AnnotationHelper annotationHelper) {
		this.rootFailureCollector = rootFailureCollector;
		this.annotationHelper = annotationHelper;
		this.annotationProcessorProvider = new AnnotationProcessorProvider();
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
			if ( applyProcessor( processor, typeMappingContext, typeModel ) ) {
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
			if ( applyProcessor( processor, mappingContext, typeModel, propertyModel ) ) {
				processedAtLeastOneAnnotation = true;
			}
		}
		return processedAtLeastOneAnnotation;
	}

	private <A extends Annotation> boolean applyProcessor(TypeAnnotationProcessor<A> processor,
			TypeMappingStep mappingContext, PojoRawTypeModel<?> typeModel) {
		List<A> annotationList = processor.extractAnnotations(
				typeModel.getAnnotations()
						.flatMap( annotationHelper::expandRepeatableContainingAnnotation ),
				annotationHelper
		)
				.collect( Collectors.toList() );
		for ( A annotation : annotationList ) {
			tryApplyProcessor( processor, mappingContext, typeModel, annotation );
		}
		return !annotationList.isEmpty();
	}

	private <A extends Annotation> void tryApplyProcessor(TypeAnnotationProcessor<A> processor,
			TypeMappingStep mapping, PojoRawTypeModel<?> typeModel, A annotation) {
		try {
			processor.process( mapping, annotation, context );
		}
		catch (RuntimeException e) {
			rootFailureCollector
					.withContext( PojoEventContexts.fromType( typeModel ) )
					.withContext( PojoEventContexts.fromAnnotation( annotation ) )
					.add( e );
		}
	}

	private <A extends Annotation> boolean applyProcessor(PropertyAnnotationProcessor<A> processor,
			PropertyMappingStep mappingContext, PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel) {
		List<A> annotationList = processor.extractAnnotations(
				propertyModel.getAnnotations()
						.flatMap( annotationHelper::expandRepeatableContainingAnnotation ),
				annotationHelper
		)
				.collect( Collectors.toList() );
		for ( A annotation : annotationList ) {
			tryApplyProcessor( processor, mappingContext, typeModel, propertyModel, annotation );
		}
		return !annotationList.isEmpty();
	}

	private <A extends Annotation> void tryApplyProcessor(PropertyAnnotationProcessor<A> processor,
			PropertyMappingStep mapping, PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel,
			A annotation) {
		try {
			processor.process( mapping, annotation, context );
		}
		catch (RuntimeException e) {
			rootFailureCollector
					.withContext( PojoEventContexts.fromType( typeModel ) )
					.withContext( PojoEventContexts.fromPath(
							PojoModelPath.ofProperty( propertyModel.getName() )
					) )
					.withContext( PojoEventContexts.fromAnnotation( annotation ) )
					.add( e );
		}
	}

}
