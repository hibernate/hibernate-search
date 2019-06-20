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
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.MarkerMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.MarkerRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.PropertyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.PropertyBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.RoutingKeyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.RoutingKeyBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.TypeBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.impl.AnnotationInitializingBeanDelegatingBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.impl.AnnotationInitializingBeanDelegatingMarkerBuilder;
import org.hibernate.search.mapper.pojo.bridge.impl.BeanBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.impl.BeanDelegatingBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.AnnotationBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.AnnotationMarkerBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ContainerExtract;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ContainerExtraction;
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

	<A extends Annotation> MarkerBuilder createMarkerBuilder(A annotation) {
		MarkerMapping markerMapping = annotation.annotationType().getAnnotation( MarkerMapping.class );
		MarkerRef markerBuilderReferenceAnnotation = markerMapping.marker();

		return new AnnotationInitializingBeanDelegatingMarkerBuilder<>(
				toBeanReference(
						AnnotationMarkerBuilder.class,
						MarkerRef.UndefinedBuilderImplementationType.class,
						markerBuilderReferenceAnnotation.builderType(), markerBuilderReferenceAnnotation.builderName()
				)
						.orElseThrow( () -> log.missingBuilderReferenceInMarkerMapping(
								MarkerMapping.class, annotation.annotationType()
						) ),
				annotation
		);
	}

	@SuppressWarnings("rawtypes") // Raw types are the best we can do here
	BridgeBuilder<? extends IdentifierBridge<?>> createIdentifierBridgeBuilder(
			DocumentId annotation, PojoPropertyModel<?> annotationHolder) {
		IdentifierBridgeRef bridgeReferenceAnnotation = annotation.identifierBridge();
		Optional<BeanReference<? extends IdentifierBridge>> bridgeReference = toBeanReference(
				IdentifierBridge.class,
				IdentifierBridgeRef.UndefinedBridgeImplementationType.class,
				bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
		);
		Optional<BeanReference<? extends BridgeBuilder>> bridgeBuilderReference = toBeanReference(
				BridgeBuilder.class,
				IdentifierBridgeRef.UndefinedBuilderImplementationType.class,
				bridgeReferenceAnnotation.builderType(), bridgeReferenceAnnotation.builderName()
		);

		if ( bridgeReference.isPresent() && bridgeBuilderReference.isPresent() ) {
			throw log.invalidDocumentIdDefiningBothBridgeReferenceAndBridgeBuilderReference( annotationHolder.getName() );
		}
		else if ( bridgeReference.isPresent() ) {
			BridgeBuilder<? extends IdentifierBridge> rawBuilder =
					new BeanBridgeBuilder<>( bridgeReference.get() );
			// The builder will return an object of some class T where T extends IdentifierBridge, so this is safe
			@SuppressWarnings( "unchecked" )
			BridgeBuilder<? extends IdentifierBridge<?>> castedBuilder =
					(BridgeBuilder<? extends IdentifierBridge<?>>) rawBuilder;
			return castedBuilder;
		}
		else if ( bridgeBuilderReference.isPresent() ) {
			BridgeBuilder<? extends IdentifierBridge> rawBuilder =
					new BeanDelegatingBridgeBuilder<>( bridgeBuilderReference.get(), IdentifierBridge.class );
			// The builder will return an object of some class T where T extends IdentifierBridge, so this is safe
			@SuppressWarnings( "unchecked" )
			BridgeBuilder<? extends IdentifierBridge<?>> castedBuilder =
					(BridgeBuilder<? extends IdentifierBridge<?>>) rawBuilder;
			return castedBuilder;
		}
		else {
			// The bridge will be auto-detected from the property type
			return null;
		}
	}

	<A extends Annotation> BridgeBuilder<? extends RoutingKeyBridge> createRoutingKeyBridgeBuilder(A annotation) {
		RoutingKeyBridgeMapping bridgeMapping = annotation.annotationType().getAnnotation( RoutingKeyBridgeMapping.class );
		RoutingKeyBridgeRef bridgeReferenceAnnotation = bridgeMapping.bridge();

		return createAnnotationMappedBridgeBuilder(
				RoutingKeyBridge.class,
				RoutingKeyBridgeMapping.class,
				annotation,
				toBeanReference(
						RoutingKeyBridge.class,
						RoutingKeyBridgeRef.UndefinedBridgeImplementationType.class,
						bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
				),
				toBeanReference(
						AnnotationBridgeBuilder.class,
						RoutingKeyBridgeRef.UndefinedBuilderImplementationType.class,
						bridgeReferenceAnnotation.builderType(), bridgeReferenceAnnotation.builderName()
				)
		);
	}

	<A extends Annotation> BridgeBuilder<? extends TypeBridge> createTypeBridgeBuilder(A annotation) {
		TypeBridgeMapping bridgeMapping = annotation.annotationType().getAnnotation( TypeBridgeMapping.class );
		TypeBridgeRef bridgeReferenceAnnotation = bridgeMapping.bridge();

		return createAnnotationMappedBridgeBuilder(
				TypeBridge.class,
				TypeBridgeMapping.class,
				annotation,
				toBeanReference(
						TypeBridge.class,
						TypeBridgeRef.UndefinedBridgeImplementationType.class,
						bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
				),
				toBeanReference(
						AnnotationBridgeBuilder.class,
						TypeBridgeRef.UndefinedBuilderImplementationType.class,
						bridgeReferenceAnnotation.builderType(), bridgeReferenceAnnotation.builderName()
				)
		);
	}

	<A extends Annotation> BridgeBuilder<? extends PropertyBridge> createPropertyBridgeBuilder(A annotation) {
		PropertyBridgeMapping bridgeMapping = annotation.annotationType().getAnnotation( PropertyBridgeMapping.class );
		PropertyBridgeRef bridgeReferenceAnnotation = bridgeMapping.bridge();

		return createAnnotationMappedBridgeBuilder(
				PropertyBridge.class,
				PropertyBridgeMapping.class,
				annotation,
				toBeanReference(
						PropertyBridge.class,
						PropertyBridgeRef.UndefinedBridgeImplementationType.class,
						bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
				),
				toBeanReference(
						AnnotationBridgeBuilder.class,
						PropertyBridgeRef.UndefinedBuilderImplementationType.class,
						bridgeReferenceAnnotation.builderType(), bridgeReferenceAnnotation.builderName()
				)
		);
	}

	@SuppressWarnings("rawtypes") // Raw types are the best we can do here
	BridgeBuilder<? extends ValueBridge<?, ?>> createValueBridgeBuilder(
			ValueBridgeRef bridgeReferenceAnnotation,
			PojoPropertyModel<?> annotationHolder) {
		Optional<BeanReference<? extends ValueBridge>> bridgeReference = toBeanReference(
				ValueBridge.class,
				ValueBridgeRef.UndefinedBridgeImplementationType.class,
				bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
		);
		Optional<BeanReference<? extends BridgeBuilder>> bridgeBuilderReference = toBeanReference(
				BridgeBuilder.class,
				ValueBridgeRef.UndefinedBuilderImplementationType.class,
				bridgeReferenceAnnotation.builderType(), bridgeReferenceAnnotation.builderName()
		);

		if ( bridgeReference.isPresent() && bridgeBuilderReference.isPresent() ) {
			throw log.invalidFieldDefiningBothBridgeReferenceAndBridgeBuilderReference( annotationHolder.getName() );
		}
		else if ( bridgeReference.isPresent() ) {
			BridgeBuilder<? extends ValueBridge> rawBuilder =
					new BeanBridgeBuilder<>( bridgeReference.get() );
			// The builder will return an object of some class T where T extends IdentifierBridge, so this is safe
			@SuppressWarnings( "unchecked" )
			BridgeBuilder<? extends ValueBridge<?, ?>> castedBuilder =
					(BridgeBuilder<? extends ValueBridge<?, ?>>) rawBuilder;
			return castedBuilder;
		}
		else if ( bridgeBuilderReference.isPresent() ) {
			BridgeBuilder<? extends ValueBridge> rawBuilder =
					new BeanDelegatingBridgeBuilder<>( bridgeBuilderReference.get(), ValueBridge.class );
			// The builder will return an object of some class T where T extends IdentifierBridge, so this is safe
			@SuppressWarnings( "unchecked" )
			BridgeBuilder<? extends ValueBridge<?, ?>> castedBuilder =
					(BridgeBuilder<? extends ValueBridge<?, ?>>) rawBuilder;
			return castedBuilder;
		}
		else {
			// The bridge will be auto-detected from the property type
			return null;
		}
	}

	@SuppressWarnings("rawtypes") // The bean reference must be to a raw type
	private <A extends Annotation, B> BridgeBuilder<? extends B> createAnnotationMappedBridgeBuilder(
			Class<B> expectedBridgeType, Class<? extends Annotation> bridgeMappingAnnotation, A annotation,
			Optional<BeanReference<? extends B>> bridgeReferenceOptional,
			Optional<BeanReference<? extends AnnotationBridgeBuilder>> builderReferenceOptional) {
		if ( bridgeReferenceOptional.isPresent() && builderReferenceOptional.isPresent() ) {
			throw log.conflictingBridgeReferenceInBridgeMapping( bridgeMappingAnnotation, annotation.annotationType() );
		}
		else if ( bridgeReferenceOptional.isPresent() ) {
			return new BeanBridgeBuilder<>( bridgeReferenceOptional.get() );
		}
		else if ( builderReferenceOptional.isPresent() ) {
			return new AnnotationInitializingBeanDelegatingBridgeBuilder<>(
					builderReferenceOptional.get(),
					expectedBridgeType,
					annotation
			);
		}
		else {
			throw log.missingBridgeReferenceInBridgeMapping( bridgeMappingAnnotation, annotation.annotationType() );
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
