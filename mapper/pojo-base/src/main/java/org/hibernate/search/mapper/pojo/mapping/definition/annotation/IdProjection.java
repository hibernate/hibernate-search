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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.IdProjectionProcessor;

/**
 * Maps a constructor parameter to a projection to the identifier of the mapped entity,
 * i.e. the value of the property marked as {@code @DocumentId}.
 *
 * @see SearchProjectionFactory#id(Class)
 */
@Documented
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@MethodParameterMapping(processor = @MethodParameterMappingAnnotationProcessorRef(type = IdProjectionProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface IdProjection {
}
