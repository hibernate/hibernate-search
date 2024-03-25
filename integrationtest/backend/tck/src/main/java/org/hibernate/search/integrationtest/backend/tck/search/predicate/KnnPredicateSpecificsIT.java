/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.backend.types.dsl.SearchableProjectableIndexFieldTypeOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.KnnPredicateOptionsStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.VectorFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

//CHECKSTYLE:OFF HideUtilityClassConstructor ignore the rule since it is a class with nested test classes.
// cannot make a private constructor.
class KnnPredicateSpecificsIT {
	//CHECKSTYLE:ON

	private static final List<VectorFieldTypeDescriptor<?>> supportedFieldTypes = FieldTypeDescriptor.getAllVector();

	@RegisterExtension
	public static SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@BeforeAll
	static void setup() {
		assumeTrue(
				TckConfiguration.get().getBackendFeatures().supportsVectorSearch(),
				"These tests only make sense for a backend where Vector Search is supported and implemented."
		);
		List<SimpleMappedIndex<?>> indexes = new ArrayList<>( SimilarityFilterKnnSearchConfigured.indexes.values() );
		indexes.addAll(
				Arrays.asList(
						WrongVectorConfigured.index,
						SearchScopeConfigured.index,
						SearchScopeConfigured.indexDifferentDimension,
						SearchScopeConfigured.indexDifferentEfConstruction,
						SearchScopeConfigured.indexDifferentM,
						SearchScopeConfigured.indexDifferentSimilarity,
						VectorSimilarityConfigured.indexDefault,
						ExampleKnnSearchConfigured.index,
						ExampleKnnSearchConfigured.indexNested,
						WrongVectorConfigured.index
				) );
		if ( TckConfiguration.get().getBackendFeatures().supportsSimilarity( VectorSimilarity.L2 ) ) {
			indexes.add( VectorSimilarityConfigured.indexL2 );
		}
		if ( TckConfiguration.get().getBackendFeatures().supportsSimilarity( VectorSimilarity.MAX_INNER_PRODUCT ) ) {
			indexes.add( VectorSimilarityConfigured.indexMaxInnerProduct );
		}
		if ( TckConfiguration.get().getBackendFeatures().supportsSimilarity( VectorSimilarity.DOT_PRODUCT ) ) {
			indexes.add( VectorSimilarityConfigured.indexDotProduct );
		}
		if ( TckConfiguration.get().getBackendFeatures().supportsSimilarity( VectorSimilarity.COSINE ) ) {
			indexes.add( VectorSimilarityConfigured.indexCosine );
		}

		setupHelper.start( tckBackendHelper -> tckBackendHelper.createHashBasedShardingBackendSetupStrategy( 1 ) )
				.withIndexes( indexes ).setup();

		List<BulkIndexer> bulkIndexers = new ArrayList<>();

		final BulkIndexer scopeIndexer = SearchScopeConfigured.index.bulkIndexer();
		SearchScopeConfigured.dataSets.forEach( d -> d.contribute( SearchScopeConfigured.index, scopeIndexer ) );
		bulkIndexers.add( scopeIndexer );
		final BulkIndexer scopeDifferentDimensionIndexer = SearchScopeConfigured.indexDifferentDimension.bulkIndexer();
		SearchScopeConfigured.dataSetsDimension
				.forEach( d -> d.contribute( SearchScopeConfigured.indexDifferentDimension, scopeDifferentDimensionIndexer ) );
		bulkIndexers.add( scopeDifferentDimensionIndexer );
		final BulkIndexer scopeDifferentEfConstructionIndexer =
				SearchScopeConfigured.indexDifferentEfConstruction.bulkIndexer();
		SearchScopeConfigured.dataSets
				.forEach( d -> d.contribute( SearchScopeConfigured.indexDifferentEfConstruction,
						scopeDifferentEfConstructionIndexer ) );
		bulkIndexers.add( scopeDifferentEfConstructionIndexer );
		final BulkIndexer scopeDifferentMIndexer = SearchScopeConfigured.indexDifferentM.bulkIndexer();
		SearchScopeConfigured.dataSets
				.forEach( d -> d.contribute( SearchScopeConfigured.indexDifferentM,
						scopeDifferentMIndexer ) );
		bulkIndexers.add( scopeDifferentMIndexer );
		final BulkIndexer scopeDifferentSimilarityIndexer = SearchScopeConfigured.indexDifferentSimilarity.bulkIndexer();
		SearchScopeConfigured.dataSets
				.forEach(
						d -> d.contribute( SearchScopeConfigured.indexDifferentSimilarity, scopeDifferentSimilarityIndexer ) );
		bulkIndexers.add( scopeDifferentSimilarityIndexer );

		for ( SimpleMappedIndex<VectorSimilarityConfigured.IndexBinding> index : VectorSimilarityConfigured.supportedIndexes ) {
			final BulkIndexer similarityIndexer = index.bulkIndexer();
			VectorSimilarityConfigured.dataSets
					.forEach( d -> d.contribute( index, similarityIndexer ) );
			bulkIndexers.add( similarityIndexer );
		}

		BulkIndexer exampleKnnSearchIndexer = ExampleKnnSearchConfigured.index.bulkIndexer();
		ExampleKnnSearchConfigured.dataset.accept( exampleKnnSearchIndexer );
		bulkIndexers.add( exampleKnnSearchIndexer );

		BulkIndexer exampleKnnSearchNestedIndexer = ExampleKnnSearchConfigured.indexNested.bulkIndexer();
		ExampleKnnSearchConfigured.datasetNested.accept( exampleKnnSearchNestedIndexer );
		bulkIndexers.add( exampleKnnSearchNestedIndexer );

		for ( SimpleMappedIndex<
				SimilarityFilterKnnSearchConfigured.IndexBinding> index : SimilarityFilterKnnSearchConfigured.indexes
						.values() ) {
			BulkIndexer indexer = index.bulkIndexer();
			SimilarityFilterKnnSearchConfigured.dataset.accept( index, indexer );
			bulkIndexers.add( indexer );
		}

		bulkIndexers.add( scopeDifferentDimensionIndexer );
		bulkIndexers.add( scopeDifferentEfConstructionIndexer );
		bulkIndexers.add( scopeDifferentMIndexer );
		bulkIndexers.add( scopeDifferentSimilarityIndexer );
		bulkIndexers.add( exampleKnnSearchIndexer );
		bulkIndexers.add( exampleKnnSearchNestedIndexer );

		scopeIndexer.join( bulkIndexers.stream().toArray( BulkIndexer[]::new ) );
	}

