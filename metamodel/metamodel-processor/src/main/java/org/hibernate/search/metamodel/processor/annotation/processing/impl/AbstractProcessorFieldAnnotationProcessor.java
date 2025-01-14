/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.annotation.processing.impl;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

public abstract class AbstractProcessorFieldAnnotationProcessor extends AbstractProcessorAnnotationProcessor {

	@Override
	public final void process(PropertyMappingStep mapping, AnnotationMirror annotation,
			Element element, ProcessorAnnotationProcessorContext context) {
		String cleanedUpRelativeFieldName = getName( annotation );
		PropertyMappingFieldOptionsStep<?> fieldContext =
				initFieldMappingContext( mapping, annotation, cleanedUpRelativeFieldName );

		AnnotationMirror valueBinder = getValueBinder( annotation );
		if ( valueBinder != null ) {
			// TODO: do we also inject fields into a value binder ... ?
			context.messager().printMessage( Diagnostic.Kind.WARNING, "Defined value binder " + valueBinder + " is ignored " );
		}
		else if ( element.asType().getKind() == TypeKind.DECLARED
				&& context.types().asElement( element.asType() ).getKind() == ElementKind.ENUM ) {
			// if it's an enum, we won't get to the built-in bridge so we just use this stub one instead:
			fieldContext.valueBridge( new ProcessorEnumValueBridge( element.asType() ) );
		}

		ContainerExtractorPath extractorPath = toContainerExtractorPath( getExtraction( annotation ) );
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

	public record ProcessorEnumValueBridge(TypeMirror valueType) implements ValueBridge<Object, String> {

		@Override
			public String toIndexedValue(Object value, ValueBridgeToIndexedValueContext context) {
				return "";
			}
		}
}
