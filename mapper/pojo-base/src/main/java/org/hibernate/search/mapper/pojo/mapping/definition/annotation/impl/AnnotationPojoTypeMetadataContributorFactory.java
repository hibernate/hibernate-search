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

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.bean.BeanResolver;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.mapper.pojo.mapping.building.spi.PojoTypeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.ConstructorMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.ConstructorMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.AnnotationProcessorProvider;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.ConstructorMappingAnnotationProcessorContextImpl;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.MethodParameterMappingAnnotationProcessorContextImpl;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.PropertyMappingAnnotationProcessorContextImpl;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.TypeMappingAnnotationProcessorContextImpl;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ConstructorMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.impl.TypeMappingStepImpl;
import org.hibernate.search.mapper.pojo.mapping.spi.PojoMappingConfigurationContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoMethodParameterModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.reflect.spi.AnnotationHelper;

class AnnotationPojoTypeMetadataContributorFactory {

	private final FailureCollector rootFailureCollector;
	private final PojoMappingConfigurationContext configurationContext;
	private final AnnotationHelper annotationHelper;
	private final AnnotationProcessorProvider annotationProcessorProvider;

	AnnotationPojoTypeMetadataContributorFactory(BeanResolver beanResolver, FailureCollector rootFailureCollector,
			PojoMappingConfigurationContext configurationContext, AnnotationHelper annotationHelper) {
		this.rootFailureCollector = rootFailureCollector;
		this.configurationContext = configurationContext;
		this.annotationHelper = annotationHelper;
		this.annotationProcessorProvider = new AnnotationProcessorProvider( beanResolver, rootFailureCollector );
	}

	public Optional<PojoTypeMetadataContributor> createIfAnnotated(PojoRawTypeModel<?> typeModel) {
		// Create a programmatic type mapping object
		TypeMappingStepImpl typeMappingStep = new TypeMappingStepImpl( typeModel );

		// Process annotations and add metadata to the type mapping
		boolean processedAtLeastOneAnnotation = processAnnotations( typeMappingStep, typeModel );

		if ( !processedAtLeastOneAnnotation ) {
			// No annotation was processed, this type mapping is pointless.
			return Optional.empty();
		}

		// Return the resulting mapping, which includes all the metadata extracted from annotations
		return Optional.of( typeMappingStep );
	}

	private boolean processAnnotations(TypeMappingStepImpl typeMappingContext, PojoRawTypeModel<?> typeModel) {
		boolean processedAtLeastOneAnnotation = false;
		List<Annotation> annotationList = typeModel.annotations()
				.flatMap( annotationHelper::expandRepeatableContainingAnnotation )
				.collect( Collectors.toList() );
		for ( Annotation annotation : annotationList ) {
			if ( tryApplyProcessor( typeMappingContext, typeModel, annotation ) ) {
				processedAtLeastOneAnnotation = true;
			}
		}

		for ( PojoConstructorModel<?> constructorModel : typeModel.declaredConstructors() ) {
			Class<?>[] parametersJavaTypes = constructorModel.parametersJavaTypes();
			ConstructorMappingStep constructorMappingContext = typeMappingContext.constructor( parametersJavaTypes );
			if ( processConstructorLevelAnnotations( constructorMappingContext, typeModel, constructorModel ) ) {
				processedAtLeastOneAnnotation = true;
			}
			for ( PojoMethodParameterModel<?> parameterModel : constructorModel.declaredParameters() ) {
				MethodParameterMappingStep mappingContext = constructorMappingContext.parameter( parameterModel.index() );
				if ( processMethodParameterLevelAnnotations( mappingContext, constructorModel, parameterModel ) ) {
					processedAtLeastOneAnnotation = true;
				}
			}
		}

		for ( PojoPropertyModel<?> propertyModel : typeModel.declaredProperties() ) {
			String propertyName = propertyModel.name();
			PropertyMappingStep mappingContext = typeMappingContext.property( propertyName );
			if ( processPropertyLevelAnnotations( mappingContext, typeModel, propertyModel ) ) {
				processedAtLeastOneAnnotation = true;
			}
		}

		return processedAtLeastOneAnnotation;
	}

	private boolean processConstructorLevelAnnotations(ConstructorMappingStep mappingContext,
			PojoRawTypeModel<?> typeModel, PojoConstructorModel<?> constructorModel) {
		boolean processedAtLeastOneAnnotation = false;
		List<Annotation> annotationList = constructorModel.annotations()
				.flatMap( annotationHelper::expandRepeatableContainingAnnotation )
				.collect( Collectors.toList() );
		for ( Annotation annotation : annotationList ) {
			if ( tryApplyProcessor( mappingContext, typeModel, constructorModel, annotation ) ) {
				processedAtLeastOneAnnotation = true;
			}
		}
		return processedAtLeastOneAnnotation;
	}

