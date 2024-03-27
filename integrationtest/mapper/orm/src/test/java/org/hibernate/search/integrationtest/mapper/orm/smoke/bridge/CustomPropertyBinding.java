/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.smoke.bridge;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@PropertyMapping(processor = @PropertyMappingAnnotationProcessorRef(type = CustomPropertyBinding.Processor.class))
public @interface CustomPropertyBinding {

	String objectName();

	class Processor implements PropertyMappingAnnotationProcessor<CustomPropertyBinding> {
		@Override
		public void process(PropertyMappingStep mapping, CustomPropertyBinding annotation,
				PropertyMappingAnnotationProcessorContext context) {
			mapping.binder( new CustomPropertyBridge.Binder().objectName( annotation.objectName() ) );
		}
	}
}
