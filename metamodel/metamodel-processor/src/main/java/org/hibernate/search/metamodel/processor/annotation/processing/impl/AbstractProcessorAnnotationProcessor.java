/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.annotation.processing.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import org.hibernate.search.engine.backend.types.IndexFieldType;
import org.hibernate.search.engine.backend.types.dsl.IndexFieldTypeFinalStep;
import org.hibernate.search.mapper.pojo.bridge.binding.PropertyBindingContext;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtract;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.metamodel.processor.logging.impl.MappingLog;
import org.hibernate.search.util.common.AssertionFailure;

public abstract class AbstractProcessorAnnotationProcessor implements ProcessorPropertyMappingAnnotationProcessor {

	protected static final String[] EMPTY = new String[0];

	@Override
	public final void process(PropertyBindingContext bindingContext, AnnotationMirror annotation,
			ProcessorAnnotationProcessorContext context, Element element) {
		String annotationName = getAnnotationValueAsString( annotation, "name", "" );
		String resolvedName = annotationName.isEmpty() ? element.getSimpleName().toString() : annotationName;

		// in binders, we only allow IndexFieldReference fields.
		if ( element.asType() instanceof DeclaredType dt
				&& ( (TypeElement) dt.asElement() ).getQualifiedName()
						.contentEquals( "org.hibernate.search.engine.backend.document.IndexFieldReference" ) ) {
			TypeMirror fieldType = dt.getTypeArguments().get( 0 );

			configureField(
					bindingContext, annotation,
					context, element, fieldType
			).ifPresent( step -> {
				IndexFieldType<?> configuredField = step.toIndexFieldType();
				bindingContext.indexSchemaElement().field( resolvedName, configuredField ).toReference();
			} );
		}
		else {
			context.messager().printMessage( Diagnostic.Kind.ERROR,
					"Only fields of org.hibernate.search.engine.backend.document.IndexFieldReference type are allowed to be annotated with Hibernate Search annotations in the binder.",
					element );
		}
	}

	protected abstract Optional<IndexFieldTypeFinalStep<?>> configureField(PropertyBindingContext bindingContext,
			AnnotationMirror annotation, ProcessorAnnotationProcessorContext context, Element element, TypeMirror fieldType);

	protected ContainerExtractorPath toContainerExtractorPath(AnnotationMirror extraction) {
		return toContainerExtractorPath( extraction, "DEFAULT" );
	}

	protected ContainerExtractorPath toContainerExtractorPath(AnnotationMirror extraction, String defaultValue) {
		if ( extraction == null ) {
			return ContainerExtractorPath.defaultExtractors();
		}
		else {
			ContainerExtract extract =
					ContainerExtract.valueOf( getAnnotationValueAsString( extraction, "extraction", defaultValue ) );
			String[] extractors = toStringArray( getAnnotationValue( extraction, "value" ) );
			switch ( extract ) {
				case NO:
					if ( extractors.length != 0 ) {
						throw MappingLog.INSTANCE.cannotReferenceExtractorsWhenExtractionDisabled();
					}
					return ContainerExtractorPath.noExtractors();
				case DEFAULT:
					if ( extractors.length == 0 ) {
						return ContainerExtractorPath.defaultExtractors();
					}
					else {
						return ContainerExtractorPath.explicitExtractors( Arrays.asList( extractors ) );
					}
				default:
					throw new AssertionFailure(
							"Unexpected " + ContainerExtract.class.getSimpleName() + " value: " + extract
					);
			}
		}
	}

	protected AnnotationMirror getAnnotationProperty(AnnotationMirror annotation, String annotationName) {
		AnnotationValue value = getAnnotationValue( annotation, annotationName );
		return (AnnotationMirror) ( value == null ? null : value.getValue() );
	}

	protected String getAnnotationValueAsString(AnnotationMirror annotation, String name, String defaultValue) {
		AnnotationValue annotationValue = getAnnotationValue( annotation, name );
		if ( annotationValue == null ) {
			return defaultValue;
		}
		return annotationValue.getValue().toString();
	}

	protected String getAnnotationValueAsString(AnnotationMirror annotation, String name) {
		return getAnnotationValueAsString( annotation, name, null );
	}

	protected int getAnnotationValueAsInt(AnnotationMirror annotation, String name, int defaultValue) {
		AnnotationValue annotationValue = getAnnotationValue( annotation, name );
		if ( annotationValue == null ) {
			return defaultValue;
		}
		return (int) annotationValue.getValue();
	}

	protected AnnotationValue getAnnotationValue(AnnotationMirror annotation, String name) {
		if ( annotation == null ) {
			return null;
		}
		var elementValues = annotation.getElementValues();
		for ( var entry : elementValues.entrySet() ) {
			if ( entry.getKey().getSimpleName().contentEquals( name ) ) {
				return entry.getValue();
			}
		}
		return null;
	}

	protected String[] toStringArray(AnnotationValue value) {
		if ( value == null ) {
			return EMPTY;
		}
		if ( value.getValue() instanceof List<?> list ) {
			return list.stream().map( v -> Objects.toString( ( (AnnotationValue) v ).getValue(), null ) )
					.toArray( String[]::new );
		}
		return new String[] { Objects.toString( value.getValue(), null ) };
	}
}
