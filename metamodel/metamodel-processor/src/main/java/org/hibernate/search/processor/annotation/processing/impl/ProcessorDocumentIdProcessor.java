/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.annotation.processing.impl;

import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

class ProcessorDocumentIdProcessor implements ProcessorPropertyMappingAnnotationProcessor {
	@Override
	public void process(PropertyMappingStep mapping, AnnotationMirror annotation, Element element,
			ProcessorAnnotationProcessorContext context) {
		IdentifierBinder binder = createIdentifierBinder( annotation, context );

		mapping.documentId().identifierBinder( binder, Map.of() );
	}

	@Override
	public void process(PropertyBindingContext bindingContext, AnnotationMirror annotation,
			ProcessorAnnotationProcessorContext context, Element element) {
		context.messager().printMessage( Diagnostic.Kind.ERROR, "ID cannot be defined in the binder.", element );
	}

	private IdentifierBinder createIdentifierBinder(AnnotationMirror annotation, ProcessorAnnotationProcessorContext context) {
		// The bridge will be auto-detected from the property type
		return null;
	}
}
