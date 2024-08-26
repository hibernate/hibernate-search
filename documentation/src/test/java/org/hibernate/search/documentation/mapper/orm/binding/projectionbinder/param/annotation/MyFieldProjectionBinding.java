/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.projectionbinder.param.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MethodParameterMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.MethodParameterMappingStep;

//tag::include[]
@Retention(RetentionPolicy.RUNTIME) // <1>
@Target({ ElementType.PARAMETER }) // <2>
@MethodParameterMapping(processor = @MethodParameterMappingAnnotationProcessorRef( // <3>
		type = MyFieldProjectionBinding.Processor.class
))
@Documented // <4>
public @interface MyFieldProjectionBinding {

	String fieldName() default ""; // <5>

	class Processor // <6>
			implements MethodParameterMappingAnnotationProcessor<MyFieldProjectionBinding> { // <7>
		@Override
		public void process(MethodParameterMappingStep mapping, MyFieldProjectionBinding annotation,
				MethodParameterMappingAnnotationProcessorContext context) {
			MyFieldProjectionBinder binder = new MyFieldProjectionBinder(); // <8>
			if ( !annotation.fieldName().isEmpty() ) { // <9>
				binder.fieldName( annotation.fieldName() );
			}
			mapping.projection( binder ); // <10>
		}
	}
}
//end::include[]
