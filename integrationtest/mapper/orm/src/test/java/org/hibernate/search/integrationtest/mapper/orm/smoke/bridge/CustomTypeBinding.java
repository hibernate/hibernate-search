/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.orm.smoke.bridge;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMapping;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@TypeMapping(processor = @TypeMappingAnnotationProcessorRef(type = CustomTypeBinding.Processor.class))
public @interface CustomTypeBinding {

	String objectName();

	class Processor implements TypeMappingAnnotationProcessor<CustomTypeBinding> {
		@Override
		public void process(TypeMappingStep mapping, CustomTypeBinding annotation,
				TypeMappingAnnotationProcessorContext context) {
			mapping.binder( new CustomTypeBridge.Binder().objectName( annotation.objectName() ) );
		}
	}
}
