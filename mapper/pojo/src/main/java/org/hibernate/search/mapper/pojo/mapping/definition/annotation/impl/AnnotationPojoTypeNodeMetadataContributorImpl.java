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
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.model.IndexSchemaFieldTypedContext;
import org.hibernate.search.engine.backend.document.model.Sortable;
import org.hibernate.search.engine.backend.document.model.Store;
import org.hibernate.search.engine.common.spi.BeanReference;
import org.hibernate.search.engine.common.spi.BeanResolver;
import org.hibernate.search.engine.common.spi.ImmutableBeanReference;
import org.hibernate.search.engine.mapper.mapping.building.spi.FieldModelContributor;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.FunctionBridge;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.declaration.PropertyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.PropertyBridgeMappingBuilderReference;
import org.hibernate.search.mapper.pojo.bridge.declaration.MarkerMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.MarkerMappingBuilderReference;
import org.hibernate.search.mapper.pojo.bridge.declaration.TypeBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.TypeBridgeMappingBuilderReference;
import org.hibernate.search.mapper.pojo.bridge.impl.BeanResolverBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationBridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.AnnotationMarkerBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoPropertyNodeModelCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeMetadataContributor;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoTypeNodeModelCollector;
import org.hibernate.search.mapper.pojo.mapping.building.impl.PojoValueNodeMappingCollector;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ContainerValueExtractorBeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Field;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FunctionBridgeBeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FunctionBridgeBuilderBeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdentifierBridgeBeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IdentifierBridgeBuilderBeanReference;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.model.spi.PropertyHandle;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoTypeModel;
import org.hibernate.search.util.spi.LoggerFactory;

