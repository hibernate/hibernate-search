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
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl.PropertyBindingProcessor;

/**
 * Maps a property to index fields using a {@link PropertyBinder},
 * which will define a {@link PropertyBridge}.
 * <p>
 * This is a more complicated,
 * but more powerful alternative to mapping properties to field directly
 * using field annotations such as {@link GenericField}.
 * <p>
 * See the reference documentation for more information about bridges in general,
 * and property bridges in particular.
 */
@Documented
@Target({ ElementType.METHOD, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(PropertyBinding.List.class)
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = PropertyBindingProcessor.class,
		retrieval = BeanRetrieval.CONSTRUCTOR))
public @interface PropertyBinding {

	/**
	 * @return A reference to the binder to use.
	 * @see PropertyBinderRef
	 */
	PropertyBinderRef binder();

	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Retention(RetentionPolicy.RUNTIME)
	@interface List {
		PropertyBinding[] value();
	}

}
