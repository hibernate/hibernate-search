/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.MarkerBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanDelegatingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.logging.impl.MappingLog;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.MarkerBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;

public final class MarkerBindingProcessor implements PropertyMappingAnnotationProcessor<MarkerBinding> {

	@Override
	public void process(PropertyMappingStep mapping, MarkerBinding annotation,
			PropertyMappingAnnotationProcessorContext context) {
		MarkerBinderRef markerBinderRef = annotation.binder();
		MarkerBinder binder = createBinder( markerBinderRef, context );

		Map<String, Object> params = context.toMap( markerBinderRef.params() );
		mapping.marker( binder, params );
	}

	private MarkerBinder createBinder(MarkerBinderRef binderReferenceAnnotation, MappingAnnotationProcessorContext context) {
		Optional<BeanReference<? extends MarkerBinder>> binderReference = context.toBeanReference(
				MarkerBinder.class,
				MarkerBinderRef.UndefinedBinderImplementationType.class,
				binderReferenceAnnotation.type(), binderReferenceAnnotation.name(),
				binderReferenceAnnotation.retrieval()
		);

		if ( !binderReference.isPresent() ) {
			throw MappingLog.INSTANCE.missingBinderReferenceInBinding();
		}

		return new BeanDelegatingBinder( binderReference.get() );
	}
}