	@Nested
	class WrongVectorIT extends WrongVectorConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class WrongVectorConfigured {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "wrong" );


		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( FieldTypeDescriptor<?, ?> fieldType : supportedFieldTypes ) {
				parameters.add( Arguments.of( index, fieldType ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@ParameterizedTest(name = "{1}")
		@MethodSource("params")
		void wrongVectorValuesType(SimpleMappedIndex<IndexBinding> index, VectorFieldTypeDescriptor<?> fieldType) {
			SearchPredicateFactory f = index.createScope( index ).predicate();

			String fieldPath = index.binding().field.get( fieldType ).relativeFieldName;

			assertThatThrownBy( () -> tryPredicateWrongType( f, fieldPath, fieldType ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Vector field '" + fieldPath + "' is defined as a '"
									+ fieldType.getJavaType().getComponentType().getSimpleName() + "' array.",
							"Use the array of the same type as the vector field"
					);
		}

		@ParameterizedTest(name = "{1}")
		@MethodSource("params")
		void wrongVectorLength(SimpleMappedIndex<IndexBinding> index, VectorFieldTypeDescriptor<?> fieldType) {
			String fieldPath = index.binding().field.get( fieldType ).relativeFieldName;

			assertThatThrownBy( () -> index.createScope( index ).query().select()
					.where( f -> tryPredicateWrongLength( f, fieldPath, fieldType ) )
					.fetchAll() )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Vector field '" + fieldPath + "' is defined as a vector with '4' dimensions (array length).",
							"Matching against an array with length of '8' is unsupported",
							"Use the array of the same size as the vector field"
					);
		}

		protected void tryPredicateWrongType(SearchPredicateFactory f, String fieldPath,
				VectorFieldTypeDescriptor<?> fieldType) {
			if ( fieldType.getJavaType() == byte[].class ) {
				f.knn( 1 ).field( fieldPath ).matching( 1.0f, 1.0f );
			}
			else {
				f.knn( 1 ).field( fieldPath ).matching( (byte) 1, (byte) 1 );
			}
		}

		protected KnnPredicateOptionsStep tryPredicateWrongLength(SearchPredicateFactory f, String fieldPath,
				VectorFieldTypeDescriptor<?> fieldType) {
			if ( fieldType.getJavaType() == byte[].class ) {
				return f.knn( 1 ).field( fieldPath ).matching( new byte[fieldType.vectorSize() * 2] );
			}
			else {
				return f.knn( 1 ).field( fieldPath ).matching( new float[fieldType.vectorSize() * 2] );
			}
		}

		public static final class IndexBinding {
			private final SimpleFieldModelsByType field;

			public IndexBinding(
					IndexSchemaElement root,
					Collection<? extends FieldTypeDescriptor<?,
							? extends SearchableProjectableIndexFieldTypeOptionsStep<?, ?>>> fieldTypes) {
				field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "" );
			}
		}
	}

	@Nested
	class SearchScopeIT extends SearchScopeConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class SearchScopeConfigured {
		private static final List<VectorFieldTypeDescriptor<?>> supportedFieldTypesWithDifferentDimension =
				supportedFieldTypes.stream()
						.map( vd -> vd.withDimension( 2 ) ).collect( Collectors.toList() );

		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes, 2, 2, VectorSimilarity.L2 ) )
						.name( "scope" );

