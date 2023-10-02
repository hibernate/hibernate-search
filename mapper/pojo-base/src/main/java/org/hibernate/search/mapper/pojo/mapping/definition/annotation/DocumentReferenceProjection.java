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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.DocumentReferenceProjectionProcessor;

/**
 * Maps a constructor parameter to a projection to a {@link org.hibernate.search.engine.backend.common.DocumentReference} representing the hit.
 *
 * @see SearchProjectionFactory#documentReference()
 */
@Documented
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@MethodParameterMapping(processor = @MethodParameterMappingAnnotationProcessorRef(
		type = DocumentReferenceProjectionProcessor.class, retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface DocumentReferenceProjection {

}
