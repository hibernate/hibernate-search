/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.processing.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.stream.Stream;

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
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

abstract class PropertyFieldAnnotationProcessor<A extends Annotation> extends PropertyAnnotationProcessor<A> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Class<A> annotationType;

	PropertyFieldAnnotationProcessor(AnnotationProcessorHelper helper, Class<A> annotationType) {
		super( helper );
		this.annotationType = annotationType;
	}

	@Override
	public final Stream<? extends A> extractAnnotations(PojoPropertyModel<?> propertyModel) {
		return propertyModel.getAnnotationsByType( annotationType );
	}

	@Override
	public final void process(PropertyMappingStep mappingContext,
			PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel,
			A annotation) {
		String cleanedUpRelativeFieldName = getName( annotation );
		if ( cleanedUpRelativeFieldName.isEmpty() ) {
			cleanedUpRelativeFieldName = null;
		}

		PropertyMappingFieldOptionsStep<?> fieldContext =
				initFieldMappingContext( mappingContext, propertyModel, annotation, cleanedUpRelativeFieldName );

		ValueBinder binder = createValueBinder(
				getValueBridge( annotation ),
				getValueBinder( annotation ),
				propertyModel
		);
		fieldContext.valueBinder( binder );

		ContainerExtractorPath extractorPath =
				helper.getExtractorPath( getExtraction( annotation ) );
		fieldContext.extractors( extractorPath );
	}

	abstract PropertyMappingFieldOptionsStep<?> initFieldMappingContext(PropertyMappingStep mappingContext,
			PojoPropertyModel<?> propertyModel, A annotation, String fieldName);

	abstract String getName(A annotation);

	abstract ValueBridgeRef getValueBridge(A annotation);

	abstract ValueBinderRef getValueBinder(A annotation);

	abstract ContainerExtraction getExtraction(A annotation);

	@SuppressWarnings("rawtypes") // Raw types are the best we can do here
	private ValueBinder createValueBinder(
			ValueBridgeRef bridgeReferenceAnnotation,
			ValueBinderRef binderReferenceAnnotation,
			PojoPropertyModel<?> annotationHolder) {
		Optional<BeanReference<? extends ValueBridge>> bridgeReference = Optional.empty();
		if ( bridgeReferenceAnnotation != null ) {
			bridgeReference = helper.toBeanReference(
					ValueBridge.class,
					ValueBridgeRef.UndefinedBridgeImplementationType.class,
					bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
			);
		}
		Optional<BeanReference<? extends ValueBinder>> binderReference = helper.toBeanReference(
				ValueBinder.class,
				ValueBinderRef.UndefinedBinderImplementationType.class,
				binderReferenceAnnotation.type(), binderReferenceAnnotation.name()
		);

		if ( bridgeReference.isPresent() && binderReference.isPresent() ) {
			throw log.invalidFieldDefiningBothBridgeReferenceAndBinderReference( annotationHolder.getName() );
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
