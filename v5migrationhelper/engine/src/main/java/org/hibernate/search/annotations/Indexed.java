/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.annotations.impl.IndexedAnnotationProcessor;
import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;

/**
 * @deprecated Use Hibernate Search 6's {@link org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed}
 * instead.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Deprecated
@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = IndexedAnnotationProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface Indexed {
	/**
	 * @return The filename of the index. Default to empty string
	 */
	String index() default "";
}
