/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Optional;

import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.MarkerBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.MarkerRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.PropertyBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.RoutingKeyBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingKeyBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.TypeBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.AnnotationInitializingBeanDelegatingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.impl.BeanDelegatingBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.ValueBinder;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtract;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.IdentifierBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

class AnnotationProcessorHelper {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final FailureCollector rootFailureCollector;

	AnnotationProcessorHelper(FailureCollector rootFailureCollector) {
		this.rootFailureCollector = rootFailureCollector;
	}

	FailureCollector getRootFailureCollector() {
		return rootFailureCollector;
	}

	Optional<PojoModelPathValueNode> getPojoModelPathValueNode(ObjectPath objectPath) {
		PropertyValue[] inversePathElements = objectPath.value();
		PojoModelPath.Builder inversePathBuilder = PojoModelPath.builder();
		for ( PropertyValue element : inversePathElements ) {
			String inversePropertyName = element.propertyName();
			ContainerExtractorPath inverseExtractorPath = getExtractorPath( element.extraction() );
			inversePathBuilder.property( inversePropertyName ).value( inverseExtractorPath );
		}
		return Optional.ofNullable( inversePathBuilder.toValuePathOrNull() );
	}

	ContainerExtractorPath getExtractorPath(ContainerExtraction extraction) {
		ContainerExtract extract = extraction.extract();
		String[] extractors = extraction.value();
		switch ( extract ) {
			case NO:
				if ( extractors.length != 0 ) {
					throw log.cannotReferenceExtractorsWhenExtractionDisabled();
				}
				return ContainerExtractorPath.noExtractors();
			case DEFAULT:
				if ( extractors.length == 0 ) {
					return ContainerExtractorPath.defaultExtractors();
				}
				else {
					return ContainerExtractorPath.explicitExtractors( Arrays.asList( extractors ) );
				}
			default:
				throw new AssertionFailure(
					"Unexpected " + ContainerExtract.class.getSimpleName() + " value: " + extract
				);
		}
	}

	<A extends Annotation> MarkerBinder createMarkerBinder(A annotation) {
		MarkerBinding markerBinding = annotation.annotationType().getAnnotation( MarkerBinding.class );
		MarkerRef binderReferenceAnnotation = markerBinding.marker();
		Optional<BeanReference<? extends MarkerBinder>> binderReference = toBeanReference(
				MarkerBinder.class,
				MarkerRef.UndefinedBinderImplementationType.class,
				binderReferenceAnnotation.binderType(), binderReferenceAnnotation.binderName()
		);

		if ( !binderReference.isPresent() ) {
			throw log.missingBinderReferenceInMarkerMapping(
					MarkerBinding.class, annotation.annotationType()
			);
		}

		MarkerBinder<A> binder = new AnnotationInitializingBeanDelegatingBinder<>( binderReference.get() );
		binder.initialize( annotation );
		return binder;
	}