		private static final SimpleMappedIndex<IndexBinding> indexDifferentDimension =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypesWithDifferentDimension, 2, 2,
						VectorSimilarity.L2
				) ).name( "scopeDifferentDimension" );

		private static final SimpleMappedIndex<IndexBinding> indexDifferentEfConstruction =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes, 4, 2, VectorSimilarity.L2 ) )
						.name( "scopeDifferentEfConstruction" );

		private static final SimpleMappedIndex<IndexBinding> indexDifferentM =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes, 2, 4, VectorSimilarity.L2 ) )
						.name( "scopeDifferentM" );

		private static final SimpleMappedIndex<IndexBinding> indexDifferentSimilarity =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes, 2, 2, VectorSimilarity.COSINE ) )
						.name( "scopeDifferentSimilarity" );

		private static final List<DataSet<?>> dataSets = new ArrayList<>();
		private static final List<DataSet<?>> dataSetsDimension = new ArrayList<>();

		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( VectorFieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
				parameters.add( Arguments.of( fieldType, index, indexDifferentDimension, indexDifferentEfConstruction,
						indexDifferentM, indexDifferentSimilarity ) );
				dataSets.add( new DataSet<>( fieldType ) );
			}
			for ( VectorFieldTypeDescriptor<?> fieldType : supportedFieldTypesWithDifferentDimension ) {
				dataSetsDimension.add( new DataSet<>( fieldType ) );
			}
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("params")
		void dimension(VectorFieldTypeDescriptor<?> fieldType,
				SimpleMappedIndex<IndexBinding> index,
				SimpleMappedIndex<IndexBinding> indexDifferentDimension,
				SimpleMappedIndex<IndexBinding> indexDifferentEfConstruction,
				SimpleMappedIndex<IndexBinding> indexDifferentM,
				SimpleMappedIndex<IndexBinding> indexDifferentSimilarity) {
			SearchPredicateFactory f = index.createScope( indexDifferentDimension ).predicate();

			String fieldPath = index.binding().field.get( fieldType ).relativeFieldName;

			assertThatThrownBy( () -> predicate( f, fieldPath, fieldType ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"'predicate:knn'", fieldPath,
							"Inconsistent configuration for field '" + fieldPath + "'",
							"codec", "differs",
							"dimension=4",
							"dimension=2"
					);
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("params")
		void similarity(VectorFieldTypeDescriptor<?> fieldType,
				SimpleMappedIndex<IndexBinding> index,
				SimpleMappedIndex<IndexBinding> indexDifferentDimension,
				SimpleMappedIndex<IndexBinding> indexDifferentEfConstruction,
				SimpleMappedIndex<IndexBinding> indexDifferentM,
				SimpleMappedIndex<IndexBinding> indexDifferentSimilarity) {
			SearchPredicateFactory f = index.createScope( indexDifferentSimilarity ).predicate();

			String fieldPath = index.binding().field.get( fieldType ).relativeFieldName;

			assertThatThrownBy( () -> predicate( f, fieldPath, fieldType ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"'predicate:knn'", fieldPath,
							"Inconsistent configuration for field '" + fieldPath + "'",
							"codec", "differs",
							"vectorSimilarity=",
							"vectorSimilarity=COSINE"
					);
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("params")
		void efConstruction(VectorFieldTypeDescriptor<?> fieldType,
				SimpleMappedIndex<IndexBinding> index,
				SimpleMappedIndex<IndexBinding> indexDifferentDimension,
				SimpleMappedIndex<IndexBinding> indexDifferentEfConstruction,
				SimpleMappedIndex<IndexBinding> indexDifferentM,
				SimpleMappedIndex<IndexBinding> indexDifferentSimilarity) {
			StubMappingScope scope = index.createScope( indexDifferentEfConstruction );
			SearchPredicateFactory f = scope.predicate();

			String fieldPath = index.binding().field.get( fieldType ).relativeFieldName;

			assertThatThrownBy( () -> predicate( f, fieldPath, fieldType ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"'predicate:knn'", fieldPath,
							"Inconsistent configuration for field '" + fieldPath + "'",
							"codec", "differs",
							"efConstruction=2",
							"efConstruction=4"
					);
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("params")
		void m(VectorFieldTypeDescriptor<?> fieldType,
				SimpleMappedIndex<IndexBinding> index,
				SimpleMappedIndex<IndexBinding> indexDifferentDimension,
				SimpleMappedIndex<IndexBinding> indexDifferentEfConstruction,
				SimpleMappedIndex<IndexBinding> indexDifferentM,
				SimpleMappedIndex<IndexBinding> indexDifferentSimilarity) {
			StubMappingScope scope = index.createScope( indexDifferentM );
			SearchPredicateFactory f = scope.predicate();

			String fieldPath = index.binding().field.get( fieldType ).relativeFieldName;

			assertThatThrownBy( () -> predicate( f, fieldPath, fieldType ) )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"'predicate:knn'", fieldPath,
							"Inconsistent configuration for field '" + fieldPath + "'",
							"codec", "differs",
							"m=2",
							"m=4"
					);
		}

		protected KnnPredicateOptionsStep predicate(SearchPredicateFactory f, String fieldPath,
				VectorFieldTypeDescriptor<?> fieldType) {
			if ( fieldType.getJavaType() == byte[].class ) {
				return f.knn( 1 ).field( fieldPath ).matching( (byte) 1, (byte) 1 );
			}
			else {
				return f.knn( 1 ).field( fieldPath ).matching( 1.0f, 1.0f );
			}
		}

		private static class IndexBinding {
			private final SimpleFieldModelsByType field;


			public IndexBinding(IndexSchemaElement root, Collection<? extends VectorFieldTypeDescriptor<?>> fieldTypes,
					int efConstruction, int m, VectorSimilarity vectorSimilarity) {

				field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "", c -> c.efConstruction( efConstruction )
						.m( m ).vectorSimilarity( vectorSimilarity ) );
			}

			public <F> F sampleVector(VectorFieldTypeDescriptor<F> vectorFieldTypeDescriptor) {
				return vectorFieldTypeDescriptor.sampleVector();
			}
		}

		public static final class DataSet<F> extends AbstractPredicateDataSet {
			private final VectorFieldTypeDescriptor<F> fieldType;

			protected DataSet(VectorFieldTypeDescriptor<F> fieldType) {
				super( fieldType.getUniqueName() );
				this.fieldType = fieldType;
			}

			public void contribute(SimpleMappedIndex<IndexBinding> index, BulkIndexer indexer) {
				for ( int i = 0; i < 1024; i++ ) {
					F fieldValue = index.binding().sampleVector( fieldType );
					indexer.add( docId( i ), routingKey, document -> initDocument( index, document, fieldValue ) );
				}
			}

			private void initDocument(SimpleMappedIndex<IndexBinding> index, DocumentElement document, F fieldValue) {
				IndexBinding binding = index.binding();
				document.addValue( binding.field.get( fieldType ).reference, fieldValue );
			}

		}
	}

	@Nested
	class VectorSimilarityIT extends VectorSimilarityConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class VectorSimilarityConfigured {
		private static final SimpleMappedIndex<IndexBinding> indexDefault =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes, VectorSimilarity.DEFAULT ) )
						.name( "similarity" );
		private static final SimpleMappedIndex<IndexBinding> indexL2 =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes, VectorSimilarity.L2 ) )
						.name( "similarityL2" );
		private static final SimpleMappedIndex<IndexBinding> indexDotProduct =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes, VectorSimilarity.DOT_PRODUCT ) )
						.name( "similarityInnerProduct" );
		private static final SimpleMappedIndex<IndexBinding> indexCosine =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes, VectorSimilarity.COSINE ) )
						.name( "similarityCosine" );
		private static final SimpleMappedIndex<IndexBinding> indexMaxInnerProduct =
				SimpleMappedIndex
						.of( root -> new IndexBinding( root, supportedFieldTypes, VectorSimilarity.MAX_INNER_PRODUCT ) )
						.name( "similarityMaxInnerProduct" );

		private static final Set<SimpleMappedIndex<IndexBinding>> supportedIndexes;

		private static final List<DataSet<?>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			Set<SimpleMappedIndex<IndexBinding>> set = new HashSet<>();
			TckBackendFeatures backendFeatures = TckConfiguration.get().getBackendFeatures();
			for ( VectorFieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
				parameters.add( Arguments.of( fieldType, indexDefault ) );
				set.add( indexDefault );
				if ( backendFeatures.supportsSimilarity( VectorSimilarity.L2 ) ) {
					parameters.add( Arguments.of( fieldType, indexL2 ) );
					set.add( indexL2 );
				}
				if ( backendFeatures.supportsSimilarity( VectorSimilarity.DOT_PRODUCT ) ) {
					parameters.add( Arguments.of( fieldType, indexDotProduct ) );
					set.add( indexDotProduct );
				}
				if ( backendFeatures.supportsSimilarity( VectorSimilarity.COSINE ) ) {
					parameters.add( Arguments.of( fieldType, indexCosine ) );
					set.add( indexCosine );
				}
				if ( backendFeatures.supportsSimilarity( VectorSimilarity.MAX_INNER_PRODUCT ) ) {
					parameters.add( Arguments.of( fieldType, indexMaxInnerProduct ) );
					set.add( indexMaxInnerProduct );
				}
				dataSets.add( new DataSet<>( fieldType ) );
			}
			supportedIndexes = Collections.unmodifiableSet( set );
		}

		public static List<? extends Arguments> params() {
			return parameters;
		}

		@ParameterizedTest
		@MethodSource("params")
		void similarity(VectorFieldTypeDescriptor<?> fieldType,
				SimpleMappedIndex<IndexBinding> index) {
			StubMappingScope scope = index.createScope();
			SearchPredicateFactory f = scope.predicate();

			String fieldPath = index.binding().field.get( fieldType ).relativeFieldName;

			assertThat( scope.query().select()
					.where( predicate( f, fieldPath, fieldType ).toPredicate() )
					.fetchAllHits() )
					.hasSize( 2 );
		}

		protected KnnPredicateOptionsStep predicate(SearchPredicateFactory f, String fieldPath,
				VectorFieldTypeDescriptor<?> fieldType) {
			if ( fieldType.getJavaType() == byte[].class ) {
				return f.knn( 2 ).field( fieldPath ).matching( (byte) 1, (byte) 0, (byte) 0, (byte) 0 );
			}
			else {
				return f.knn( 2 ).field( fieldPath ).matching( 1.0f, 0.0f, 0.0f, 0.0f );
			}
		}

		private static class IndexBinding {
			private final SimpleFieldModelsByType field;

			public IndexBinding(IndexSchemaElement root, Collection<? extends VectorFieldTypeDescriptor<?>> fieldTypes,
					VectorSimilarity vectorSimilarity) {
				field = SimpleFieldModelsByType.mapAll( fieldTypes, root, "", c -> c.vectorSimilarity( vectorSimilarity ) );
			}
		}

		public static final class DataSet<F> extends AbstractPredicateDataSet {
			private final VectorFieldTypeDescriptor<F> fieldType;

			protected DataSet(VectorFieldTypeDescriptor<F> fieldType) {
				super( fieldType.getUniqueName() );
				this.fieldType = fieldType;
			}

			public void contribute(SimpleMappedIndex<IndexBinding> index, BulkIndexer indexer) {
				int id = 0;
				for ( F value : fieldType.unitLengthVectors() ) {
					indexer.add( docId( id++ ), routingKey, document -> initDocument( index, document, value ) );
				}
			}

			private void initDocument(SimpleMappedIndex<IndexBinding> index, DocumentElement document, F fieldValue) {
				IndexBinding binding = index.binding();
				document.addValue( binding.field.get( fieldType ).reference, fieldValue );
			}

		}
	}

	/*
	* A few tests based on the example from https://opensearch.org/docs/latest/search-plugins/knn/filter-search-knn/#using-a-lucene-k-nn-filter
	* */
	@Nested
	class ExampleKnnSearchIT extends ExampleKnnSearchConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class ExampleKnnSearchConfigured {
		private static final SimpleMappedIndex<PredicateIndexBinding> index =
				SimpleMappedIndex.of( PredicateIndexBinding::new ).name( "exampleKnnSearchPredicate" );
		private static final SimpleMappedIndex<NestedIndexBinding> indexNested =
				SimpleMappedIndex.of( NestedIndexBinding::new ).name( "exampleKnnSearchPredicateNested" );

		private static final Consumer<BulkIndexer> dataset = bulkIndexer -> bulkIndexer
				.add( "ID:1", document -> {
					document.addValue( index.binding().location, new float[] { 5.2f, 4.4f } );
					document.addValue( index.binding().parking, true );
					document.addValue( index.binding().rating, 5 );
				} )
				.add( "ID:2", document -> {
					document.addValue( index.binding().location, new float[] { 5.2f, 3.9f } );
					document.addValue( index.binding().parking, false );
					document.addValue( index.binding().rating, 4 );
				} )
				.add( "ID:3", document -> {
					document.addValue( index.binding().location, new float[] { 4.9f, 3.4f } );
					document.addValue( index.binding().parking, true );
					document.addValue( index.binding().rating, 9 );
				} )
				.add( "ID:4", document -> {
					document.addValue( index.binding().location, new float[] { 4.2f, 4.6f } );
					document.addValue( index.binding().parking, false );
					document.addValue( index.binding().rating, 6 );
				} )
				.add( "ID:5", document -> {
					document.addValue( index.binding().location, new float[] { 3.3f, 4.5f } );
					document.addValue( index.binding().parking, true );
					document.addValue( index.binding().rating, 8 );
				} )
				.add( "ID:6", document -> {
					document.addValue( index.binding().location, new float[] { 6.4f, 3.4f } );
					document.addValue( index.binding().parking, true );
					document.addValue( index.binding().rating, 9 );
				} )
				.add( "ID:7", document -> {
					document.addValue( index.binding().location, new float[] { 4.2f, 6.2f } );
					document.addValue( index.binding().parking, true );
					document.addValue( index.binding().rating, 5 );
				} )
				.add( "ID:8", document -> {
					document.addValue( index.binding().location, new float[] { 2.4f, 4.0f } );
					document.addValue( index.binding().parking, true );
					document.addValue( index.binding().rating, 8 );
				} )
				.add( "ID:9", document -> {
					document.addValue( index.binding().location, new float[] { 1.4f, 3.2f } );
					document.addValue( index.binding().parking, false );
					document.addValue( index.binding().rating, 5 );
				} )
				.add( "ID:10", document -> {
					document.addValue( index.binding().location, new float[] { 7.0f, 9.9f } );
					document.addValue( index.binding().parking, true );
					document.addValue( index.binding().rating, 9 );
				} )
				.add( "ID:11", document -> {
					document.addValue( index.binding().location, new float[] { 3.0f, 2.3f } );
					document.addValue( index.binding().parking, false );
					document.addValue( index.binding().rating, 6 );
				} )
				.add( "ID:12", document -> {
					document.addValue( index.binding().location, new float[] { 5.0f, 1.0f } );
					document.addValue( index.binding().parking, true );
					document.addValue( index.binding().rating, 3 );
				} );

		private static final Consumer<BulkIndexer> datasetNested = bulkIndexer -> bulkIndexer
				.add( "ID:1", document -> {
					DocumentElement nested = document.addObject( indexNested.binding().nested );
					nested.addValue( indexNested.binding().byteVector, bytes( 2, (byte) 1 ) );
					nested.addValue( indexNested.binding().floatVector, floats( 2, 1.0f ) );

					nested = document.addObject( indexNested.binding().nested );
					nested.addValue( indexNested.binding().byteVector, bytes( 2, (byte) 10 ) );
					nested.addValue( indexNested.binding().floatVector, floats( 2, 10.0f ) );

					nested = document.addObject( indexNested.binding().nested );
					nested.addValue( indexNested.binding().byteVector, bytes( 2, (byte) 100 ) );
					nested.addValue( indexNested.binding().floatVector, floats( 2, 100.0f ) );

					nested = document.addObject( indexNested.binding().nested );
					nested.addValue( indexNested.binding().byteVector, bytes( 2, (byte) 127 ) );
					nested.addValue( indexNested.binding().floatVector, floats( 2, 1000.0f ) );
				} )
				.add( "ID:2", document -> {
					DocumentElement nested = document.addObject( indexNested.binding().nested );
					nested.addValue( indexNested.binding().byteVector, bytes( 2, (byte) -120 ) );
					nested.addValue( indexNested.binding().floatVector, floats( 2, 12345.0f ) );
				} );

		@Test
		void simpleVectorPredicateWithFilter() {
			int k = 3;
			SearchQuery<float[]> query = index.createScope().query()
					.select(
							f -> f.field( "location", float[].class )
					)
					.where( f -> f.knn( k )
							.field( "location" )
							.matching( 5f, 4f )
							.filter( f.range().field( "rating" ).between( 8, 10 ) )
							.filter( fa -> fa.terms().field( "parking" ).matchingAny( true ) )
					).toQuery();

			List<float[]> result = query.fetchAll().hits();

			assertThat( result )
					.hasSize( k ) // since that is how many neighbors we were asking for in the predicate
					.containsExactly(
							new float[] { 4.9f, 3.4f },
							new float[] { 6.4f, 3.4f },
							new float[] { 3.3f, 4.5f }
					);
		}

		@Test
		void knnPredicateInsideOrYieldsMoreResults() {
			int k = 3;
			SearchQuery<float[]> query = index.createScope().query()
					.select(
							f -> f.field( "location", float[].class )
					)
					.where( f -> f.or(
							f.knn( k )
									.field( "location" )
									.matching( 5f, 4f ),
							f.terms().field( "parking" ).matchingAny( true )
					) ).toQuery();

			List<float[]> result = query.fetchAll().hits();

			assertThat( result ).hasSize( 9 );
		}

		@Test
		void knnAsKnnFilter() {
			SearchQuery<Object> query = index.createScope().query()
					.select(
							SearchProjectionFactory::id
					)
					.where( f -> f.knn( 3 )
							.field( "location" )
							.matching( 5f, 4f )
							.filter(
									f.knn( 6 )
											.field( "location" )
											.matching( 6f, 3f )
							)
					).toQuery();

			List<Object> result = query.fetchAll().hits();

			assertThat( result ).hasSize( 3 )
					.containsOnly( "ID:2", "ID:1", "ID:3" );
		}

		@Test
		void insideOtherPredicate() {
			SearchQuery<float[]> query = index.createScope().query()
					.select(
							f -> f.field( "location", float[].class )
					)
					.where( f -> f.bool()
							.must( f.knn( 5 ).field( "location" ).matching( 5f, 4f ) )
							.must( f.match().field( "parking" ).matching( true ) )
					)
					.toQuery();

			List<float[]> result = query.fetchAll().hits();
			assertThat( result )
					.hasSize( 3 );
		}

		@Test
		@SuppressWarnings("unchecked")
		void nestedVector() {
			assertThat(
					indexNested.createScope().query()
							.select( f -> f.composite()
									.from(
											f.id(),
											f.object( "nested" )
													.from(
															f.field( "nested.byteVector" ),
															f.field( "nested.floatVector" )
													)
													.asList()
													.multi()
									).asList()
							).where(
									f -> f.knn( 1 ).field( "nested.byteVector" ).matching( bytes( 2, (byte) -120 ) )
							).fetchAllHits()
			).hasSize( 1 )
					.element( 0 )
					.satisfies( el -> {
						assertThat( el ).hasSize( 2 );
						assertThat( el ).element( 0 ).isEqualTo( "ID:2" );
						assertThat( el ).element( 1 ).satisfies( inner -> {
							List<Object> vectors = (List<Object>) ( (List<Object>) inner ).get( 0 );
							assertThat( vectors ).element( 0 ).isEqualTo( bytes( 2, (byte) -120 ) );
							assertThat( vectors ).element( 1 ).isEqualTo( floats( 2, 12345.0f ) );
						} );
					} );
		}

		@Test
		void multivaluedFieldsNotAllowed() {
			SimpleMappedIndex<MultiValuedIndexBinding> index =
					SimpleMappedIndex.of( MultiValuedIndexBinding::new ).name( "index" );
			assertThatThrownBy( () -> setupHelper.start().withIndexes( index ).setup() )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining(
							"field 'vector'",
							"Fields of this type cannot be multivalued"
					);
		}

		@Test
		void multipleKnn_shouldClauses() {
			SearchQuery<Object> query = index.createScope().query()
					.select(
							SearchProjectionFactory::id
					)
					.where( f -> f.bool()
							.should( f.knn( 3 ).field( "location" ).matching( 5.2f, 4.4f ) )
							.should( f.knn( 3 ).field( "location" ).matching( 7.0f, 9.9f ) )
							// so that we can get to a step where we add another knn clause to already an array of knn clauses:
							.should( f.knn( 3 ).field( "location" ).matching( 1.4f, 3.2f ) )
					).toQuery();

			List<Object> result = query.fetchAll().hits();

			assertThat( result ).hasSizeGreaterThanOrEqualTo( 8 )
					.contains( "ID:1", "ID:10", "ID:9", "ID:2", "ID:4", "ID:8", "ID:11", "ID:7" );
		}

		private static class PredicateIndexBinding {
			final IndexFieldReference<Boolean> parking;
			final IndexFieldReference<Integer> rating;
			final IndexFieldReference<float[]> location;

			PredicateIndexBinding(IndexSchemaElement root) {
				parking = root.field( "parking", f -> f.asBoolean().projectable( Projectable.YES ) ).toReference();
				rating = root.field( "rating", f -> f.asInteger().projectable( Projectable.YES ) ).toReference();
				location = root.field( "location", f -> f.asFloatVector().dimension( 2 ).projectable( Projectable.YES )
						.m( 16 ).efConstruction( 100 ).vectorSimilarity( VectorSimilarity.L2 ) ).toReference();
			}
		}

		private static class MultiValuedIndexBinding {
			final IndexFieldReference<byte[]> vector;

			private MultiValuedIndexBinding(IndexSchemaElement root) {
				vector = root.field( "vector", f -> f.asByteVector().dimension( 2 ) ).multiValued().toReference();
			}
		}

		private static class NestedIndexBinding {
			final IndexObjectFieldReference nested;
			final IndexFieldReference<byte[]> byteVector;
			final IndexFieldReference<float[]> floatVector;

			NestedIndexBinding(IndexSchemaElement root) {
				IndexSchemaObjectField nestedField = root.objectField( "nested", ObjectStructure.NESTED )
						.multiValued();
				nested = nestedField.toReference();

				byteVector = nestedField.field(
						"byteVector",
						f -> f.asByteVector().dimension( 2 ).vectorSimilarity( VectorSimilarity.L2 )
								.projectable( Projectable.YES ) )
						.toReference();
				floatVector = nestedField
						.field( "floatVector",
								f -> f.asFloatVector().dimension( 2 ).vectorSimilarity( VectorSimilarity.L2 )
										.projectable( Projectable.YES ) )
						.toReference();
			}
		}

		private static byte[] bytes(int size, byte value) {
			byte[] bytes = new byte[size];
			Arrays.fill( bytes, value );
			return bytes;
		}

		private static float[] floats(int size, float value) {
			float[] bytes = new float[size];
			Arrays.fill( bytes, value );
			return bytes;
		}
	}


	@Nested
	class SimilarityFilterKnnSearchIT extends SimilarityFilterKnnSearchConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class SimilarityFilterKnnSearchConfigured {

		private static final Map<VectorSimilarity, SimpleMappedIndex<IndexBinding>> indexes;

		static {
			HashMap<VectorSimilarity, SimpleMappedIndex<IndexBinding>> map = new HashMap<>();
			for ( VectorSimilarity similarity : VectorSimilarity.values() ) {
				if ( VectorSimilarity.DEFAULT.equals( similarity ) ) {
					continue;
				}
				if ( TckConfiguration.get().getBackendFeatures().supportsSimilarity( similarity ) ) {
					map.put( similarity, SimpleMappedIndex.of( r -> new IndexBinding( r, similarity ) )
							.name( "similarityFilterKnnSearch" + similarity.name() ) );
				}
			}

			indexes = Collections.unmodifiableMap( map );
		}

		private static final BiConsumer<SimpleMappedIndex<IndexBinding>, BulkIndexer> dataset =
				(index, bulkIndexer) -> bulkIndexer
						.add( "ID:1", document -> {
							document.addValue( index.binding().location, noramlize( 5.2f, 4.4f ) );
							document.addValue( index.binding().bytes, noramlize( index.binding().similarity, 5, 4 ) );
							document.addValue( index.binding().rating, 5 );
						} )
						.add( "ID:2", document -> {
							document.addValue( index.binding().location, noramlize( 5.2f, 3.9f ) );
							document.addValue( index.binding().bytes, noramlize( index.binding().similarity, 5, 3 ) );
							document.addValue( index.binding().rating, 4 );
						} )
						.add( "ID:3", document -> {
							document.addValue( index.binding().location, noramlize( 4.9f, 3.4f ) );
							document.addValue( index.binding().bytes, noramlize( index.binding().similarity, 4, 3 ) );
							document.addValue( index.binding().rating, 9 );
						} )
						.add( "ID:4", document -> {
							document.addValue( index.binding().location, noramlize( 4.2f, 4.6f ) );
							document.addValue( index.binding().bytes, noramlize( index.binding().similarity, 4, 4 ) );
							document.addValue( index.binding().rating, 6 );
						} )
						.add( "ID:5", document -> {
							document.addValue( index.binding().location, noramlize( 3.3f, 4.5f ) );
							document.addValue( index.binding().bytes, noramlize( index.binding().similarity, 3, 4 ) );
							document.addValue( index.binding().rating, 8 );
						} )
						.add( "ID:6", document -> {
							document.addValue( index.binding().location, noramlize( 6.4f, 3.4f ) );
							document.addValue( index.binding().bytes, noramlize( index.binding().similarity, 6, 3 ) );
							document.addValue( index.binding().rating, 9 );
						} )
						.add( "ID:7", document -> {
							document.addValue( index.binding().location, noramlize( 4.2f, 6.2f ) );
							document.addValue( index.binding().bytes, noramlize( index.binding().similarity, 4, 6 ) );
							document.addValue( index.binding().rating, 5 );
						} )
						.add( "ID:8", document -> {
							document.addValue( index.binding().location, noramlize( 2.4f, 4.0f ) );
							document.addValue( index.binding().bytes, noramlize( index.binding().similarity, 2, 4 ) );
							document.addValue( index.binding().rating, 8 );
						} )
						.add( "ID:9", document -> {
							document.addValue( index.binding().location, noramlize( 1.4f, 3.2f ) );
							document.addValue( index.binding().bytes, noramlize( index.binding().similarity, 1, 3 ) );
							document.addValue( index.binding().rating, 5 );
						} )
						.add( "ID:10", document -> {
							document.addValue( index.binding().location, noramlize( 7.0f, 9.9f ) );
							document.addValue( index.binding().bytes, noramlize( index.binding().similarity, 7, 9 ) );
							document.addValue( index.binding().rating, 9 );
						} )
						.add( "ID:11", document -> {
							document.addValue( index.binding().location, noramlize( 3.0f, 2.3f ) );
							document.addValue( index.binding().bytes, noramlize( index.binding().similarity, 3, 2 ) );
							document.addValue( index.binding().rating, 6 );
						} )
						.add( "ID:12", document -> {
							document.addValue( index.binding().location, noramlize( 5.0f, 1.0f ) );
							document.addValue( index.binding().bytes, noramlize( index.binding().similarity, 5, 1 ) );
							document.addValue( index.binding().rating, 3 );
						} );

		private static float[] noramlize(float... vector) {
			float sum = 0.0f;
			for ( int i = 0; i < vector.length; ++i ) {
				sum += vector[i] * vector[i];
			}
			sum = (float) Math.sqrt( sum );
			for ( int i = 0; i < vector.length; i++ ) {
				vector[i] /= sum;
			}
			return vector;
		}

		private static byte[] noramlize(VectorSimilarity similarity, int... vector) {
			byte[] result = new byte[vector.length];
			for ( int i = 0; i < vector.length; i++ ) {
				result[i] = (byte) ( vector[i] );
			}
			return result;
		}

		@BeforeAll
		static void beforeAll() {
			assumeTrue(
					TckConfiguration.get().getBackendFeatures().supportsVectorSearchRequiredMinimumSimilarity(),
					"This test only make sense for backends that have a way to specify the required minimum similarity."
			);
		}

		@ParameterizedTest
		@MethodSource
		void similarityFilterFloat(SimpleMappedIndex<IndexBinding> index, float similarity, float score, int matches) {
			assertThat( index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.knn( 10 )
							.field( "location" )
							.matching( noramlize( 5f, 4f ) )
							.requiredMinimumSimilarity( similarity )
					).fetchAll().hits() )
					.hasSize( matches )
					.allSatisfy( s -> assertThat( s ).isGreaterThanOrEqualTo( score ) );
		}

		/*
		* index, similarity, score, number of hits
		*/
		public static List<? extends Arguments> similarityFilterFloat() {
			List<Arguments> args = new ArrayList<>();
			SimpleMappedIndex<IndexBinding> index = indexes.get( VectorSimilarity.L2 );

			// L2 scores for vector [5f, 4f]
			// [0.9995734, 0.9992435, 0.9990251, 0.99538076, 0.9762654, 0.9665131, 0.9355144, 0.92745376, 0.91767627, 0.8887709]
			// score is 1/(1+d*d) and we are passing d here: d = sqrt( 1/s - 1 )
			args.add( Arguments.of( index, 0.06812251021f, 0.5f, 4 ) );
			args.add( Arguments.of( index, 0.15592186099f, 0.25f, 5 ) );
			args.add( Arguments.of( index, 0.26254644016f, 0.14f, 7 ) );

			index = indexes.get( VectorSimilarity.COSINE );

			// COSINE scores for vector [5f, 4f]
			// [0.9998933, 0.9998107, 0.99975604, 0.9988398, 0.9939221, 0.9913382, 0.98276734, 0.9804448, 0.9775728, 0.9687126]
			// score is ( 1.0f + d ) / 2.0f and we are passing d here: d = 2s-1
			args.add( Arguments.of( index, 0.9976796f, 0.9988397f, 4 ) );
			args.add( Arguments.of( index, 0.9996214f, 0.9998106f, 2 ) );
			args.add( Arguments.of( index, 0.960889f, 0.9804447f, 8 ) );

			index = indexes.get( VectorSimilarity.DOT_PRODUCT );

			// DOT_PRODUCT scores for vector [5f, 4f]
			// [0.9998933, 0.9998107, 0.99975604, 0.99883986, 0.9939221, 0.9913382, 0.9827673, 0.9804447, 0.9775728, 0.9687126]
			// score is ( 1.0f + d ) / 2.0f and we are passing d here: d = 2s-1
			args.add( Arguments.of( index, 0.99767972f, 0.99883985f, 4 ) );
			args.add( Arguments.of( index, 0.99951208f, 0.99975603f, 3 ) );
			args.add( Arguments.of( index, 0.9608894f, 0.9804446f, 8 ) );

			index = indexes.get( VectorSimilarity.MAX_INNER_PRODUCT );

			// MAX_INNER_PRODUCT scores for vector [5f, 4f]
			// [0.9998933, 0.9998107, 0.99975604, 0.99883986, 0.9939221, 0.9913382, 0.9827673, 0.9804447, 0.9775728, 0.9687126]
			// score is ( 1.0f + d ) / 2.0f and we are passing d here: d = 2s-1
			args.add( Arguments.of( index, 0.99767972f, 0.99883985f, 4 ) );
			args.add( Arguments.of( index, 0.99951208f, 0.99975603f, 3 ) );
			args.add( Arguments.of( index, 0.9608894f, 0.9804446f, 8 ) );

			return args;
		}

		@ParameterizedTest
		@MethodSource
		void similarityFilterFloatWithBoost(SimpleMappedIndex<IndexBinding> index, float similarity, float score, int matches) {
			assertThat( index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.knn(
							10 )
							.field( "location" )
							.matching( noramlize( 5f, 4f ) )
							.requiredMinimumSimilarity( similarity )
							.boost( 100.0f )
					).fetchAll().hits() )
					.hasSize( matches )
					.allSatisfy( s -> assertThat( s ).isGreaterThanOrEqualTo( score ) );
		}

		/*
		 * index, similarity, score, number of hits
		 */
		public static List<? extends Arguments> similarityFilterFloatWithBoost() {
			List<Arguments> args = new ArrayList<>();
			SimpleMappedIndex<IndexBinding> index = indexes.get( VectorSimilarity.L2 );

			// L2 scores for vector [5f, 4f]
			// [0.9995734, 0.9992435, 0.9990251, 0.99538076, 0.9762654, 0.9665131, 0.9355144, 0.92745376, 0.91767627, 0.8887709]
			// score is 1/(1+d*d) and we are passing d here: d = sqrt( 1/s - 1 )
			args.add( Arguments.of( index, 0.06812251021f, 50.f, 4 ) );
			args.add( Arguments.of( index, 0.15592186099f, 25.f, 5 ) );
			args.add( Arguments.of( index, 0.26254644016f, 14.f, 7 ) );

			index = indexes.get( VectorSimilarity.COSINE );

			// COSINE scores for vector [5f, 4f]
			// [0.9998933, 0.9998107, 0.99975604, 0.9988398, 0.9939221, 0.9913382, 0.98276734, 0.9804448, 0.9775728, 0.9687126]
			// score is ( 1.0f + d ) / 2.0f and we are passing d here: d = 2s-1
			args.add( Arguments.of( index, 0.9976796f, 99.88397f, 4 ) );
			args.add( Arguments.of( index, 0.9996214f, 99.98106f, 2 ) );
			args.add( Arguments.of( index, 0.960889f, 98.04447f, 8 ) );

			index = indexes.get( VectorSimilarity.DOT_PRODUCT );

			// DOT_PRODUCT scores for vector [5f, 4f]
			// [0.9998933, 0.9998107, 0.99975604, 0.99883986, 0.9939221, 0.9913382, 0.9827673, 0.9804447, 0.9775728, 0.9687126]
			// score is ( 1.0f + d ) / 2.0f and we are passing d here: d = 2s-1
			args.add( Arguments.of( index, 0.99767972f, 99.883985f, 4 ) );
			args.add( Arguments.of( index, 0.99951208f, 99.975603f, 3 ) );
			args.add( Arguments.of( index, 0.9608894f, 98.04446f, 8 ) );

			index = indexes.get( VectorSimilarity.MAX_INNER_PRODUCT );

			// MAX_INNER_PRODUCT scores for vector [5f, 4f]
			// [0.9998933, 0.9998107, 0.99975604, 0.99883986, 0.9939221, 0.9913382, 0.9827673, 0.9804447, 0.9775728, 0.9687126]
			// score is ( 1.0f + d ) / 2.0f and we are passing d here: d = 2s-1
			args.add( Arguments.of( index, 0.99767972f, 99.883985f, 4 ) );
			args.add( Arguments.of( index, 0.99951208f, 99.975603f, 3 ) );
			args.add( Arguments.of( index, 0.9608894f, 98.04446f, 8 ) );

			return args;
		}

		@ParameterizedTest
		@MethodSource
		void similarityFilterByte(SimpleMappedIndex<IndexBinding> index, float similarity, float score, int matches) {
			assertThat( index.createScope().query()
					.select( SearchProjectionFactory::score )
					.where( f -> f.knn( 10 )
							.field( "bytes" )
							.matching( noramlize( index.binding().similarity, 5, 4 ) )
							.requiredMinimumSimilarity( similarity )
					).fetchAll().hits() )
					.hasSize( matches )
					.allSatisfy( s -> assertThat( s ).isGreaterThanOrEqualTo( score ) );
		}

		/*
		 * index, similarity, score, number of hits
		 */
		public static List<? extends Arguments> similarityFilterByte() {
			List<Arguments> args = new ArrayList<>();
			SimpleMappedIndex<IndexBinding> index = indexes.get( VectorSimilarity.L2 );

			// L2 scores for vector [5f, 4f]
			// [1.0, 0.5, 0.5, 0.33333334, 0.33333334, 0.2, 0.16666667, 0.11111111, 0.1, 0.1]
			// score is 1/(1+d*d) and we are passing d here: d = sqrt( 1/s - 1 )
			args.add( Arguments.of( index, 1.0f, 0.5f, 3 ) );
			args.add( Arguments.of( index, 2.0f, 0.2f, 6 ) );
			args.add( Arguments.of( index, 2.23606795067f, 0.16666667f, 7 ) );

			index = indexes.get( VectorSimilarity.COSINE );

			// COSINE scores for vector [5f, 4f]
			// [1.0, 0.99975604, 0.9981203, 0.99694186, 0.9954962, 0.9889012, 0.98625576, 0.98413867, 0.9764629, 0.95397973]
			// score is ( 1.0f + d ) / 2.0f and we are passing d here: d = 2s-1
			args.add( Arguments.of( index, 0.99388372f, 0.99694186f, 4 ) );
			args.add( Arguments.of( index, 0.99951208f, 0.99975604f, 2 ) );
			args.add( Arguments.of( index, 0.96827734f, 0.98413867f, 8 ) );

			index = indexes.get( VectorSimilarity.DOT_PRODUCT );

			// DOT_PRODUCT scores for vector [5f, 4f]
			// [0.5010834, 0.5006714, 0.50064087, 0.5006256, 0.5005646, 0.5005493, 0.5004883, 0.500473, 0.5004425, 0.5003967]
			// score is 0.5f + d / (float) ( 2 * ( 1 << 15 ) ) and we are passing d here: d = (s - 0.5) * 65536.0
			args.add( Arguments.of( index, 40.9993216f, 0.5006256f, 4 ) );
			args.add( Arguments.of( index, 42.00005632f, 0.50064087f, 3 ) );
			args.add( Arguments.of( index, 30.998528f, 0.500473f, 8 ) );

			index = indexes.get( VectorSimilarity.MAX_INNER_PRODUCT );

			// MAX_INNER_PRODUCT scores for vector [5f, 4f]
			// [0.5010834, 0.5006714, 0.50064087, 0.5006256, 0.5005646, 0.5005493, 0.5004883, 0.500473, 0.5004425, 0.5003967]
			// score is 0.5f + d / (float) ( 2 * ( 1 << 15 ) ) and we are passing d here: d = (s - 0.5) * 65536.0
			args.add( Arguments.of( index, 40.9993216f, 0.5006256f, 4 ) );
			args.add( Arguments.of( index, 42.00005632f, 0.50064087f, 2 ) );
			args.add( Arguments.of( index, 30.998528f, 0.500473f, 8 ) );

			return args;
		}

		private static class IndexBinding {

			private final VectorSimilarity similarity;
			final IndexFieldReference<Integer> rating;
			final IndexFieldReference<float[]> location;

			final IndexFieldReference<byte[]> bytes;

			IndexBinding(IndexSchemaElement root, VectorSimilarity similarity) {
				this.similarity = similarity;
				rating = root.field( "rating", f -> f.asInteger().projectable( Projectable.YES ) ).toReference();
				location =
						root.field( "location", f -> f.asVector( float[].class ).dimension( 2 ).vectorSimilarity( similarity ) )
								.toReference();
				bytes = root.field( "bytes", f -> f.asVector( byte[].class ).dimension( 2 ).vectorSimilarity( similarity ) )
						.toReference();
			}
		}
	}
}
