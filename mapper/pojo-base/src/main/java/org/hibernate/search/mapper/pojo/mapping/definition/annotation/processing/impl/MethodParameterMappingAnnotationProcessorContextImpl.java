/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotatedMethodParameter;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoConstructorModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoMethodParameterModel;
import org.hibernate.search.mapper.pojo.reporting.spi.PojoEventContexts;
import org.hibernate.search.util.common.reflect.spi.AnnotationHelper;
import org.hibernate.search.util.common.reporting.EventContext;

public class MethodParameterMappingAnnotationProcessorContextImpl
		extends AbstractMappingAnnotationProcessorContext
		implements MethodParameterMappingAnnotationProcessorContext, MappingAnnotatedMethodParameter {
	private final PojoConstructorModel<?> constructorModel;
	private final PojoMethodParameterModel<?> methodParameterModel;
	private final Annotation annotation;

	public MethodParameterMappingAnnotationProcessorContextImpl(PojoConstructorModel<?> constructorModel,
			PojoMethodParameterModel<?> methodParameterModel, Annotation annotation,
			AnnotationHelper annotationHelper) {
		super( annotationHelper );
		this.constructorModel = constructorModel;
		this.methodParameterModel = methodParameterModel;
		this.annotation = annotation;
	}

	@Override
	public MappingAnnotatedMethodParameter annotatedElement() {
		return this; // Not a lot to implement, so we just implement everything in the same class
	}

	@Override
	public EventContext eventContext() {
		return PojoEventContexts.fromType( constructorModel.typeModel() )
				.append( PojoEventContexts.fromConstructor( constructorModel ) )
				.append( PojoEventContexts.fromMethodParameter( methodParameterModel ) )
				.append( PojoEventContexts.fromAnnotation( annotation ) );
	}

	@Override
	public Optional<String> name() {
		return methodParameterModel.name();
	}

	@Override
	public Class<?> javaClass() {
		return methodParameterModel.typeModel().rawType().typeIdentifier().javaClass();
	}

	@Override
	public Stream<Annotation> allAnnotations() {
		return methodParameterModel.annotations().flatMap( annotationHelper::expandRepeatableContainingAnnotation );
	}

}
