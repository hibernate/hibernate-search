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
import java.util.stream.Collectors;

import org.hibernate.search.engine.environment.bean.BeanProvider;
import org.hibernate.search.engine.environment.bean.BeanReference;
import org.hibernate.search.engine.logging.spi.FailureCollector;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.declaration.MarkerMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.MarkerMappingBuilderReference;
import org.hibernate.search.mapper.pojo.bridge.declaration.PropertyBridgeAnnotationBuilderReference;
import org.hibernate.search.mapper.pojo.bridge.declaration.PropertyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.PropertyBridgeReference;
import org.hibernate.search.mapper.pojo.bridge.declaration.RoutingKeyBridgeAnnotationBuilderReference;
import org.hibernate.search.mapper.pojo.bridge.declaration.RoutingKeyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.RoutingKeyBridgeReference;
import org.hibernate.search.mapper.pojo.bridge.declaration.TypeBridgeAnnotationBuilderReference;
import org.hibernate.search.mapper.pojo.bridge.declaration.TypeBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.TypeBridgeReference;
import org.hibernate.search.mapper.pojo.bridge.impl.BeanBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationMarkerBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuilder;
import org.hibernate.search.mapper.pojo.extractor.ContainerValueExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ContainerValueExtractorBeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdentifierBridgeBeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdentifierBridgeBuilderBeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ValueBridgeBeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ValueBridgeBuilderBeanReference;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPath;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.util.impl.common.LoggerFactory;

class AnnotationProcessorHelper {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanProvider beanProvider;
	private final FailureCollector rootFailureCollector;

	AnnotationProcessorHelper(BeanProvider beanProvider, FailureCollector rootFailureCollector) {
		this.beanProvider = beanProvider;
		this.rootFailureCollector = rootFailureCollector;
	}

	FailureCollector getRootFailureCollector() {
		return rootFailureCollector;
	}

	Optional<PojoModelPathValueNode> getPojoModelPathValueNode(ObjectPath objectPath) {
		PropertyValue[] inversePathElements = objectPath.value();
		PojoModelPathValueNode inversePath = null;
		for ( PropertyValue element : inversePathElements ) {
			String inversePropertyName = element.propertyName();
			ContainerValueExtractorPath inverseExtractorPath = getExtractorPath(
					element.extractors()
			);
			if ( inversePath == null ) {
				inversePath = PojoModelPath.fromRoot( inversePropertyName ).value( inverseExtractorPath );
			}
			else {
				inversePath = inversePath.property( inversePropertyName ).value( inverseExtractorPath );
			}
		}
		return Optional.ofNullable( inversePath );
	}

	ContainerValueExtractorPath getExtractorPath(ContainerValueExtractorBeanReference[] extractors) {
		if ( extractors.length == 0 ) {
			return ContainerValueExtractorPath.noExtractors();
		}
		else if ( extractors.length == 1 && ContainerValueExtractorBeanReference.DefaultExtractors.class.equals( extractors[0].type() ) ) {
			return ContainerValueExtractorPath.defaultExtractors();
		}
		else {
			return ContainerValueExtractorPath.explicitExtractors(
					Arrays.stream( extractors )
							.map( ContainerValueExtractorBeanReference::type )
							.collect( Collectors.toList() )
			);
		}
	}

	<A extends Annotation> MarkerBuilder createMarkerBuilder(A annotation) {
		MarkerMapping markerMapping = annotation.annotationType().getAnnotation( MarkerMapping.class );
		MarkerMappingBuilderReference markerBuilderReferenceAnnotation = markerMapping.builder();

		// TODO check generic parameters of builder.getClass() somehow, maybe in a similar way to what we do in PojoIndexModelBinderImpl#addValueBridge
		AnnotationMarkerBuilder<A> builder =
				toBeanReference(
						AnnotationMarkerBuilder.class,
						MarkerMappingBuilderReference.UndefinedImplementationType.class,
						markerBuilderReferenceAnnotation.type(), markerBuilderReferenceAnnotation.name()
				)
						.orElseThrow( () -> log.missingBuilderReferenceInMarkerMapping(
								MarkerMapping.class, annotation.annotationType()
						) )
						.getBean( beanProvider );

		builder.initialize( annotation );

		return builder;
	}

