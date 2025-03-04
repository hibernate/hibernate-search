/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.metamodel.processor.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

public final class ProcessorElementUtils {

	private ProcessorElementUtils() {
	}

	public static Stream<? extends Element> propertyElements(Elements elementUtils, TypeElement typeElement) {
		return elementUtils.getAllMembers( typeElement )
				.stream()
				.filter( ProcessorElementUtils::isProperty );
	}

	public static Stream<? extends AnnotationMirror> flattenedAnnotations(Types types, Element element) {
		return element.getAnnotationMirrors().stream()
				.flatMap( a -> ProcessorElementUtils.flattened( types, a ) );
	}

	@SuppressWarnings("unchecked")
	private static Stream<? extends AnnotationMirror> flattened(Types types, AnnotationMirror annotationMirror) {
		for ( Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotationMirror.getElementValues()
				.entrySet() ) {
			if ( entry.getKey().getSimpleName().contentEquals( "value" ) ) {
				if ( entry.getKey().getReturnType() instanceof ArrayType arrayType ) {
					Optional<? extends AnnotationMirror> repeatable = types.asElement( arrayType.getComponentType() )
							.getAnnotationMirrors()
							.stream()
							.filter( a -> ( (TypeElement) a.getAnnotationType().asElement() ).getQualifiedName()
									.contentEquals( "java.lang.annotation.Repeatable" ) )
							.findAny();
					if ( repeatable.isPresent() ) {
						TypeMirror returnType = (TypeMirror) repeatable.get().getElementValues().entrySet().iterator().next()
								.getValue().getValue();
						TypeMirror annotationType = annotationMirror.getAnnotationType();
						if ( types.isSameType( returnType, annotationType ) ) {
							return ( (List<? extends AnnotationMirror>) entry.getValue().getValue() ).stream();
						}
					}
				}

			}
		}
		return Stream.of( annotationMirror );
	}

	private static boolean isProperty(Element element) {
		if ( element.getKind() == ElementKind.METHOD ) {
			ExecutableElement executable = (ExecutableElement) element;
			return isGetter( executable );
		}
		else {
			return element.getKind() == ElementKind.FIELD;
		}
	}

	private static boolean isGetter(ExecutableElement executable) {
		if ( !executable.getParameters().isEmpty() ) {
			return false;
		}
		TypeMirror returnType = executable.getReturnType();
		if ( returnType.getKind() == TypeKind.VOID ) {
			return false;
		}
		String name = executable.getSimpleName().toString();
		if ( returnType.getKind() == TypeKind.BOOLEAN
				|| !returnType.getKind().isPrimitive()
						&& returnType.toString().equals( "java.lang.Boolean" ) ) {
			return name.startsWith( "is" ) || name.startsWith( "has" );
		}

		return name.startsWith( "get" );
	}
}