	private boolean processMethodParameterLevelAnnotations(MethodParameterMappingStep mappingContext,
			PojoConstructorModel<?> constructorModel, PojoMethodParameterModel<?> methodParameterModel) {
		boolean processedAtLeastOneAnnotation = false;
		List<Annotation> annotationList = methodParameterModel.annotations()
				.flatMap( annotationHelper::expandRepeatableContainingAnnotation )
				.collect( Collectors.toList() );
		for ( Annotation annotation : annotationList ) {
			if ( tryApplyProcessor( mappingContext, constructorModel, methodParameterModel, annotation ) ) {
				processedAtLeastOneAnnotation = true;
			}
		}
		return processedAtLeastOneAnnotation;
	}

	private boolean processPropertyLevelAnnotations(PropertyMappingStep mappingContext,
			PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel) {
		boolean processedAtLeastOneAnnotation = false;
		List<Annotation> annotationList = propertyModel.annotations()
				.flatMap( annotationHelper::expandRepeatableContainingAnnotation )
				.collect( Collectors.toList() );
		for ( Annotation annotation : annotationList ) {
			if ( tryApplyProcessor( mappingContext, typeModel, propertyModel, annotation ) ) {
				processedAtLeastOneAnnotation = true;
			}
		}
		return processedAtLeastOneAnnotation;
	}

	private <A extends Annotation> boolean tryApplyProcessor(TypeMappingStep mapping, PojoRawTypeModel<?> typeModel,
			A annotation) {
		Optional<BeanHolder<? extends TypeMappingAnnotationProcessor<? super A>>> processorOptional =
				annotationProcessorProvider.createTypeAnnotationProcessor( annotation );
		if ( !processorOptional.isPresent() ) {
			return false;
		}

		TypeMappingAnnotationProcessorContext context = new TypeMappingAnnotationProcessorContextImpl(
				typeModel, annotation, annotationHelper );

		try ( BeanHolder<? extends TypeMappingAnnotationProcessor<? super A>> processorHolder =
				processorOptional.get() ) {
			processorHolder.get().process( mapping, annotation, context );
		}
		catch (RuntimeException e) {
			rootFailureCollector.withContext( context.eventContext() )
					.add( e );
		}

		return true;
	}

	private <A extends Annotation> boolean tryApplyProcessor(ConstructorMappingStep mapping,
			PojoRawTypeModel<?> typeModel, PojoConstructorModel<?> constructorModel,
			A annotation) {
		Optional<BeanHolder<? extends ConstructorMappingAnnotationProcessor<? super A>>> processorOptional =
				annotationProcessorProvider.createConstructorAnnotationProcessor( annotation );
		if ( !processorOptional.isPresent() ) {
			return false;
		}

		ConstructorMappingAnnotationProcessorContext context =
				new ConstructorMappingAnnotationProcessorContextImpl( typeModel, constructorModel, annotation,
						annotationHelper );

		try ( BeanHolder<? extends ConstructorMappingAnnotationProcessor<? super A>> processorHolder =
				processorOptional.get() ) {
			processorHolder.get().process( mapping, annotation, context );
		}
		catch (RuntimeException e) {
			rootFailureCollector.withContext( context.eventContext() ).add( e );
		}

		return true;
	}

	private <A extends Annotation> boolean tryApplyProcessor(MethodParameterMappingStep mapping,
			PojoConstructorModel<?> constructorModel, PojoMethodParameterModel<?> methodParameterModel,
			A annotation) {
		Optional<BeanHolder<? extends MethodParameterMappingAnnotationProcessor<? super A>>> processorOptional =
				annotationProcessorProvider.createMethodParameterAnnotationProcessor( annotation );
		if ( !processorOptional.isPresent() ) {
			return false;
		}

		MethodParameterMappingAnnotationProcessorContext context =
				new MethodParameterMappingAnnotationProcessorContextImpl( constructorModel, methodParameterModel, annotation,
						annotationHelper );

		try ( BeanHolder<? extends MethodParameterMappingAnnotationProcessor<? super A>> processorHolder =
				processorOptional.get() ) {
			processorHolder.get().process( mapping, annotation, context );
		}
		catch (RuntimeException e) {
			rootFailureCollector.withContext( context.eventContext() ).add( e );
		}

		return true;
	}

	private <A extends Annotation> boolean tryApplyProcessor(PropertyMappingStep mapping,
			PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel,
			A annotation) {
		Optional<BeanHolder<? extends PropertyMappingAnnotationProcessor<? super A>>> processorOptional =
				annotationProcessorProvider.createPropertyAnnotationProcessor( annotation );
		if ( !processorOptional.isPresent() ) {
			return false;
		}

		PropertyMappingAnnotationProcessorContext context =
				new PropertyMappingAnnotationProcessorContextImpl( typeModel, propertyModel, annotation,
						annotationHelper, configurationContext );

		try ( BeanHolder<? extends PropertyMappingAnnotationProcessor<? super A>> processorHolder =
				processorOptional.get() ) {
			processorHolder.get().process( mapping, annotation, context );
		}
		catch (RuntimeException e) {
			rootFailureCollector.withContext( context.eventContext() ).add( e );
		}

		return true;
	}

}