	BridgeBuilder<? extends IdentifierBridge<?>> createIdentifierBridgeBuilder(
			DocumentId annotation, PojoPropertyModel<?> annotationHolder) {
		IdentifierBridgeBeanReference bridgeReferenceAnnotation = annotation.identifierBridge();
		@SuppressWarnings("rawtypes") // Raw types are the best we can do here
		Optional<BeanReference<? extends IdentifierBridge>> bridgeReference = toBeanReference(
				IdentifierBridge.class,
				IdentifierBridgeBeanReference.UndefinedImplementationType.class,
				bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
		);
		IdentifierBridgeBuilderBeanReference bridgeBuilderReferenceAnnotation = annotation.identifierBridgeBuilder();
		@SuppressWarnings("rawtypes") // Raw types are the best we can do here
		Optional<BeanReference<? extends BridgeBuilder>> bridgeBuilderReference = toBeanReference(
				BridgeBuilder.class,
				IdentifierBridgeBuilderBeanReference.UndefinedImplementationType.class,
				bridgeBuilderReferenceAnnotation.type(), bridgeBuilderReferenceAnnotation.name()
		);

		if ( bridgeReference.isPresent() && bridgeBuilderReference.isPresent() ) {
			throw log.invalidDocumentIdDefiningBothBridgeReferenceAndBridgeBuilderReference( annotationHolder.getName() );
		}
		else if ( bridgeReference.isPresent() ) {
			// The builder will return an object of some class T where T extends IdentifierBridge, so this is safe
			@SuppressWarnings( "unchecked" )
			BridgeBuilder<? extends IdentifierBridge<?>> castedBuilder =
					(BridgeBuilder<? extends IdentifierBridge<?>>) new BeanBridgeBuilder<>( bridgeReference.get() );
			return castedBuilder;
		}
		else if ( bridgeBuilderReference.isPresent() ) {
			// TODO check generic parameters of builder.getClass() somehow, maybe in a similar way to what we do in PojoIndexModelBinderImpl#addValueBridge
			return bridgeBuilderReference.get().getBean( beanProvider );
		}
		else {
			// The bridge will be auto-detected from the property type
			return null;
		}
	}

	<A extends Annotation> BridgeBuilder<? extends RoutingKeyBridge> createRoutingKeyBridgeBuilder(A annotation) {
		RoutingKeyBridgeMapping bridgeMapping = annotation.annotationType().getAnnotation( RoutingKeyBridgeMapping.class );
		RoutingKeyBridgeReference bridgeReferenceAnnotation = bridgeMapping.bridge();
		RoutingKeyBridgeAnnotationBuilderReference bridgeBuilderReferenceAnnotation = bridgeMapping.builder();

		return createAnnotationMappedBridgeBuilder(
				RoutingKeyBridgeMapping.class, annotation,
				toBeanReference(
						RoutingKeyBridge.class,
						RoutingKeyBridgeReference.UndefinedImplementationType.class,
						bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
				),
				toBeanReference(
						AnnotationBridgeBuilder.class,
						RoutingKeyBridgeAnnotationBuilderReference.UndefinedImplementationType.class,
						bridgeBuilderReferenceAnnotation.type(), bridgeBuilderReferenceAnnotation.name()
				)
		);
	}

	<A extends Annotation> BridgeBuilder<? extends TypeBridge> createTypeBridgeBuilder(A annotation) {
		TypeBridgeMapping bridgeMapping = annotation.annotationType().getAnnotation( TypeBridgeMapping.class );
		TypeBridgeReference bridgeReferenceAnnotation = bridgeMapping.bridge();
		TypeBridgeAnnotationBuilderReference bridgeBuilderReferenceAnnotation = bridgeMapping.builder();

		return createAnnotationMappedBridgeBuilder(
				TypeBridgeMapping.class, annotation,
				toBeanReference(
						TypeBridge.class,
						TypeBridgeReference.UndefinedImplementationType.class,
						bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
				),
				toBeanReference(
						AnnotationBridgeBuilder.class,
						TypeBridgeAnnotationBuilderReference.UndefinedImplementationType.class,
						bridgeBuilderReferenceAnnotation.type(), bridgeBuilderReferenceAnnotation.name()
				)
		);
	}