	@SuppressWarnings("rawtypes") // Raw types are the best we can do here
	IdentifierBinder createIdentifierBinder(
			DocumentId annotation, PojoPropertyModel<?> annotationHolder) {
		IdentifierBridgeRef bridgeReferenceAnnotation = annotation.identifierBridge();
		Optional<BeanReference<? extends IdentifierBridge>> bridgeReference = toBeanReference(
				IdentifierBridge.class,
				IdentifierBridgeRef.UndefinedBridgeImplementationType.class,
				bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
		);
		Optional<BeanReference<? extends IdentifierBinder>> binderReference = toBeanReference(
				IdentifierBinder.class,
				IdentifierBridgeRef.UndefinedBinderImplementationType.class,
				bridgeReferenceAnnotation.binderType(), bridgeReferenceAnnotation.binderName()
		);

		if ( bridgeReference.isPresent() && binderReference.isPresent() ) {
			throw log.invalidDocumentIdDefiningBothBridgeReferenceAndBinderReference( annotationHolder.getName() );
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

	<A extends Annotation> RoutingKeyBinder createRoutingKeyBinder(A annotation) {
		RoutingKeyBinding bridgeMapping = annotation.annotationType().getAnnotation( RoutingKeyBinding.class );
		RoutingKeyBridgeRef bridgeReferenceAnnotation = bridgeMapping.bridge();
		Optional<BeanReference<? extends RoutingKeyBinder>> binderReference = toBeanReference(
				RoutingKeyBinder.class,
				RoutingKeyBridgeRef.UndefinedBinderImplementationType.class,
				bridgeReferenceAnnotation.binderType(), bridgeReferenceAnnotation.binderName()
		);

		if ( !binderReference.isPresent() ) {
			throw log.missingBinderReferenceInBridgeMapping(
					bridgeMapping.annotationType(), annotation.annotationType()
			);
		}

		RoutingKeyBinder<A> binder =
				new AnnotationInitializingBeanDelegatingBinder<>( binderReference.get() );
		binder.initialize( annotation );
		return binder;
	}

	<A extends Annotation> TypeBinder createTypeBinder(A annotation) {
		TypeBinding bridgeMapping = annotation.annotationType().getAnnotation( TypeBinding.class );
		TypeBridgeRef bridgeReferenceAnnotation = bridgeMapping.bridge();
		Optional<BeanReference<? extends TypeBinder>> binderReference = toBeanReference(
				TypeBinder.class,
				TypeBridgeRef.UndefinedBinderImplementationType.class,
				bridgeReferenceAnnotation.binderType(), bridgeReferenceAnnotation.binderName()
		);

		if ( !binderReference.isPresent() ) {
			throw log.missingBinderReferenceInBridgeMapping(
					bridgeMapping.annotationType(), annotation.annotationType()
			);
		}

		TypeBinder<A> binder = new AnnotationInitializingBeanDelegatingBinder<>( binderReference.get() );
		binder.initialize( annotation );
		return binder;
	}

	<A extends Annotation> PropertyBinder createPropertyBinder(A annotation) {
		PropertyBinding bridgeMapping = annotation.annotationType().getAnnotation( PropertyBinding.class );
		PropertyBridgeRef bridgeReferenceAnnotation = bridgeMapping.bridge();
		Optional<BeanReference<? extends PropertyBinder>> binderReference = toBeanReference(
				PropertyBinder.class,
				PropertyBridgeRef.UndefinedBinderImplementationType.class,
				bridgeReferenceAnnotation.binderType(), bridgeReferenceAnnotation.binderName()
		);

		if ( !binderReference.isPresent() ) {
			throw log.missingBinderReferenceInBridgeMapping(
					bridgeMapping.annotationType(), annotation.annotationType()
			);
		}

		PropertyBinder<A> binder =
				new AnnotationInitializingBeanDelegatingBinder<>( binderReference.get() );
		binder.initialize( annotation );
		return binder;
	}

	@SuppressWarnings("rawtypes") // Raw types are the best we can do here
	ValueBinder createValueBinder(
			ValueBridgeRef bridgeReferenceAnnotation,
			PojoPropertyModel<?> annotationHolder) {
		Optional<BeanReference<? extends ValueBridge>> bridgeReference = toBeanReference(
				ValueBridge.class,
				ValueBridgeRef.UndefinedBridgeImplementationType.class,
				bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
		);
		Optional<BeanReference<? extends ValueBinder>> binderReference = toBeanReference(
				ValueBinder.class,
				ValueBridgeRef.UndefinedBinderImplementationType.class,
				bridgeReferenceAnnotation.binderType(), bridgeReferenceAnnotation.binderName()
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

	private <T> Optional<BeanReference<? extends T>> toBeanReference(Class<T> expectedType, Class<?> undefinedTypeMarker,
			Class<? extends T> type, String name) {
		String cleanedUpName = name.isEmpty() ? null : name;
		Class<? extends T> cleanedUpType = undefinedTypeMarker.equals( type ) ? null : type;
		if ( cleanedUpName == null && cleanedUpType == null ) {
			return Optional.empty();
		}
		else {
			Class<? extends T> defaultedType = cleanedUpType == null ? expectedType : cleanedUpType;
			return Optional.of( BeanReference.of( defaultedType, cleanedUpName ) );
		}
	}

}
