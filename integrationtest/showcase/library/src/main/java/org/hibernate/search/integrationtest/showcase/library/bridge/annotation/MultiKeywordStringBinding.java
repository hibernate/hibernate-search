/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.showcase.library.bridge.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.regex.Pattern;

import org.hibernate.search.integrationtest.showcase.library.bridge.MultiKeywordStringBridge;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.FIELD })
@Documented
@Repeatable(MultiKeywordStringBinding.List.class)
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = MultiKeywordStringBinding.Processor.class))
public @interface MultiKeywordStringBinding {

	String fieldName();

	String separatorPattern() default org.hibernate.search.integrationtest.showcase.library.bridge.MultiKeywordStringBridge.SEPARATOR_PATTERN_DEFAULT;

	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@Documented
	@interface List {
		MultiKeywordStringBinding[] value();
	}

	class Processor implements PropertyMappingAnnotationProcessor<MultiKeywordStringBinding> {
		@Override
		public void process(PropertyMappingStep mapping, MultiKeywordStringBinding annotation,
				PropertyMappingAnnotationProcessorContext context) {
			mapping.binder(
					new MultiKeywordStringBridge.Binder()
							.fieldName( annotation.fieldName() )
							.separatorPattern( Pattern.compile( annotation.separatorPattern() ) )
			);
		}
	}
}
