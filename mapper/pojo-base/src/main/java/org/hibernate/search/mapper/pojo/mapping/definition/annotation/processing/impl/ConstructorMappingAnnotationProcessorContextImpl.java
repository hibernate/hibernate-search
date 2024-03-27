/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.ConstructorMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotatedElement;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.util.common.reflect.spi.AnnotationHelper;
import org.hibernate.search.util.common.reporting.EventContext;

public class ConstructorMappingAnnotationProcessorContextImpl
		extends AbstractMappingAnnotationProcessorContext
		implements ConstructorMappingAnnotationProcessorContext, MappingAnnotatedElement {
	private final PojoRawTypeModel<?> typeModel;
	private final PojoConstructorModel<?> constructorModel;
	private final Annotation annotation;

	public ConstructorMappingAnnotationProcessorContextImpl(PojoRawTypeModel<?> typeModel,
			PojoConstructorModel<?> constructorModel, Annotation annotation,
			AnnotationHelper annotationHelper) {
		super( annotationHelper );
		this.typeModel = typeModel;
		this.constructorModel = constructorModel;
		this.annotation = annotation;
	}

	@Override
	public MappingAnnotatedElement annotatedElement() {
		return this; // Not a lot to implement, so we just implement everything in the same class
	}

	@Override
	public EventContext eventContext() {
		return PojoEventContexts.fromType( typeModel )
				.append( PojoEventContexts.fromConstructor( constructorModel ) )
				.append( PojoEventContexts.fromAnnotation( annotation ) );
	}

	@Override
	public Class<?> javaClass() {
		return constructorModel.typeModel().rawType().typeIdentifier().javaClass();
	}

	@Override
	public Stream<Annotation> allAnnotations() {
		return constructorModel.annotations().flatMap( annotationHelper::expandRepeatableContainingAnnotation );
	}

}