class AnnotationPojoTypeNodeMetadataContributorImpl implements PojoTypeNodeMetadataContributor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanResolver beanResolver;
	private final PojoTypeModel<?> typeModel;

	AnnotationPojoTypeNodeMetadataContributorImpl(BeanResolver beanResolver, PojoTypeModel<?> typeModel) {
		this.beanResolver = beanResolver;
		this.typeModel = typeModel;
	}

	@Override
	public void contributeModel(PojoTypeNodeModelCollector collector) {
		typeModel.getDeclaredProperties()
				.forEach( property -> contributePropertyModel( collector, property ) );
	}

	@Override
	public void contributeMapping(PojoTypeNodeMappingCollector collector) {
		// FIXME annotation for routing key bridge
		// FIXME routing key bridge in programmatic mapping should probably be in the context of .indexed()?

		typeModel.getAnnotationsByMetaAnnotationType( TypeBridgeMapping.class )
				.forEach( annotation -> addTypeBridge( collector, annotation ) );

		typeModel.getDeclaredProperties()
				.forEach( property -> contributePropertyMapping( collector, property ) );
	}

	private void contributePropertyModel(PojoTypeNodeModelCollector collector, PojoPropertyModel<?> propertyModel) {
		String name = propertyModel.getName();
		propertyModel.getAnnotationsByMetaAnnotationType( MarkerMapping.class )
				.forEach( annotation -> addMarker( collector.property( name ), annotation ) );
	}

	private void contributePropertyMapping(PojoTypeNodeMappingCollector collector, PojoPropertyModel<?> propertyModel) {
		PropertyHandle handle = propertyModel.getHandle();
		propertyModel.getAnnotationsByType( DocumentId.class )
				.forEach( annotation -> addDocumentId( collector.property( handle ), propertyModel, annotation ) );
		propertyModel.getAnnotationsByMetaAnnotationType( PropertyBridgeMapping.class )
				.forEach( annotation -> addPropertyBridge( collector.property( handle ), annotation ) );
		propertyModel.getAnnotationsByType( Field.class )
				.forEach( annotation -> addField( collector.property( handle ), propertyModel, annotation ) );
		propertyModel.getAnnotationsByType( IndexedEmbedded.class )
				.forEach( annotation -> addIndexedEmbedded( collector.property( handle ), propertyModel, annotation ) );
	}

	private <A extends Annotation> void addMarker(PojoPropertyNodeModelCollector collector, A annotation) {
		AnnotationMarkerBuilder<A> builder = createMarkerBuilder( annotation );
		builder.initialize( annotation );
		collector.marker( builder );
	}

	private void addDocumentId(PojoPropertyNodeMappingCollector collector, PojoPropertyModel<?> propertyModel, DocumentId annotation) {
		BridgeBuilder<? extends IdentifierBridge<?>> builder = createIdentifierBridgeBuilder( annotation, propertyModel );

		collector.identifierBridge( builder );
	}

	private <A extends Annotation> void addTypeBridge(PojoTypeNodeMappingCollector collector, A annotation) {
		AnnotationBridgeBuilder<? extends TypeBridge, A> builder = createTypeBridgeBuilder( annotation );
		builder.initialize( annotation );
		collector.bridge( builder );
	}

	private <A extends Annotation> void addPropertyBridge(PojoPropertyNodeMappingCollector collector, A annotation) {
		AnnotationBridgeBuilder<? extends PropertyBridge, A> builder = createPropertyBridgeBuilder( annotation );
		builder.initialize( annotation );
		collector.bridge( builder );
	}

	private void addField(PojoPropertyNodeMappingCollector collector, PojoPropertyModel<?> propertyModel, Field annotation) {
		String cleanedUpFieldName = annotation.name();
		if ( cleanedUpFieldName.isEmpty() ) {
			cleanedUpFieldName = null;
		}

		BridgeBuilder<? extends FunctionBridge<?, ?>> builder = createFunctionBridgeBuilder( annotation, propertyModel );

		getValueNode( collector, annotation.extractors(), Field.DefaultExtractors.class )
				.functionBridge( builder, cleanedUpFieldName, new AnnotationFieldModelContributor( annotation ) );
	}

	private void addIndexedEmbedded(PojoPropertyNodeMappingCollector collector, PojoPropertyModel<?> propertyModel,
			IndexedEmbedded annotation) {
		String cleanedUpPrefix = annotation.prefix();
		if ( cleanedUpPrefix.isEmpty() ) {
			cleanedUpPrefix = null;
		}

		Integer cleanedUpMaxDepth = annotation.maxDepth();
		if ( cleanedUpMaxDepth.equals( -1 ) ) {
			cleanedUpMaxDepth = null;
		}

		String[] includePathsArray = annotation.includePaths();
		Set<String> cleanedUpIncludePaths;
		if ( includePathsArray.length > 0 ) {
			cleanedUpIncludePaths = new HashSet<>();
			Collections.addAll( cleanedUpIncludePaths, includePathsArray );
		}
		else {
			cleanedUpIncludePaths = Collections.emptySet();
		}

		getValueNode( collector, annotation.extractors(), IndexedEmbedded.DefaultExtractors.class )
				.indexedEmbedded( cleanedUpPrefix, annotation.storage(), cleanedUpMaxDepth, cleanedUpIncludePaths );
	}

	private PojoValueNodeMappingCollector getValueNode(PojoPropertyNodeMappingCollector collector,
			ContainerValueExtractorBeanReference[] extractors, Class<?> defaultExtractorsClass) {
		if ( extractors.length == 0 ) {
			return collector.valueWithoutExtractors();
		}
		else if ( extractors.length == 1 && defaultExtractorsClass.equals( extractors[0].type() ) ) {
			return collector.valueWithDefaultExtractors();
		}
		else {
			return collector.valueWithExtractors(
					Arrays.stream( extractors )
							.map( ContainerValueExtractorBeanReference::type )
							.collect( Collectors.toList() )
			);
		}
	}

	private <A extends Annotation> AnnotationMarkerBuilder<A> createMarkerBuilder(A annotation) {
		MarkerMapping markerMapping = annotation.annotationType().getAnnotation( MarkerMapping.class );
		MarkerMappingBuilderReference markerBuilderReferenceAnnotation = markerMapping.builder();
		BeanReference markerBuilderReference =
				toBeanReference(
						markerBuilderReferenceAnnotation.name(),
						markerBuilderReferenceAnnotation.type(),
						MarkerMappingBuilderReference.UndefinedImplementationType.class
				)
						.orElseThrow( () -> log.missingBuilderReferenceInBridgeMapping( annotation.annotationType() ) );

		// TODO check generic parameters of builder.getClass() somehow, maybe in a similar way to what we do in FunctionBridgeUtil
		return beanResolver.resolve( markerBuilderReference, AnnotationMarkerBuilder.class );
	}

	private BridgeBuilder<? extends IdentifierBridge<?>> createIdentifierBridgeBuilder(
			DocumentId annotation, PojoPropertyModel<?> propertyModel) {
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
			throw log.invalidDocumentIdDefiningBothBridgeReferenceAndBridgeBuilderReference( propertyModel.getName() );
		}
		else if ( bridgeReference.isPresent() ) {
			// The builder will return an object of some class T where T extends FunctionBridge<?, ?>, so this is safe
			@SuppressWarnings( "unchecked" )
			BridgeBuilder<? extends IdentifierBridge<?>> castedBuilder =
					new BeanResolverBridgeBuilder( IdentifierBridge.class, bridgeReference.get() );
			return castedBuilder;
		}
		else if ( bridgeBuilderReference.isPresent() ) {
			// TODO check generic parameters of builder.getClass() somehow, maybe in a similar way to what we do in FunctionBridgeUtil
			return beanResolver.resolve( bridgeBuilderReference.get(), BridgeBuilder.class );
		}
		else {
			// The bridge will be auto-detected from the property type
			return null;
		}
	}

	private <A extends Annotation> AnnotationBridgeBuilder<? extends TypeBridge, A> createTypeBridgeBuilder(
			A annotation) {
		TypeBridgeMapping bridgeMapping = annotation.annotationType().getAnnotation( TypeBridgeMapping.class );
		TypeBridgeMappingBuilderReference bridgeBuilderReferenceAnnotation = bridgeMapping.builder();
		BeanReference builderReference =
				toBeanReference(
						bridgeBuilderReferenceAnnotation.name(),
						bridgeBuilderReferenceAnnotation.type(),
						TypeBridgeMappingBuilderReference.UndefinedImplementationType.class
				)
						.orElseThrow( () -> log.missingBuilderReferenceInBridgeMapping( annotation.annotationType() ) );

		// TODO check generic parameters of builder.getClass() somehow, maybe in a similar way to what we do in FunctionBridgeUtil
		return beanResolver.resolve( builderReference, AnnotationBridgeBuilder.class );
	}

	private <A extends Annotation> AnnotationBridgeBuilder<? extends PropertyBridge, A> createPropertyBridgeBuilder(
			A annotation) {
		PropertyBridgeMapping bridgeMapping = annotation.annotationType().getAnnotation( PropertyBridgeMapping.class );
		PropertyBridgeMappingBuilderReference bridgeBuilderReferenceAnnotation = bridgeMapping.builder();
		BeanReference builderReference =
				toBeanReference(
						bridgeBuilderReferenceAnnotation.name(),
						bridgeBuilderReferenceAnnotation.type(),
						PropertyBridgeMappingBuilderReference.UndefinedImplementationType.class
				)
						.orElseThrow( () -> log.missingBuilderReferenceInBridgeMapping( annotation.annotationType() ) );

		// TODO check generic parameters of builder.getClass() somehow, maybe in a similar way to what we do in FunctionBridgeUtil
		return beanResolver.resolve( builderReference, AnnotationBridgeBuilder.class );
	}

	private BridgeBuilder<? extends FunctionBridge<?, ?>> createFunctionBridgeBuilder(
			Field annotation, PojoPropertyModel<?> propertyModel) {
		FunctionBridgeBeanReference bridgeReferenceAnnotation = annotation.functionBridge();
		Optional<BeanReference> bridgeReference = toBeanReference(
				bridgeReferenceAnnotation.name(),
				bridgeReferenceAnnotation.type(),
				FunctionBridgeBeanReference.UndefinedImplementationType.class
		);
		FunctionBridgeBuilderBeanReference bridgeBuilderReferenceAnnotation = annotation.functionBridgeBuilder();
		Optional<BeanReference> bridgeBuilderReference = toBeanReference(
				bridgeBuilderReferenceAnnotation.name(),
				bridgeBuilderReferenceAnnotation.type(),
				FunctionBridgeBuilderBeanReference.UndefinedImplementationType.class
		);

		if ( bridgeReference.isPresent() && bridgeBuilderReference.isPresent() ) {
			throw log.invalidFieldDefiningBothBridgeReferenceAndBridgeBuilderReference( propertyModel.getName() );
		}
		else if ( bridgeReference.isPresent() ) {
			// The builder will return an object of some class T where T extends FunctionBridge<?, ?>, so this is safe
			@SuppressWarnings( "unchecked" )
			BridgeBuilder<? extends FunctionBridge<?, ?>> castedBuilder =
					new BeanResolverBridgeBuilder( FunctionBridge.class, bridgeReference.get() );
			return castedBuilder;
		}
		else if ( bridgeBuilderReference.isPresent() ) {
			// TODO check generic parameters of builder.getClass() somehow, maybe in a similar way to what we do in FunctionBridgeUtil
			return beanResolver.resolve( bridgeBuilderReference.get(), BridgeBuilder.class );
		}
		else {
			// The bridge will be auto-detected from the property type
			return null;
		}
	}

	private Optional<BeanReference> toBeanReference(String name, Class<?> type, Class<?> undefinedTypeMarker) {
		String cleanedUpName = name.isEmpty() ? null : name;
		Class<?> cleanedUpType = undefinedTypeMarker.equals( type ) ? null : type;
		if ( cleanedUpName == null && cleanedUpType == null ) {
			return Optional.empty();
		}
		else {
			return Optional.of( new ImmutableBeanReference( cleanedUpName, cleanedUpType ) );
		}
	}

	private static class AnnotationFieldModelContributor implements FieldModelContributor {

		private final Field annotation;

		private AnnotationFieldModelContributor(Field annotation) {
			this.annotation = annotation;
		}

		@Override
		public void contribute(IndexSchemaFieldTypedContext<?> context) {
			if ( !Store.DEFAULT.equals( annotation.store() ) ) {
				context.store( annotation.store() );
			}
			if ( !Sortable.DEFAULT.equals( annotation.sortable() ) ) {
				context.sortable( annotation.sortable() );
			}
			if ( !annotation.analyzer().isEmpty() ) {
				context.analyzer( annotation.analyzer() );
			}
			if ( !annotation.normalizer().isEmpty() ) {
				context.normalizer( annotation.normalizer() );
			}
		}
	}
}
