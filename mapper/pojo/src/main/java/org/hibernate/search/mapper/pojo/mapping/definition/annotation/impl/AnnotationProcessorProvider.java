/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.mapping.definition.annotation.impl;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.PropertyBridge;
import org.hibernate.search.mapper.pojo.bridge.RoutingKeyBridge;
import org.hibernate.search.mapper.pojo.bridge.TypeBridge;
import org.hibernate.search.mapper.pojo.bridge.declaration.MarkerMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.PropertyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.RoutingKeyBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.declaration.TypeBridgeMapping;
import org.hibernate.search.mapper.pojo.bridge.mapping.BridgeBuilder;
import org.hibernate.search.mapper.pojo.bridge.mapping.MarkerBuilder;
import org.hibernate.search.mapper.pojo.dirtiness.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ContainerExtractorRef;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.IndexingDependencyMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyKeywordFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertySortableFieldMappingContext;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingContext;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.impl.common.CollectionHelper;
import org.hibernate.search.util.impl.common.LoggerFactory;

class AnnotationProcessorProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final List<TypeAnnotationProcessor<?>> typeAnnotationProcessors;
	private final List<PropertyAnnotationProcessor<?>> propertyAnnotationProcessors;

	AnnotationProcessorProvider(FailureCollector rootFailureCollector) {
		AnnotationProcessorHelper helper = new AnnotationProcessorHelper( rootFailureCollector );

		this.typeAnnotationProcessors = CollectionHelper.toImmutableList( CollectionHelper.asList(
				new RoutingKeyBridgeProcessor( helper ),
				new TypeBridgeProcessor( helper )
		) );

		this.propertyAnnotationProcessors = CollectionHelper.toImmutableList( CollectionHelper.asList(
				new MarkerProcessor( helper ),
				new AssociationInverseSideProcessor( helper ),
				new IndexingDependencyProcessor( helper ),
				new DocumentIdProcessor( helper ),
				new PropertyBridgeProcessor( helper ),
				new GenericFieldProcessor( helper ),
				new FullTextFieldProcessor( helper ),
				new KeywordFieldProcessor( helper ),
				new IndexedEmbeddedProcessor( helper )
		) );
	}

	List<TypeAnnotationProcessor<?>> getTypeAnnotationProcessors() {
		return typeAnnotationProcessors;
	}

	List<PropertyAnnotationProcessor<?>> getPropertyAnnotationProcessors() {
		return propertyAnnotationProcessors;
	}

	private static class MarkerProcessor extends PropertyAnnotationProcessor<Annotation> {
		MarkerProcessor(AnnotationProcessorHelper helper) {
			super( helper );
		}

		@Override
		Stream<? extends Annotation> extractAnnotations(PojoPropertyModel<?> propertyModel) {
			return propertyModel.getAnnotationsByMetaAnnotationType( MarkerMapping.class );
		}

		@Override
		void doProcess(PropertyMappingContext mappingContext,
				PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel, Annotation annotation) {
			MarkerBuilder builder = helper.createMarkerBuilder( annotation );
			mappingContext.marker( builder );
		}
	}

	private static class AssociationInverseSideProcessor extends PropertyAnnotationProcessor<AssociationInverseSide> {
		AssociationInverseSideProcessor(AnnotationProcessorHelper helper) {
			super( helper );
		}

		@Override
		Stream<? extends AssociationInverseSide> extractAnnotations(PojoPropertyModel<?> propertyModel) {
			return propertyModel.getAnnotationsByType( AssociationInverseSide.class );
		}

		@Override
		void doProcess(PropertyMappingContext mappingContext,
				PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel,
				AssociationInverseSide annotation) {
			ContainerExtractorPath extractorPath = helper.getExtractorPath( annotation.extractors() );

			Optional<PojoModelPathValueNode> inversePathOptional =
					helper.getPojoModelPathValueNode( annotation.inversePath() );
			if ( !inversePathOptional.isPresent() ) {
				throw log.missingInversePathInAssociationInverseSideMapping( typeModel, propertyModel.getName() );
			}

			mappingContext.associationInverseSide( inversePathOptional.get() )
					.withExtractors( extractorPath );
		}
	}

	private static class IndexingDependencyProcessor extends PropertyAnnotationProcessor<IndexingDependency> {
		IndexingDependencyProcessor(AnnotationProcessorHelper helper) {
			super( helper );
		}

		@Override
		Stream<? extends IndexingDependency> extractAnnotations(PojoPropertyModel<?> propertyModel) {
			return propertyModel.getAnnotationsByType( IndexingDependency.class );
		}

		@Override
		void doProcess(PropertyMappingContext mappingContext,
				PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel,
				IndexingDependency annotation) {
			ContainerExtractorPath extractorPath = helper.getExtractorPath( annotation.extractors() );

			ReindexOnUpdate reindexOnUpdate = annotation.reindexOnUpdate();

			IndexingDependencyMappingContext indexingDependencyContext = mappingContext.indexingDependency()
					.withExtractors( extractorPath );

			indexingDependencyContext.reindexOnUpdate( reindexOnUpdate );

			ObjectPath[] derivedFromAnnotations = annotation.derivedFrom();
			if ( derivedFromAnnotations.length > 0 ) {
				for ( ObjectPath objectPath : annotation.derivedFrom() ) {
					Optional<PojoModelPathValueNode> pojoModelPathOptional = helper.getPojoModelPathValueNode( objectPath );
					if ( !pojoModelPathOptional.isPresent() ) {
						throw log.missingPathInIndexingDependencyDerivedFrom( typeModel, propertyModel.getName() );
					}
					indexingDependencyContext.derivedFrom( pojoModelPathOptional.get() );
				}
			}
		}
	}

	private static class DocumentIdProcessor extends PropertyAnnotationProcessor<DocumentId> {
		DocumentIdProcessor(AnnotationProcessorHelper helper) {
			super( helper );
		}

		@Override
		Stream<? extends DocumentId> extractAnnotations(PojoPropertyModel<?> propertyModel) {
			return propertyModel.getAnnotationsByType( DocumentId.class );
		}

		@Override
		void doProcess(PropertyMappingContext mappingContext,
				PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel,
				DocumentId annotation) {
			BridgeBuilder<? extends IdentifierBridge<?>> builder =
					helper.createIdentifierBridgeBuilder( annotation, propertyModel );

			mappingContext.documentId().identifierBridge( builder );
		}
	}

	private static class RoutingKeyBridgeProcessor extends TypeAnnotationProcessor<Annotation> {
		RoutingKeyBridgeProcessor(AnnotationProcessorHelper helper) {
			super( helper );
		}

		@Override
		Stream<? extends Annotation> extractAnnotations(PojoRawTypeModel<?> typeModel) {
			return typeModel.getAnnotationsByMetaAnnotationType( RoutingKeyBridgeMapping.class );
		}

		@Override
		void doProcess(TypeMappingContext mappingContext, PojoRawTypeModel<?> typeModel, Annotation annotation) {
			BridgeBuilder<? extends RoutingKeyBridge> builder = helper.createRoutingKeyBridgeBuilder( annotation );
			mappingContext.routingKeyBridge( builder );
		}
	}

	private static class TypeBridgeProcessor extends TypeAnnotationProcessor<Annotation> {
		TypeBridgeProcessor(AnnotationProcessorHelper helper) {
			super( helper );
		}

		@Override
		Stream<? extends Annotation> extractAnnotations(PojoRawTypeModel<?> typeModel) {
			return typeModel.getAnnotationsByMetaAnnotationType( TypeBridgeMapping.class );
		}

		@Override
		void doProcess(TypeMappingContext mappingContext, PojoRawTypeModel<?> typeModel, Annotation annotation) {
			BridgeBuilder<? extends TypeBridge> builder = helper.createTypeBridgeBuilder( annotation );
			mappingContext.bridge( builder );
		}
	}

	private static class PropertyBridgeProcessor extends PropertyAnnotationProcessor<Annotation> {
		PropertyBridgeProcessor(AnnotationProcessorHelper helper) {
			super( helper );
		}

		@Override
		Stream<? extends Annotation> extractAnnotations(PojoPropertyModel<?> propertyModel) {
			return propertyModel.getAnnotationsByMetaAnnotationType( PropertyBridgeMapping.class );
		}

		@Override
		void doProcess(PropertyMappingContext mappingContext,
				PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel,
				Annotation annotation) {
			BridgeBuilder<? extends PropertyBridge> builder = helper.createPropertyBridgeBuilder( annotation );
			mappingContext.bridge( builder );
		}
	}

	private static class GenericFieldProcessor extends PropertySortableFieldAnnotationProcessor<GenericField> {
		GenericFieldProcessor(AnnotationProcessorHelper helper) {
			super( helper, GenericField.class );
		}

		@Override
		PropertySortableFieldMappingContext<?> initSortableFieldMappingContext(PropertyMappingContext mappingContext,
				PojoPropertyModel<?> propertyModel, GenericField annotation, String fieldName) {
			return mappingContext.genericField( fieldName );
		}

		@Override
		String getName(GenericField annotation) {
			return annotation.name();
		}

		@Override
		Projectable getProjectable(GenericField annotation) {
			return annotation.projectable();
		}

		@Override
		Sortable getSortable(GenericField annotation) {
			return annotation.sortable();
		}

		@Override
		ValueBridgeRef getValueBridge(GenericField annotation) {
			return annotation.valueBridge();
		}

		@Override
		ContainerExtractorRef[] getExtractors(GenericField annotation) {
			return annotation.extractors();
		}
	}

	private static class FullTextFieldProcessor extends PropertyFieldAnnotationProcessor<FullTextField> {
		FullTextFieldProcessor(AnnotationProcessorHelper helper) {
			super( helper, FullTextField.class );
		}

		@Override
		PropertyFieldMappingContext<?> initFieldMappingContext(PropertyMappingContext mappingContext,
				PojoPropertyModel<?> propertyModel, FullTextField annotation, String fieldName) {
			return mappingContext.fullTextField( fieldName )
					.analyzer( annotation.analyzer() );
		}

		@Override
		String getName(FullTextField annotation) {
			return annotation.name();
		}

		@Override
		Projectable getProjectable(FullTextField annotation) {
			return annotation.projectable();
		}

		@Override
		ValueBridgeRef getValueBridge(FullTextField annotation) {
			return annotation.valueBridge();
		}

		@Override
		ContainerExtractorRef[] getExtractors(FullTextField annotation) {
			return annotation.extractors();
		}
	}

	private static class KeywordFieldProcessor extends PropertySortableFieldAnnotationProcessor<KeywordField> {
		KeywordFieldProcessor(AnnotationProcessorHelper helper) {
			super( helper, KeywordField.class );
		}

		@Override
		PropertySortableFieldMappingContext<?> initSortableFieldMappingContext(PropertyMappingContext mappingContext,
				PojoPropertyModel<?> propertyModel, KeywordField annotation, String fieldName) {
			PropertyKeywordFieldMappingContext fieldContext = mappingContext.keywordField( fieldName );
			String normalizer = annotation.normalizer();
			if ( !normalizer.isEmpty() ) {
				fieldContext.normalizer( annotation.normalizer() );
			}
			return fieldContext;
		}

		@Override
		String getName(KeywordField annotation) {
			return annotation.name();
		}

		@Override
		Projectable getProjectable(KeywordField annotation) {
			return annotation.projectable();
		}

		@Override
		Sortable getSortable(KeywordField annotation) {
			return annotation.sortable();
		}

		@Override
		ValueBridgeRef getValueBridge(KeywordField annotation) {
			return annotation.valueBridge();
		}

		@Override
		ContainerExtractorRef[] getExtractors(KeywordField annotation) {
			return annotation.extractors();
		}
	}


	private static class IndexedEmbeddedProcessor extends PropertyAnnotationProcessor<IndexedEmbedded> {
		IndexedEmbeddedProcessor(AnnotationProcessorHelper helper) {
			super( helper );
		}

		@Override
		Stream<? extends IndexedEmbedded> extractAnnotations(PojoPropertyModel<?> propertyModel) {
			return propertyModel.getAnnotationsByType( IndexedEmbedded.class );
		}

		@Override
		void doProcess(PropertyMappingContext mappingContext,
				PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel,
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

			ContainerExtractorPath extractorPath = helper.getExtractorPath( annotation.extractors() );

			mappingContext.indexedEmbedded()
					.withExtractors( extractorPath )
					.prefix( cleanedUpPrefix )
					.storage( annotation.storage() )
					.maxDepth( cleanedUpMaxDepth )
					.includePaths( cleanedUpIncludePaths );
		}
	}
}
