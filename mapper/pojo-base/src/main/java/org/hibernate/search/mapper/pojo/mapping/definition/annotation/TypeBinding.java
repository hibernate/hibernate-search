/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanRetrieval;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.TypeBindingProcessor;

/**
 * Maps a type to index fields using a {@link TypeBinder},
 * which will define a {@link TypeBridge}.
 * <p>
 * This is a more complicated,
 * but more powerful alternative to mapping properties to field directly
 * using field annotations such as {@link GenericField}.
 * <p>
 * See the reference documentation for more information about bridges in general,
 * and type bridges in particular.
 */
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(TypeBinding.List.class)
@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = TypeBindingProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface TypeBinding {

	/**
	 * @return A reference to the binder to use.
	 * @see TypeBinderRef
	 */
	TypeBinderRef binder();

	@Documented
	@Target({ ElementType.TYPE })
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		TypeBinding[] value();
	}

}
