/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.typebridge.param.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

//tag::include[]
@Retention(RetentionPolicy.RUNTIME) // <1>
@Target({ ElementType.TYPE }) // <2>
@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = FullNameBinding.Processor.class)) // <3>
@Documented // <4>
public @interface FullNameBinding {

	boolean sortField() default false; // <5>

	class Processor // <6>
			implements TypeMappingAnnotationProcessor<FullNameBinding> { // <7>
		@Override
		public void process(TypeMappingStep mapping, FullNameBinding annotation,
				TypeMappingAnnotationProcessorContext context) {
			FullNameBinder binder = new FullNameBinder() // <8>
					.sortField( annotation.sortField() ); // <9>
			mapping.binder( binder ); // <10>
		}
	}
}
//end::include[]
