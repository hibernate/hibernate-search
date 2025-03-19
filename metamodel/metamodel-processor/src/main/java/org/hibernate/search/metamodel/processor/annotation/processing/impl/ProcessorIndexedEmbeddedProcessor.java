/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.annotation.processing.impl;

import static org.hibernate.search.metamodel.processor.impl.IndexedEntityMetamodelAnnotationProcessor.processTypeAndProperties;

import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.spi.MappingAnnotationProcessorUtils;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

class ProcessorIndexedEmbeddedProcessor extends AbstractProcessorAnnotationProcessor {

	@SuppressWarnings("deprecation")
	@Override
	public void process(PropertyMappingStep mapping, AnnotationMirror annotation, Element element,
			ProcessorAnnotationProcessorContext context) {
		String cleanedUpPrefix = getAnnotationValueAsString( annotation, "prefix", null );

		String cleanedUpName = getAnnotationValueAsString( annotation, "name", null );

		String[] includePathsArray = toStringArray( getAnnotationValue( annotation, "includePaths" ) );
		String[] excludePathsArray = toStringArray( getAnnotationValue( annotation, "excludePaths" ) );

		ContainerExtractorPath extractorPath = toContainerExtractorPath( annotation, context );

		if ( getAnnotationValue( annotation, "targetType" ) != null ) {
			context.messager().printMessage( Diagnostic.Kind.WARNING,
					annotation + " defines a targetType, which cannot be processed and will be ignored" );
		}

		ObjectStructure structure = ObjectStructure.valueOf( getAnnotationValueAsString( annotation, "structure", "DEFAULT" ) );

		AnnotationValue value = getAnnotationValue( annotation, "includeDepth" );
		Integer includeDepth = value == null ? null : (int) value.getValue();

		value = getAnnotationValue( annotation, "includeEmbeddedObjectId" );
		boolean includeEmbeddedObjectId = value != null && (boolean) value.getValue();

		mapping.indexedEmbedded( cleanedUpName )
				.extractors( extractorPath )
				.prefix( cleanedUpPrefix )
				.structure( structure )
				.includeDepth( includeDepth )
				.includePaths( MappingAnnotationProcessorUtils.cleanUpPaths( includePathsArray ) )
				.excludePaths( MappingAnnotationProcessorUtils.cleanUpPaths( excludePathsArray ) )
				.includeEmbeddedObjectId( includeEmbeddedObjectId );

		// so we won't need to care about inverse side and other stuff:
		mapping.indexingDependency().reindexOnUpdate( ReindexOnUpdate.NO );

		collectExtraTypes( element.asType(), context );
	}

	private void collectExtraTypes(TypeMirror type, ProcessorAnnotationProcessorContext context) {
		if ( type == null || type.getKind() == TypeKind.NONE ) {
			return;
		}
		TypeElement element = (TypeElement) context.types().asElement( type );
		processTypeAndProperties(
				element,
				context.programmaticMapping().type( element.getQualifiedName().toString() ),
				context
		);
		collectExtraTypes( element.getSuperclass(), context );
		if ( type instanceof DeclaredType declaredType ) {
			for ( TypeMirror typeArgument : declaredType.getTypeArguments() ) {
				collectExtraTypes( typeArgument, context );
			}
		}
	}

	@Override
	protected Optional<IndexFieldTypeFinalStep<?>> configureField(PropertyBindingContext bindingContext,
			AnnotationMirror annotation, ProcessorAnnotationProcessorContext context, Element element, TypeMirror fieldType) {
		context.messager().printMessage( Diagnostic.Kind.ERROR, "IndexedEmbedded are not allowed within binders.", element );
		return Optional.empty();
	}
}
