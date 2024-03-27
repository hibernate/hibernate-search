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

/**
 * Meta-annotation for annotations that apply mapping to a constructor.
 * <p>
 * Whenever an annotation meta-annotated with {@link ConstructorMapping}
 * is found on a constructor in the domain model,
 * the corresponding annotation processor will be retrieved and applied to that constructor.
 * The processor will be passed the annotation instance through its
 * {@link ConstructorMappingAnnotationProcessor#process(org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ConstructorMappingStep, Annotation, ConstructorMappingAnnotationProcessorContext)} method.
 */
@Documented
@Target(value = ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConstructorMapping {

	/**
	 * @return A reference to the processor to use for the target annotation.
	 * @see ConstructorMappingAnnotationProcessorRef
	 */
	ConstructorMappingAnnotationProcessorRef processor();

}
