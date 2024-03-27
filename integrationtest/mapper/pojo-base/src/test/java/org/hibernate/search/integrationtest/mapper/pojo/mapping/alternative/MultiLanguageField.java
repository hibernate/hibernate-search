/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.mapping.alternative;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.AlternativeBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Documented
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = MultiLanguageField.Processor.class))
public @interface MultiLanguageField {

	/**
	 * @return The field name, if any.
	 *
	 * @see FullTextField#name()
	 */
	String name() default "";

	/**
	 * @return The field name, if any.
	 *
	 * @see FullTextField#projectable()
	 */
	Projectable projectable() default Projectable.DEFAULT;

	class Processor implements PropertyMappingAnnotationProcessor<MultiLanguageField> {
		@Override
		public void process(PropertyMappingStep mapping, MultiLanguageField annotation,
				PropertyMappingAnnotationProcessorContext context) {
			LanguageAlternativeBinderDelegate delegate = new LanguageAlternativeBinderDelegate(
					annotation.name().isEmpty() ? null : annotation.name(), annotation.projectable() );
			mapping.hostingType().binder( AlternativeBinder.create( Language.class,
					context.annotatedElement().name(), String.class, BeanReference.ofInstance( delegate ) ) );
		}
	}
}
