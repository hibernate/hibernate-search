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
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.FieldProjectionProcessor;

/**
 * Maps a constructor parameter to a projection to the value of a field in the indexed document.
 *
 * @see SearchProjectionFactory#field(String, Class, ValueModel)
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
	 * @see org.hibernate.search.engine.search.common.ValueConvert
	 * @see SearchProjectionFactory#field(String, Class, org.hibernate.search.engine.search.common.ValueConvert)
	 * @deprecated Use {@link #valueModel()} instead.
	 * Note, setting {@link #convert()} to non-default {@link org.hibernate.search.engine.search.common.ValueConvert#NO}
	 * will result in an exception at runtime, use {@link #valueModel()} with {@link ValueModel#INDEX} instead.
	 * <p>
	 * Setting {@link #valueModel()} to any non-default value will take precedence over {@link #convert()} default {@link org.hibernate.search.engine.search.common.ValueConvert#YES} value.
	 */
	@Deprecated
	org.hibernate.search.engine.search.common.ValueConvert convert() default org.hibernate.search.engine.search.common.ValueConvert.YES;

	/**
	 * @return The model value, determines how the data fetched from the backend should be converted.
	 * @see ValueModel
	 * @see SearchProjectionFactory#field(String, Class, ValueModel)
	 */
	ValueModel valueModel() default ValueModel.MAPPING;

}
