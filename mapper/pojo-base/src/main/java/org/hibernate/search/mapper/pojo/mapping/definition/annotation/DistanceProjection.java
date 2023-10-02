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
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.DistanceProjectionProcessor;

/**
 * Maps a constructor parameter to a distance projection,
 * i.e. sequences of text that matched the query, extracted from the given field's value.
 *
 * @see SearchProjectionFactory#highlight(String)
 */
@Documented
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@MethodParameterMapping(processor = @MethodParameterMappingAnnotationProcessorRef(type = DistanceProjectionProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface DistanceProjection {

	/**
	 * @return The <a href="../../../../../engine/search/projection/dsl/SearchProjectionFactory.html#field-paths">path</a>
	 * to the index field whose value will be extracted.
	 * Defaults to the name of the annotated constructor parameter,
	 * if it can be retrieved (requires the class to be compiled with the {@code -parameters} flag;
	 * otherwise an empty {@code path} will lead to a failure).
	 * @see SearchProjectionFactory#distance(String, GeoPoint)
	 */
	String path() default "";

	/**
	 * @return The name of a {@link org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep#param(String, Object) query parameter}
	 * that will represent a {@link GeoPoint} point, from which the distance to the field value will be calculated.
	 * @see SearchProjectionFactory#distance(String, GeoPoint)
	 */
	String fromParam();

	/**
	 * @return The unit of the computed distance (default is meters).
	 */
	DistanceUnit unit() default DistanceUnit.METERS;

}
