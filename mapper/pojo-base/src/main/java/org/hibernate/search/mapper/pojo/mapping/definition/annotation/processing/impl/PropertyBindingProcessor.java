/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanDelegatingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.logging.impl.MappingLog;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

public final class PropertyBindingProcessor implements PropertyMappingAnnotationProcessor<PropertyBinding> {

	@Override
	public void process(PropertyMappingStep mapping, PropertyBinding annotation,
			PropertyMappingAnnotationProcessorContext context) {
		PropertyBinderRef propertyBinder = annotation.binder();
		PropertyBinder binder = createBinder( propertyBinder, context );

		Map<String, Object> params = context.toMap( propertyBinder.params() );
		mapping.binder( binder, params );
	}

	private PropertyBinder createBinder(PropertyBinderRef binderReferenceAnnotation,
			MappingAnnotationProcessorContext context) {
		Optional<BeanReference<? extends PropertyBinder>> binderReference = context.toBeanReference(
				PropertyBinder.class,
				PropertyBinderRef.UndefinedBinderImplementationType.class,
				binderReferenceAnnotation.type(), binderReferenceAnnotation.name(),
				binderReferenceAnnotation.retrieval()
		);

		if ( !binderReference.isPresent() ) {
			throw MappingLog.INSTANCE.missingBinderReferenceInBinding();
		}

		return new BeanDelegatingBinder( binderReference.get() );
	}
}
