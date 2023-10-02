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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.CompositeProjectionProcessor;

/**
 * Maps a constructor parameter to a composite projection,
 * which will combine multiple inner projections.
 * <p>
 * The content of the composite projection is defined in the constructor parameter type
 * by another {@link ProjectionConstructor}.
 * <p>
 * On contrary to the {@link ObjectProjection object projection},
 * a composite projection is not bound to a specific object field,
 * and thus it will always yield one and only one value.
 *
 * @see SearchProjectionFactory#composite()
 */
@Documented
@Target({ ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@MethodParameterMapping(processor = @MethodParameterMappingAnnotationProcessorRef(type = CompositeProjectionProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface CompositeProjection {

}
