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

import org.hibernate.search.engine.backend.types.Aggregable;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.engine.reporting.spi.FailureCollector;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBinderRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.MarkerBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.PropertyBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.RoutingKeyBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.declaration.TypeBinding;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.IdentifierBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.MarkerBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.PropertyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.RoutingKeyBinder;
import org.hibernate.search.mapper.pojo.bridge.mapping.programmatic.TypeBinder;
import org.hibernate.search.mapper.pojo.dirtiness.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.extractor.mapping.annotation.ContainerExtraction;
import org.hibernate.search.mapper.pojo.extractor.mapping.programmatic.ContainerExtractorPath;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AnnotationDefaultValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ScaledNumberField;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.IndexingDependencyOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingKeywordFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingNonFullTextFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingScaledNumberFieldOptionsStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.PropertyMappingStep;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.mapper.pojo.model.path.PojoModelPathValueNode;
import org.hibernate.search.mapper.pojo.model.spi.PojoPropertyModel;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeModel;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

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
				new ScaledNumberFieldProcessor( helper ),
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
			return propertyModel.getAnnotationsByMetaAnnotationType( MarkerBinding.class );
		}

		@Override
		void doProcess(PropertyMappingStep mappingContext,
				PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel, Annotation annotation) {
			MarkerBinder<?> binder = helper.createMarkerBinder( annotation );
			mappingContext.marker( binder );
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
		void doProcess(PropertyMappingStep mappingContext,
				PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel,
				AssociationInverseSide annotation) {
			ContainerExtractorPath extractorPath = helper.getExtractorPath( annotation.extraction() );

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
		void doProcess(PropertyMappingStep mappingContext,
				PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel,
				IndexingDependency annotation) {
			ContainerExtractorPath extractorPath = helper.getExtractorPath( annotation.extraction() );

			ReindexOnUpdate reindexOnUpdate = annotation.reindexOnUpdate();

			IndexingDependencyOptionsStep indexingDependencyContext = mappingContext.indexingDependency()
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
		void doProcess(PropertyMappingStep mappingContext,
				PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel,
				DocumentId annotation) {
			IdentifierBinder binder =
					helper.createIdentifierBinder( annotation, propertyModel );

			mappingContext.documentId().identifierBinder( binder );
		}
	}

	private static class RoutingKeyBridgeProcessor extends TypeAnnotationProcessor<Annotation> {
		RoutingKeyBridgeProcessor(AnnotationProcessorHelper helper) {
			super( helper );
		}

		@Override
		Stream<? extends Annotation> extractAnnotations(PojoRawTypeModel<?> typeModel) {
			return typeModel.getAnnotationsByMetaAnnotationType( RoutingKeyBinding.class );
		}

		@Override
		void doProcess(TypeMappingStep mappingContext, PojoRawTypeModel<?> typeModel, Annotation annotation) {
			RoutingKeyBinder<?> binder = helper.createRoutingKeyBinder( annotation );
			mappingContext.routingKeyBinder( binder );
		}
	}

	private static class TypeBridgeProcessor extends TypeAnnotationProcessor<Annotation> {
		TypeBridgeProcessor(AnnotationProcessorHelper helper) {
			super( helper );
		}

		@Override
		Stream<? extends Annotation> extractAnnotations(PojoRawTypeModel<?> typeModel) {
			return typeModel.getAnnotationsByMetaAnnotationType( TypeBinding.class );
		}

		@Override
		void doProcess(TypeMappingStep mappingContext, PojoRawTypeModel<?> typeModel, Annotation annotation) {
			TypeBinder<?> binder = helper.createTypeBinder( annotation );
			mappingContext.binder( binder );
		}
	}

	private static class PropertyBridgeProcessor extends PropertyAnnotationProcessor<Annotation> {
		PropertyBridgeProcessor(AnnotationProcessorHelper helper) {
			super( helper );
		}

		@Override
		Stream<? extends Annotation> extractAnnotations(PojoPropertyModel<?> propertyModel) {
			return propertyModel.getAnnotationsByMetaAnnotationType( PropertyBinding.class );
		}

		@Override
		void doProcess(PropertyMappingStep mappingContext,
				PojoRawTypeModel<?> typeModel, PojoPropertyModel<?> propertyModel,
				Annotation annotation) {
			PropertyBinder<?> binder = helper.createPropertyBinder( annotation );
			mappingContext.binder( binder );
		}
	}

	private static class GenericFieldProcessor extends PropertyNotFullTextFieldAnnotationProcessor<GenericField> {
		GenericFieldProcessor(AnnotationProcessorHelper helper) {
			super( helper, GenericField.class );
		}

		@Override
		PropertyMappingNonFullTextFieldOptionsStep<?> initSortableFieldMappingContext(PropertyMappingStep mappingContext,
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
		Searchable getSearchable(GenericField annotation) {
			return annotation.searchable();
		}

		@Override
		Sortable getSortable(GenericField annotation) {
			return annotation.sortable();
		}

		@Override
		Aggregable getAggregable(GenericField annotation) {
			return annotation.aggregable();
		}

		@Override
		String getIndexNullAs(GenericField annotation) {
			return annotation.indexNullAs();
		}

		@Override
		ValueBridgeRef getValueBridge(GenericField annotation) {
			return annotation.valueBridge();
		}

		@Override
		ValueBinderRef getValueBinder(GenericField annotation) {
			return annotation.valueBinder();
		}

		@Override
		ContainerExtraction getExtraction(GenericField annotation) {
			return annotation.extraction();
		}
	}

	private static class FullTextFieldProcessor extends PropertyFieldAnnotationProcessor<FullTextField> {
		FullTextFieldProcessor(AnnotationProcessorHelper helper) {
			super( helper, FullTextField.class );
		}

		@Override
		PropertyMappingFieldOptionsStep<?> initFieldMappingContext(PropertyMappingStep mappingContext,
				PojoPropertyModel<?> propertyModel, FullTextField annotation, String fieldName) {
			PropertyMappingFullTextFieldOptionsStep fieldContext = mappingContext.fullTextField( fieldName )
					.analyzer( annotation.analyzer() );

			Norms norms = annotation.norms();
			if ( !Norms.DEFAULT.equals( norms ) ) {
				fieldContext.norms( norms );
			}

			TermVector termVector = annotation.termVector();
			if ( !TermVector.DEFAULT.equals( termVector ) ) {
				fieldContext.termVector( termVector );
			}

			return fieldContext;
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
		Searchable getSearchable(FullTextField annotation) {
			return annotation.searchable();
		}

		@Override
		ValueBridgeRef getValueBridge(FullTextField annotation) {
			return annotation.valueBridge();
		}

		@Override
		ValueBinderRef getValueBinder(FullTextField annotation) {
			return annotation.valueBinder();
		}

		@Override
		ContainerExtraction getExtraction(FullTextField annotation) {
			return annotation.extraction();
		}
	}

	private static class KeywordFieldProcessor extends PropertyNotFullTextFieldAnnotationProcessor<KeywordField> {
		KeywordFieldProcessor(AnnotationProcessorHelper helper) {
			super( helper, KeywordField.class );
		}

		@Override
		PropertyMappingNonFullTextFieldOptionsStep<?> initSortableFieldMappingContext(PropertyMappingStep mappingContext,
				PojoPropertyModel<?> propertyModel, KeywordField annotation, String fieldName) {
			PropertyMappingKeywordFieldOptionsStep fieldContext = mappingContext.keywordField( fieldName );

			String normalizer = annotation.normalizer();
			if ( !normalizer.isEmpty() ) {
				fieldContext.normalizer( annotation.normalizer() );
			}

			Norms norms = annotation.norms();
			if ( !Norms.DEFAULT.equals( norms ) ) {
				fieldContext.norms( norms );
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
		Searchable getSearchable(KeywordField annotation) {
			return annotation.searchable();
		}

		@Override
		Sortable getSortable(KeywordField annotation) {
			return annotation.sortable();
		}

		@Override
		Aggregable getAggregable(KeywordField annotation) {
			return annotation.aggregable();
		}

		@Override
		String getIndexNullAs(KeywordField annotation) {
			return annotation.indexNullAs();
		}

		@Override
		ValueBridgeRef getValueBridge(KeywordField annotation) {
			return annotation.valueBridge();
		}

		@Override
		ValueBinderRef getValueBinder(KeywordField annotation) {
			return annotation.valueBinder();
		}

		@Override
		ContainerExtraction getExtraction(KeywordField annotation) {
			return annotation.extraction();
		}
	}

	private static class ScaledNumberFieldProcessor extends PropertyNotFullTextFieldAnnotationProcessor<ScaledNumberField> {
		ScaledNumberFieldProcessor(AnnotationProcessorHelper helper) {
			super( helper, ScaledNumberField.class );
		}

		@Override
		PropertyMappingNonFullTextFieldOptionsStep<?> initSortableFieldMappingContext(PropertyMappingStep mappingContext,
				PojoPropertyModel<?> propertyModel, ScaledNumberField annotation, String fieldName) {
			PropertyMappingScaledNumberFieldOptionsStep fieldContext = mappingContext.scaledNumberField( fieldName );
			int decimalScale = annotation.decimalScale();
			if ( decimalScale != AnnotationDefaultValues.DEFAULT_DECIMAL_SCALE ) {
				fieldContext.decimalScale( decimalScale );
			}
			return fieldContext;
		}

		@Override
		String getName(ScaledNumberField annotation) {
			return annotation.name();
		}

		@Override
		Projectable getProjectable(ScaledNumberField annotation) {
			return annotation.projectable();
		}

		@Override
		Searchable getSearchable(ScaledNumberField annotation) {
			return annotation.searchable();
		}

		@Override
		Sortable getSortable(ScaledNumberField annotation) {
			return annotation.sortable();
		}

		@Override
		Aggregable getAggregable(ScaledNumberField annotation) {
			return annotation.aggregable();
		}

		@Override
		String getIndexNullAs(ScaledNumberField annotation) {
			return annotation.indexNullAs();
		}

		@Override
		ValueBridgeRef getValueBridge(ScaledNumberField annotation) {
			return annotation.valueBridge();
		}

		@Override
		ValueBinderRef getValueBinder(ScaledNumberField annotation) {
			return annotation.valueBinder();
		}

		@Override
		ContainerExtraction getExtraction(ScaledNumberField annotation) {
			return annotation.extraction();
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
		void doProcess(PropertyMappingStep mappingContext,
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

			ContainerExtractorPath extractorPath = helper.getExtractorPath( annotation.extraction() );

			mappingContext.indexedEmbedded()
					.withExtractors( extractorPath )
					.prefix( cleanedUpPrefix )
					.storage( annotation.storage() )
					.maxDepth( cleanedUpMaxDepth )
					.includePaths( cleanedUpIncludePaths );
		}
	}
}
