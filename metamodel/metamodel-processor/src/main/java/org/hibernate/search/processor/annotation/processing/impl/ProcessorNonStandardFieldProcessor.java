/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.annotation.processing.impl;

import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingNonFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.processor.model.impl.BuiltInBridgeResolverTypes;

class ProcessorNonStandardFieldProcessor extends AbstractProcessorNonFullTextFieldAnnotationProcessor {
	@Override
	PropertyMappingNonFullTextFieldOptionsStep<?> initSortableFieldMappingContext(PropertyMappingStep mappingContext,
			AnnotationMirror annotation, String fieldName) {
		return mappingContext.genericField( fieldName );
	}

	@Override
	protected Optional<IndexFieldTypeFinalStep<?>> configureField(PropertyBindingContext bindingContext,
			AnnotationMirror annotation, ProcessorAnnotationProcessorContext context, Element element, TypeMirror fieldType) {
		Optional<Class<?>> loadableType = BuiltInBridgeResolverTypes.loadableType( fieldType, context.types() );
		if ( loadableType.isPresent() ) {
			var step = bindingContext.typeFactory().as( loadableType.get() );
			return Optional.of( step );
		}
		else {
			context.messager().printMessage( Diagnostic.Kind.ERROR, "Unexpected property type.", element );
			return Optional.empty();
		}
	}
}
