/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.FieldProjectionProcessor;

/**
 * Maps a constructor parameter to a projection to the value of a field in the indexed document.
 *
 * @see SearchProjectionFactory#field(String, Class, ValueConvert)
 */
@Documented
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@MethodParameterMapping(processor = @MethodParameterMappingAnnotationProcessorRef(type = FieldProjectionProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface FieldProjection {

	/**
	 * @return The <a href="../../../../../engine/search/projection/dsl/SearchProjectionFactory.html#field-paths">path</a>
	 * to the index field whose value will be extracted.
	 * Defaults to the name of the annotated constructor parameter,
	 * if it can be retrieved (requires the class to be compiled with the {@code -parameters} flag;
	 * otherwise an empty {@code path} will lead to a failure).
	 * @see SearchProjectionFactory#field(String, Class)
	 */
	String path() default "";

	/**
	 * @return A value controlling how the data fetched from the backend should be converted.
	 * @see ValueConvert
	 * @see SearchProjectionFactory#field(String, Class, ValueConvert)
	 */
	ValueConvert convert() default ValueConvert.YES;

}
