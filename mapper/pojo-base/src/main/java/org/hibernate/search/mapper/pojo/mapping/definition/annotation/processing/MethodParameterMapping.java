/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep;

/**
 * Meta-annotation for annotations that apply mapping to a method parameter.
 * <p>
 * <strong>WARNING:</strong> at the moment, Hibernate Search only considers constructor parameters.
 * Parameters of any other kind of method are ignored.
 * <p>
 * Whenever an annotation meta-annotated with {@link MethodParameterMapping}
 * is found on a method parameter in the domain model,
 * the corresponding annotation processor will be retrieved and applied to that method parameter.
 * The processor will be passed the annotation instance through its
 * {@link MethodParameterMappingAnnotationProcessor#process(MethodParameterMappingStep, Annotation, MethodParameterMappingAnnotationProcessorContext)} method.
 */
@Documented
@Target(value = ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface MethodParameterMapping {

	/**
	 * @return A reference to the processor to use for the target annotation.
	 * @see MethodParameterMappingAnnotationProcessorRef
	 */
	MethodParameterMappingAnnotationProcessorRef processor();

}
