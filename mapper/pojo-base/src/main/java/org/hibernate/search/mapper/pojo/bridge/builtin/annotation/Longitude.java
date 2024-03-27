/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.bridge.builtin.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.processor.impl.LongitudeProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;

/**
 * Mark the property hosting the longitude of a specific spatial coordinate.
 * The property must be of type {@code Double} or {@code double}.
 *
 * @author Nicolas Helleringer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Documented
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = LongitudeProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface Longitude {

	/**
	 * @return The name of the marker set this marker belongs to.
	 * Set it to the value of {@link GeoPointBinding#markerSet()}
	 * so that the bridge detects this marker.
	 */
	String markerSet() default "";

}
