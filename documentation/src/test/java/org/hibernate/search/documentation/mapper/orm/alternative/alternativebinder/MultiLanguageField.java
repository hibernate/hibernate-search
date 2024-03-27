/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.alternative.alternativebinder;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.AlternativeBinder;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

//tag::include[]
@Retention(RetentionPolicy.RUNTIME) // <1>
@Target({ ElementType.METHOD, ElementType.FIELD }) // <2>
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef( // <3>
		type = MultiLanguageField.Processor.class
))
@Documented // <4>
public @interface MultiLanguageField {

	String name() default ""; // <5>

	class Processor // <6>
			implements PropertyMappingAnnotationProcessor<MultiLanguageField> { // <7>
		@Override
		public void process(PropertyMappingStep mapping, MultiLanguageField annotation,
				PropertyMappingAnnotationProcessorContext context) {
			LanguageAlternativeBinderDelegate delegate = new LanguageAlternativeBinderDelegate( // <8>
					annotation.name().isEmpty() ? null : annotation.name()
			);
			mapping.hostingType() // <9>
					.binder( AlternativeBinder.create( // <10>
							Language.class, // <11>
							context.annotatedElement().name(), // <12>
							String.class, // <13>
							BeanReference.ofInstance( delegate ) // <14>
					) );
		}
	}
}
//end::include[]
