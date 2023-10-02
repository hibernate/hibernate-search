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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.ConstructorMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.ConstructorMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.RootMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.ProjectionConstructorProcessor;

/**
 * Marks a constructor to use for projections from an index object (root or object field) to a Java object.
 * <p>
 * The constructor must accept at least one argument.
 * <p>
 * When this annotation is added on a type instead of a constructor,
 * it will apply to the only constructor of that type.
 * If multiple constructors exist, an exception will be thrown on startup.
 */
@Documented
@Target({ ElementType.CONSTRUCTOR, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@RootMapping
@ConstructorMapping(processor = @ConstructorMappingAnnotationProcessorRef(type = ProjectionConstructorProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = ProjectionConstructorProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface ProjectionConstructor {
}
