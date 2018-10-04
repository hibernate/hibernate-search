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
import org.hibernate.search.engine.environment.bean.spi.ImmutableBeanReference;
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
import org.hibernate.search.mapper.pojo.bridge.impl.BeanResolverBridgeBuilder;
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
		BeanReference markerBuilderReference =
				toBeanReference(
						markerBuilderReferenceAnnotation.name(),
						markerBuilderReferenceAnnotation.type(),
						MarkerMappingBuilderReference.UndefinedImplementationType.class
				)
						.orElseThrow( () -> log.missingBuilderReferenceInMarkerMapping(
								MarkerMapping.class, annotation.annotationType() )
						);

		// TODO check generic parameters of builder.getClass() somehow, maybe in a similar way to what we do in PojoIndexModelBinderImpl#addValueBridge
		AnnotationMarkerBuilder<A> builder =
				beanProvider.getBean( markerBuilderReference, AnnotationMarkerBuilder.class );

		builder.initialize( annotation );

		return builder;
	}

	BridgeBuilder<? extends IdentifierBridge<?>> createIdentifierBridgeBuilder(
			DocumentId annotation, PojoPropertyModel<?> annotationHolder) {
		IdentifierBridgeBeanReference bridgeReferenceAnnotation = annotation.identifierBridge();
		Optional<BeanReference> bridgeReference = toBeanReference(
				bridgeReferenceAnnotation.name(),
				bridgeReferenceAnnotation.type(),
				IdentifierBridgeBeanReference.UndefinedImplementationType.class
		);
		IdentifierBridgeBuilderBeanReference bridgeBuilderReferenceAnnotation = annotation.identifierBridgeBuilder();
		Optional<BeanReference> bridgeBuilderReference = toBeanReference(
				bridgeBuilderReferenceAnnotation.name(),
				bridgeBuilderReferenceAnnotation.type(),
				IdentifierBridgeBuilderBeanReference.UndefinedImplementationType.class
		);

		if ( bridgeReference.isPresent() && bridgeBuilderReference.isPresent() ) {
			throw log.invalidDocumentIdDefiningBothBridgeReferenceAndBridgeBuilderReference( annotationHolder.getName() );
		}
		else if ( bridgeReference.isPresent() ) {
			// The builder will return an object of some class T where T extends ValueBridge<?, ?>, so this is safe
			@SuppressWarnings( "unchecked" )
			BridgeBuilder<? extends IdentifierBridge<?>> castedBuilder =
					new BeanResolverBridgeBuilder( IdentifierBridge.class, bridgeReference.get() );
			return castedBuilder;
		}
		else if ( bridgeBuilderReference.isPresent() ) {
			// TODO check generic parameters of builder.getClass() somehow, maybe in a similar way to what we do in PojoIndexModelBinderImpl#addValueBridge
			return beanProvider.getBean( bridgeBuilderReference.get(), BridgeBuilder.class );
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
				RoutingKeyBridgeMapping.class, RoutingKeyBridge.class, annotation,
				toBeanReference(
						bridgeReferenceAnnotation.name(),
						bridgeReferenceAnnotation.type(),
						RoutingKeyBridgeReference.UndefinedImplementationType.class
				),
				toBeanReference(
						bridgeBuilderReferenceAnnotation.name(),
						bridgeBuilderReferenceAnnotation.type(),
						RoutingKeyBridgeAnnotationBuilderReference.UndefinedImplementationType.class
				)
		);
	}

	<A extends Annotation> BridgeBuilder<? extends TypeBridge> createTypeBridgeBuilder(A annotation) {
		TypeBridgeMapping bridgeMapping = annotation.annotationType().getAnnotation( TypeBridgeMapping.class );
		TypeBridgeReference bridgeReferenceAnnotation = bridgeMapping.bridge();
		TypeBridgeAnnotationBuilderReference bridgeBuilderReferenceAnnotation = bridgeMapping.builder();

		return createAnnotationMappedBridgeBuilder(
				TypeBridgeMapping.class, TypeBridge.class, annotation,
				toBeanReference(
						bridgeReferenceAnnotation.name(),
						bridgeReferenceAnnotation.type(),
						TypeBridgeReference.UndefinedImplementationType.class
				),
				toBeanReference(
						bridgeBuilderReferenceAnnotation.name(),
						bridgeBuilderReferenceAnnotation.type(),
						TypeBridgeAnnotationBuilderReference.UndefinedImplementationType.class
				)
		);
	}

	<A extends Annotation> BridgeBuilder<? extends PropertyBridge> createPropertyBridgeBuilder(A annotation) {
		PropertyBridgeMapping bridgeMapping = annotation.annotationType().getAnnotation( PropertyBridgeMapping.class );
		PropertyBridgeReference bridgeReferenceAnnotation = bridgeMapping.bridge();
		PropertyBridgeAnnotationBuilderReference bridgeBuilderReferenceAnnotation = bridgeMapping.builder();

		return createAnnotationMappedBridgeBuilder(
				PropertyBridgeMapping.class, PropertyBridge.class, annotation,
				toBeanReference(
						bridgeReferenceAnnotation.name(),
						bridgeReferenceAnnotation.type(),
						PropertyBridgeReference.UndefinedImplementationType.class
				),
				toBeanReference(
						bridgeBuilderReferenceAnnotation.name(),
						bridgeBuilderReferenceAnnotation.type(),
						PropertyBridgeAnnotationBuilderReference.UndefinedImplementationType.class
				)
		);
	}

	BridgeBuilder<? extends ValueBridge<?, ?>> createValueBridgeBuilder(
			ValueBridgeBeanReference bridgeReferenceAnnotation,
			ValueBridgeBuilderBeanReference bridgeBuilderReferenceAnnotation,
			PojoPropertyModel<?> annotationHolder) {
		Optional<BeanReference> bridgeReference = toBeanReference(
				bridgeReferenceAnnotation.name(),
				bridgeReferenceAnnotation.type(),
				ValueBridgeBeanReference.UndefinedImplementationType.class
		);
		Optional<BeanReference> bridgeBuilderReference = toBeanReference(
				bridgeBuilderReferenceAnnotation.name(),
				bridgeBuilderReferenceAnnotation.type(),
				ValueBridgeBuilderBeanReference.UndefinedImplementationType.class
		);

		if ( bridgeReference.isPresent() && bridgeBuilderReference.isPresent() ) {
			throw log.invalidFieldDefiningBothBridgeReferenceAndBridgeBuilderReference( annotationHolder.getName() );
		}
		else if ( bridgeReference.isPresent() ) {
			// The builder will return an object of some class T where T extends ValueBridge<?, ?>, so this is safe
			@SuppressWarnings( "unchecked" )
			BridgeBuilder<? extends ValueBridge<?, ?>> castedBuilder =
					new BeanResolverBridgeBuilder( ValueBridge.class, bridgeReference.get() );
			return castedBuilder;
		}
		else if ( bridgeBuilderReference.isPresent() ) {
			// TODO check generic parameters of builder.getClass() somehow, maybe in a similar way to what we do in PojoIndexModelBinderImpl#addValueBridge
			return beanProvider.getBean( bridgeBuilderReference.get(), BridgeBuilder.class );
		}
		else {
			// The bridge will be auto-detected from the property type
			return null;
		}
	}

	private <A extends Annotation, B> BridgeBuilder<B> createAnnotationMappedBridgeBuilder(
			Class<? extends Annotation> bridgeMappingAnnotation, Class<B> bridgeClass, A annotation,
			Optional<BeanReference> bridgeReferenceOptional, Optional<BeanReference> builderReferenceOptional) {
		if ( bridgeReferenceOptional.isPresent() && builderReferenceOptional.isPresent() ) {
			throw log.conflictingBridgeReferenceInBridgeMapping( bridgeMappingAnnotation, annotation.annotationType() );
		}
		else if ( bridgeReferenceOptional.isPresent() ) {
			return new BeanResolverBridgeBuilder<>( bridgeClass, bridgeReferenceOptional.get() );
		}
		else if ( builderReferenceOptional.isPresent() ) {
			AnnotationBridgeBuilder builder = beanProvider.getBean( builderReferenceOptional.get(), AnnotationBridgeBuilder.class );
			// TODO check generic parameters of builder.getClass() somehow, maybe in a similar way to what we do in PojoIndexModelBinderImpl#addValueBridge
			builder.initialize( annotation );
			return builder;
		}
		else {
			throw log.missingBridgeReferenceInBridgeMapping( bridgeMappingAnnotation, annotation.annotationType() );
		}
	}

	Optional<BeanReference> toBeanReference(String name, Class<?> type, Class<?> undefinedTypeMarker) {
		String cleanedUpName = name.isEmpty() ? null : name;
		Class<?> cleanedUpType = undefinedTypeMarker.equals( type ) ? null : type;
		if ( cleanedUpName == null && cleanedUpType == null ) {
			return Optional.empty();
		}
		else {
			return Optional.of( new ImmutableBeanReference( cleanedUpName, cleanedUpType ) );
		}
	}
}
