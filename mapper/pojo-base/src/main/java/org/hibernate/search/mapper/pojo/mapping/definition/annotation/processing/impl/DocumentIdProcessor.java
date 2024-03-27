/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanDelegatingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.MappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessor;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class DocumentIdProcessor implements PropertyMappingAnnotationProcessor<DocumentId> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public void process(PropertyMappingStep mappingContext, DocumentId annotation,
			PropertyMappingAnnotationProcessorContext context) {
		IdentifierBinder binder = createIdentifierBinder( annotation, context );

		IdentifierBinderRef identifierBinderRef = annotation.identifierBinder();
		Map<String, Object> params = context.toMap( identifierBinderRef.params() );
		mappingContext.documentId().identifierBinder( binder, params );
	}

	@SuppressWarnings("rawtypes") // Raw types are the best we can do here
	private IdentifierBinder createIdentifierBinder(DocumentId annotation, MappingAnnotationProcessorContext context) {
		IdentifierBridgeRef bridgeReferenceAnnotation = annotation.identifierBridge();
		IdentifierBinderRef binderReferenceAnnotation = annotation.identifierBinder();
		Optional<BeanReference<? extends IdentifierBridge>> bridgeReference = context.toBeanReference(
				IdentifierBridge.class,
				IdentifierBridgeRef.UndefinedBridgeImplementationType.class,
				bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name(),
				bridgeReferenceAnnotation.retrieval()
		);
		Optional<BeanReference<? extends IdentifierBinder>> binderReference = context.toBeanReference(
				IdentifierBinder.class,
				IdentifierBinderRef.UndefinedBinderImplementationType.class,
				binderReferenceAnnotation.type(), binderReferenceAnnotation.name(),
				binderReferenceAnnotation.retrieval()
		);

		if ( bridgeReference.isPresent() && binderReference.isPresent() ) {
			throw log.invalidDocumentIdDefiningBothBridgeReferenceAndBinderReference();
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
