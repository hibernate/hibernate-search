/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanDelegatingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class AbstractFieldAnnotationProcessor<A extends Annotation> implements PropertyMappingAnnotationProcessor<A> {


	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public final void process(PropertyMappingStep mappingContext, A annotation,
			PropertyMappingAnnotationProcessorContext context) {
		String cleanedUpRelativeFieldName = context.toNullIfDefault( getName( annotation ), "" );
		PropertyMappingFieldOptionsStep<?> fieldContext =
				initFieldMappingContext( mappingContext, annotation, cleanedUpRelativeFieldName );

		ValueBinderRef valueBinder = getValueBinder( annotation );
		ValueBinder binder = createValueBinder(
				getValueBridge( annotation ),
				valueBinder,
				context
		);

		Map<String, Object> params = context.toMap( valueBinder.params() );
		fieldContext.valueBinder( binder, params );

		ContainerExtractorPath extractorPath = context.toContainerExtractorPath( getExtraction( annotation ) );
		fieldContext.extractors( extractorPath );
	}

	abstract PropertyMappingFieldOptionsStep<?> initFieldMappingContext(PropertyMappingStep mappingContext,
			A annotation, String fieldName);

	abstract ContainerExtraction getExtraction(A annotation);

	abstract String getName(A annotation);

	abstract ValueBridgeRef getValueBridge(A annotation);

	abstract ValueBinderRef getValueBinder(A annotation);

	@SuppressWarnings("rawtypes") // Raw types are the best we can do here
	private ValueBinder createValueBinder(ValueBridgeRef bridgeReferenceAnnotation,
			ValueBinderRef binderReferenceAnnotation, MappingAnnotationProcessorContext context) {
		Optional<BeanReference<? extends ValueBridge>> bridgeReference = Optional.empty();
		if ( bridgeReferenceAnnotation != null ) {
			bridgeReference = context.toBeanReference(
					ValueBridge.class,
					ValueBridgeRef.UndefinedBridgeImplementationType.class,
					bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name(),
					bridgeReferenceAnnotation.retrieval()
			);
		}
		Optional<BeanReference<? extends ValueBinder>> binderReference = context.toBeanReference(
				ValueBinder.class,
				ValueBinderRef.UndefinedBinderImplementationType.class,
				binderReferenceAnnotation.type(), binderReferenceAnnotation.name(),
				binderReferenceAnnotation.retrieval()
		);

		if ( bridgeReference.isPresent() && binderReference.isPresent() ) {
			throw log.invalidFieldDefiningBothBridgeReferenceAndBinderReference();
		}
		else if ( bridgeReference.isPresent() ) {
			return new BeanBinder( bridgeReference.get() );
		}
		else if ( binderReference.isPresent() ) {
			return new BeanDelegatingBinder( binderReference.get() );
		}
		else {
			// The bridge will be auto-detected from the property type
			return null;
		}
	}
}
