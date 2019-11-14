/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.stream.Stream;

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
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.PropertyMappingAnnotationProcessorContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class DocumentIdProcessor extends PropertyAnnotationProcessor<DocumentId> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public Stream<? extends DocumentId> extractAnnotations(PojoPropertyModel<?> propertyModel) {
		return propertyModel.getAnnotationsByType( DocumentId.class );
	}

	@Override
	public void process(PropertyMappingStep mappingContext, DocumentId annotation,
			PropertyMappingAnnotationProcessorContext context) {
		IdentifierBinder binder = createIdentifierBinder( annotation, context );

		mappingContext.documentId().identifierBinder( binder );
	}

	@SuppressWarnings("rawtypes") // Raw types are the best we can do here
	private IdentifierBinder createIdentifierBinder(DocumentId annotation, MappingAnnotationProcessorContext context) {
		IdentifierBridgeRef bridgeReferenceAnnotation = annotation.identifierBridge();
		IdentifierBinderRef binderReferenceAnnotation = annotation.identifierBinder();
		Optional<BeanReference<? extends IdentifierBridge>> bridgeReference = context.toBeanReference(
				IdentifierBridge.class,
				IdentifierBridgeRef.UndefinedBridgeImplementationType.class,
				bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
		);
		Optional<BeanReference<? extends IdentifierBinder>> binderReference = context.toBeanReference(
				IdentifierBinder.class,
				IdentifierBinderRef.UndefinedBinderImplementationType.class,
				binderReferenceAnnotation.type(), binderReferenceAnnotation.name()
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
