/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.annotation.processing.impl;

import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.runtime.PropertyBridgeWriteContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

class ProcessorTypePropertyBindingProcessor extends AbstractProcessorAnnotationProcessor {
	@Override
	public void process(PropertyMappingStep mapping, AnnotationMirror annotation, Element element,
			ProcessorAnnotationProcessorContext context) {
		AnnotationMirror propertyBinder = getAnnotationProperty( annotation, "binder" );
		if ( propertyBinder != null ) {
			AnnotationValue type = getAnnotationValue( propertyBinder, "type" );
			if ( type != null ) {
				TypeMirror binder = (TypeMirror) type.getValue();

				mapping.binder( new ProcessorPropertyBinder( context, binder ) );
			}
			context.messager().printMessage( Diagnostic.Kind.WARNING,
					"Custom binders are not yet supported by the static metamodel processor and will be ignored.", element );
		}
	}

	@Override
	protected Optional<IndexFieldTypeFinalStep<?>> configureField(PropertyBindingContext bindingContext,
			AnnotationMirror annotation, ProcessorAnnotationProcessorContext context, Element element, TypeMirror fieldType) {
		context.messager().printMessage( Diagnostic.Kind.WARNING, "Property binding within the binder is not supported",
				element );
		return Optional.empty();
	}

	private static class ProcessorPropertyBinder implements PropertyBinder {
		private final ProcessorAnnotationProcessorContext context;
		private final TypeMirror binder;

		public ProcessorPropertyBinder(ProcessorAnnotationProcessorContext context, TypeMirror binder) {
			this.context = context;
			this.binder = binder;
		}

		@Override
		public void bind(PropertyBindingContext bindingContext) {
			bindingContext.dependencies().useRootOnly();

			for ( Element member : context.elements()
					.getAllMembers( (TypeElement) context.types().asElement( binder ) ) ) {
				if ( member.getKind() == ElementKind.FIELD ) {
					// we only care about fields in the "injectable binders" (at least for now):
					for ( AnnotationMirror annotationMirror : member.getAnnotationMirrors() ) {
						ProcessorPropertyMappingAnnotationProcessor.processor( annotationMirror )
								.ifPresent( p -> p.process(
										bindingContext,
										annotationMirror,
										context,
										member
								) );
					}
				}
			}

			bindingContext.bridge( DoNothingPropertyBridge.INSTANCE );
		}

		private static class DoNothingPropertyBridge implements PropertyBridge<Object> {
			private static final DoNothingPropertyBridge INSTANCE = new DoNothingPropertyBridge();

			@Override
			public void write(DocumentElement target, Object bridgedElement,
					PropertyBridgeWriteContext context) {
				// do nothing
			}
		}
	}
}
