/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanDelegatingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.logging.impl.MappingLog;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.TypeMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;

public final class TypeBindingProcessor implements TypeMappingAnnotationProcessor<TypeBinding> {

	@Override
	public void process(TypeMappingStep mapping, TypeBinding annotation,
			TypeMappingAnnotationProcessorContext context) {
		TypeBinderRef typeBinder = annotation.binder();
		TypeBinder binder = createBinder( typeBinder, context );

		Map<String, Object> params = context.toMap( typeBinder.params() );
		mapping.binder( binder, params );
	}

	private TypeBinder createBinder(TypeBinderRef binderReferenceAnnotation, MappingAnnotationProcessorContext context) {
		Optional<BeanReference<? extends TypeBinder>> binderReference = context.toBeanReference(
				TypeBinder.class,
				TypeBinderRef.UndefinedBinderImplementationType.class,
				binderReferenceAnnotation.type(), binderReferenceAnnotation.name(),
				binderReferenceAnnotation.retrieval()
		);

		if ( !binderReference.isPresent() ) {
			throw MappingLog.INSTANCE.missingBinderReferenceInBinding();
		}

		return new BeanDelegatingBinder( binderReference.get() );
	}
}
