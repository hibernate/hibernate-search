/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanRetrieval;

/**
 * A reference to a {@link ConstructorMappingAnnotationProcessor}.
 * <p>
 * References can use either a name, a type, or both.
 */
@Documented
@Target({ }) // Only used as a component in other annotations
@Retention(RetentionPolicy.RUNTIME)
public @interface ConstructorMappingAnnotationProcessorRef {

	/**
	 * Reference a {@link ConstructorMappingAnnotationProcessor} by its bean name.
	 * @return The bean name of the annotation processor.
	 */
	String name() default "";

	/**
	 * Reference a {@link ConstructorMappingAnnotationProcessor} by its bean type.
	 * @return The type of the annotation processor.
	 */
	Class<? extends ConstructorMappingAnnotationProcessor<?>> type() default UndefinedProcessorImplementationType.class;

	/**
	 * @return How to retrieve the processor. See {@link BeanRetrieval}.
	 */
	BeanRetrieval retrieval() default BeanRetrieval.ANY;

	/**
	 * Class used as a marker for the default value of the {@link #type()} attribute.
	 */
	abstract class UndefinedProcessorImplementationType implements ConstructorMappingAnnotationProcessor<Annotation> {
		private UndefinedProcessorImplementationType() {
		}
	}
}
