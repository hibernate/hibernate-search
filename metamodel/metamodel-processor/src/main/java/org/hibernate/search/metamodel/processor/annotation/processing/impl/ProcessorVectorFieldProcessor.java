/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.annotation.processing.impl;

import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.engine.backend.types.dsl.VectorFieldTypeOptionsStep;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtract;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationDefaultValues;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingVectorFieldOptionsStep;
import org.hibernate.search.metamodel.processor.model.impl.BuiltInBridgeResolverTypes;

class ProcessorVectorFieldProcessor extends AbstractProcessorFieldAnnotationProcessor {
	@Override
	PropertyMappingFieldOptionsStep<?> initFieldMappingContext(PropertyMappingStep mappingContext, AnnotationMirror annotation,
			String fieldName) {
		int dimension = getAnnotationValueAsInt( annotation, "dimension", AnnotationDefaultValues.DEFAULT_DIMENSION );
		PropertyMappingVectorFieldOptionsStep fieldContext = dimension == AnnotationDefaultValues.DEFAULT_DIMENSION
				? mappingContext.vectorField( fieldName )
				: mappingContext.vectorField( dimension, fieldName );

		int m = getAnnotationValueAsInt( annotation, "m", AnnotationDefaultValues.DEFAULT_M );
		if ( m != AnnotationDefaultValues.DEFAULT_M ) {
			fieldContext.m( m );
		}

		int efConstruction =
				getAnnotationValueAsInt( annotation, "efConstruction", AnnotationDefaultValues.DEFAULT_EF_CONSTRUCTION );
		if ( efConstruction != AnnotationDefaultValues.DEFAULT_EF_CONSTRUCTION ) {
			fieldContext.efConstruction( efConstruction );
		}

		VectorSimilarity vectorSimilarity = getVectorSimilarity( annotation );
		if ( !VectorSimilarity.DEFAULT.equals( vectorSimilarity ) ) {
			fieldContext.vectorSimilarity( vectorSimilarity );
		}
		Projectable projectable = getProjectable( annotation );
		if ( !Projectable.DEFAULT.equals( projectable ) ) {
			fieldContext.projectable( projectable );
		}

		Searchable searchable = getSearchable( annotation );
		if ( !Searchable.DEFAULT.equals( searchable ) ) {
			fieldContext.searchable( searchable );
		}

		String indexNullAs = getIndexNullAs( annotation );
		if ( indexNullAs != null && !AnnotationDefaultValues.DO_NOT_INDEX_NULL.equals( indexNullAs ) ) {
			fieldContext.indexNullAs( indexNullAs );
		}

		return fieldContext;
	}

	@Override
	protected Optional<IndexFieldTypeFinalStep<?>> configureField(PropertyBindingContext bindingContext,
			AnnotationMirror annotation,
			ProcessorAnnotationProcessorContext context, Element element, TypeMirror fieldType) {
		Class<?> vectorType;
		Optional<Class<?>> loadableType = BuiltInBridgeResolverTypes.loadableType( fieldType, context.types() );
		if ( loadableType.isPresent() ) {
			vectorType = loadableType.get();
		}
		else {
			context.messager().printMessage( Diagnostic.Kind.ERROR, "Only float[]/byte[] fields are allowed.", element );
			return Optional.empty();
		}

		VectorFieldTypeOptionsStep<?, ?> optionsStep = bindingContext.typeFactory().asVector( vectorType )
				// it doesn't really matter since we won't be actually using this value anywhere,
				// but since it's "optional" let's just keep it simple for now here:
				.dimension( 10 );

		int m = getAnnotationValueAsInt( annotation, "m", AnnotationDefaultValues.DEFAULT_M );
		if ( m != AnnotationDefaultValues.DEFAULT_M ) {
			optionsStep.m( m );
		}

		int efConstruction =
				getAnnotationValueAsInt( annotation, "efConstruction", AnnotationDefaultValues.DEFAULT_EF_CONSTRUCTION );
		if ( efConstruction != AnnotationDefaultValues.DEFAULT_EF_CONSTRUCTION ) {
			optionsStep.efConstruction( efConstruction );
		}

		VectorSimilarity vectorSimilarity = getVectorSimilarity( annotation );
		if ( !VectorSimilarity.DEFAULT.equals( vectorSimilarity ) ) {
			optionsStep.vectorSimilarity( vectorSimilarity );
		}
		Projectable projectable = getProjectable( annotation );
		if ( !Projectable.DEFAULT.equals( projectable ) ) {
			optionsStep.projectable( projectable );
		}

		Searchable searchable = getSearchable( annotation );
		if ( !Searchable.DEFAULT.equals( searchable ) ) {
			optionsStep.searchable( searchable );
		}

		//indexNullAs doesn't really influence the model, let's ignore it:
		//		String indexNullAs = getIndexNullAs( annotation );
		//		if ( indexNullAs != null && !AnnotationDefaultValues.DO_NOT_INDEX_NULL.equals( indexNullAs ) ) {
		//			optionsStep.indexNullAs( indexNullAs );
		//		}
		return Optional.of( optionsStep );
	}

	@Override
	protected ContainerExtractorPath toContainerExtractorPath(AnnotationMirror extraction,
			ProcessorAnnotationProcessorContext context) {
		if ( extraction == null ) {
			return ContainerExtractorPath.noExtractors();
		}
		return toContainerExtractorPath( extraction, ContainerExtract.NO.name(), context );
	}

	protected VectorSimilarity getVectorSimilarity(AnnotationMirror annotation) {
		return VectorSimilarity
				.valueOf( getAnnotationValueAsString( annotation, "vectorSimilarity", VectorSimilarity.DEFAULT.name() ) );
	}

	protected Searchable getSearchable(AnnotationMirror annotation) {
		return Searchable.valueOf( getAnnotationValueAsString( annotation, "searchable", Searchable.DEFAULT.name() ) );
	}

	protected Projectable getProjectable(AnnotationMirror annotation) {
		return Projectable.valueOf( getAnnotationValueAsString( annotation, "projectable", Projectable.DEFAULT.name() ) );
	}

	protected String getIndexNullAs(AnnotationMirror annotation) {
		return getAnnotationValueAsString( annotation, "indexNullAs", AnnotationDefaultValues.DO_NOT_INDEX_NULL );
	}
}