	<A extends Annotation> BridgeBuilder<? extends PropertyBridge> createPropertyBridgeBuilder(A annotation) {
		PropertyBridgeMapping bridgeMapping = annotation.annotationType().getAnnotation( PropertyBridgeMapping.class );
		PropertyBridgeReference bridgeReferenceAnnotation = bridgeMapping.bridge();
		PropertyBridgeAnnotationBuilderReference bridgeBuilderReferenceAnnotation = bridgeMapping.builder();

		return createAnnotationMappedBridgeBuilder(
				PropertyBridgeMapping.class, annotation,
				toBeanReference(
						PropertyBridge.class,
						PropertyBridgeReference.UndefinedImplementationType.class,
						bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
				),
				toBeanReference(
						AnnotationBridgeBuilder.class,
						PropertyBridgeAnnotationBuilderReference.UndefinedImplementationType.class,
						bridgeBuilderReferenceAnnotation.type(), bridgeBuilderReferenceAnnotation.name()
				)
		);
	}

	BridgeBuilder<? extends ValueBridge<?, ?>> createValueBridgeBuilder(
			ValueBridgeBeanReference bridgeReferenceAnnotation,
			ValueBridgeBuilderBeanReference bridgeBuilderReferenceAnnotation,
			PojoPropertyModel<?> annotationHolder) {
		@SuppressWarnings("rawtypes") // Raw types are the best we can do here
		Optional<BeanReference<? extends ValueBridge>> bridgeReference = toBeanReference(
				ValueBridge.class,
				ValueBridgeBeanReference.UndefinedImplementationType.class,
				bridgeReferenceAnnotation.type(), bridgeReferenceAnnotation.name()
		);
		@SuppressWarnings("rawtypes") // Raw types are the best we can do here
		Optional<BeanReference<? extends BridgeBuilder>> bridgeBuilderReference = toBeanReference(
				BridgeBuilder.class,
				ValueBridgeBuilderBeanReference.UndefinedImplementationType.class,
				bridgeBuilderReferenceAnnotation.type(), bridgeBuilderReferenceAnnotation.name()
		);

		if ( bridgeReference.isPresent() && bridgeBuilderReference.isPresent() ) {
			throw log.invalidFieldDefiningBothBridgeReferenceAndBridgeBuilderReference( annotationHolder.getName() );
		}
		else if ( bridgeReference.isPresent() ) {
			// The builder will return an object of some class T where T extends ValueBridge<?, ?>, so this is safe
			@SuppressWarnings( "unchecked" )
			BridgeBuilder<? extends ValueBridge<?, ?>> castedBuilder =
					(BridgeBuilder<? extends ValueBridge<?, ?>>) new BeanBridgeBuilder<>( bridgeReference.get() );
			return castedBuilder;
		}
		else if ( bridgeBuilderReference.isPresent() ) {
			// TODO check generic parameters of builder.getClass() somehow, maybe in a similar way to what we do in PojoIndexModelBinderImpl#addValueBridge
			return bridgeBuilderReference.get().getBean( beanProvider );
		}
		else {
			// The bridge will be auto-detected from the property type
			return null;
		}
	}

	private <A extends Annotation, B> BridgeBuilder<? extends B> createAnnotationMappedBridgeBuilder(
			Class<? extends Annotation> bridgeMappingAnnotation, A annotation,
			Optional<BeanReference<? extends B>> bridgeReferenceOptional,
			Optional<BeanReference<? extends AnnotationBridgeBuilder>> builderReferenceOptional) {
		if ( bridgeReferenceOptional.isPresent() && builderReferenceOptional.isPresent() ) {
			throw log.conflictingBridgeReferenceInBridgeMapping( bridgeMappingAnnotation, annotation.annotationType() );
		}
		else if ( bridgeReferenceOptional.isPresent() ) {
			return new BeanBridgeBuilder<>( bridgeReferenceOptional.get() );
		}
		else if ( builderReferenceOptional.isPresent() ) {
			AnnotationBridgeBuilder builder = builderReferenceOptional.get().getBean( beanProvider );
			// TODO check generic parameters of builder.getClass() somehow, maybe in a similar way to what we do in PojoIndexModelBinderImpl#addValueBridge
			builder.initialize( annotation );
			return builder;
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
