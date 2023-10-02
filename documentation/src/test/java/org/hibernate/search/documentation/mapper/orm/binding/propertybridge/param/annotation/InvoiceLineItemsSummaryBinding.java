/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.propertybridge.param.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

//tag::include[]
@Retention(RetentionPolicy.RUNTIME) // <1>
@Target({ ElementType.METHOD, ElementType.FIELD }) // <2>
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef( // <3>
		type = InvoiceLineItemsSummaryBinding.Processor.class
))
@Documented // <4>
public @interface InvoiceLineItemsSummaryBinding {

	String fieldName() default ""; // <5>

	class Processor // <6>
			implements PropertyMappingAnnotationProcessor<InvoiceLineItemsSummaryBinding> { // <7>
		@Override
		public void process(PropertyMappingStep mapping,
				InvoiceLineItemsSummaryBinding annotation,
				PropertyMappingAnnotationProcessorContext context) {
			InvoiceLineItemsSummaryBinder binder = new InvoiceLineItemsSummaryBinder(); // <8>
			if ( !annotation.fieldName().isEmpty() ) { // <9>
				binder.fieldName( annotation.fieldName() );
			}
			mapping.binder( binder ); // <10>
		}
	}
}
//end::include[]
