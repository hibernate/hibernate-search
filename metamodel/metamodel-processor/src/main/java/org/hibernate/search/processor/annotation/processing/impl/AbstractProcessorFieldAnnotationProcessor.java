/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.processor.annotation.processing.impl;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

public abstract class AbstractProcessorFieldAnnotationProcessor extends AbstractProcessorAnnotationProcessor {

	@Override
	public final void process(PropertyMappingStep mapping, AnnotationMirror annotation,
			Element element, ProcessorAnnotationProcessorContext context) {
		// we check for bridges and binders first, to make sure none are defined,
		// if any -- we want to stop processing as soon as possible so that field is not added to the index!
		AnnotationMirror valueBinder = getValueBinder( annotation );
		if ( valueBinder != null ) {
			context.messager().printMessage( Diagnostic.Kind.WARNING, "Defined value binder " + valueBinder + " is ignored ",
					element );
			return;
		}
		AnnotationMirror valueBridge = getValueBridge( annotation );
		if ( valueBridge != null ) {
			context.messager().printMessage( Diagnostic.Kind.WARNING, "Defined value bridge " + valueBridge + " is ignored ",
					element );
			return;
		}

		String cleanedUpRelativeFieldName = getName( annotation );
		PropertyMappingFieldOptionsStep<?> fieldContext =
				initFieldMappingContext( mapping, annotation, cleanedUpRelativeFieldName );

		ContainerExtractorPath extractorPath = toContainerExtractorPath( getExtraction( annotation ), context );
		fieldContext.extractors( extractorPath );
	}

	protected String getName(AnnotationMirror annotation) {
		return getAnnotationValueAsString( annotation, "name" );
	}

	abstract PropertyMappingFieldOptionsStep<?> initFieldMappingContext(PropertyMappingStep mappingContext,
			AnnotationMirror annotation, String fieldName);

	private AnnotationMirror getExtraction(AnnotationMirror annotation) {
		return getAnnotationProperty( annotation, "extraction" );
	}

	private AnnotationMirror getValueBinder(AnnotationMirror annotation) {
		return getAnnotationProperty( annotation, "valueBinder" );
	}

	private AnnotationMirror getValueBridge(AnnotationMirror annotation) {
		return getAnnotationProperty( annotation, "valueBridge" );
	}

}
