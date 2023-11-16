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
import java.util.List;
import java.util.function.Consumer;

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
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.FieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.types.VectorFieldTypeDescriptor;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.SimpleFieldModelsByType;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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
		setupHelper.start()
				.withIndexes(
						WrongVectorIT.index,
						VectorSimilarityIT.indexDefault,
						VectorSimilarityIT.indexL2,
						VectorSimilarityIT.indexInnerProduct,
						VectorSimilarityIT.indexCosine,
						ExampleKnnSearchIT.index,
						ExampleKnnSearchIT.indexNested
				)
				.setup();

		final BulkIndexer scopeIndexer = WrongVectorIT.index.bulkIndexer();

		final BulkIndexer similarityIndexer = VectorSimilarityIT.indexDefault.bulkIndexer();
		VectorSimilarityIT.dataSets.forEach( d -> d.contribute( VectorSimilarityIT.indexDefault, similarityIndexer ) );
		final BulkIndexer similarityL2Indexer = VectorSimilarityIT.indexL2.bulkIndexer();
		VectorSimilarityIT.dataSets.forEach( d -> d.contribute( VectorSimilarityIT.indexL2, similarityL2Indexer ) );
		final BulkIndexer similarityInnerProductIndexer = VectorSimilarityIT.indexInnerProduct.bulkIndexer();
		VectorSimilarityIT.dataSets
				.forEach( d -> d.contribute( VectorSimilarityIT.indexInnerProduct, similarityInnerProductIndexer ) );
		final BulkIndexer similarityCosineIndexer = VectorSimilarityIT.indexCosine.bulkIndexer();
		VectorSimilarityIT.dataSets.forEach( d -> d.contribute( VectorSimilarityIT.indexCosine, similarityCosineIndexer ) );

		BulkIndexer exampleKnnSearchIndexer = ExampleKnnSearchIT.index.bulkIndexer();
		ExampleKnnSearchIT.dataset.accept( exampleKnnSearchIndexer );

		BulkIndexer exampleKnnSearchNestedIndexer = ExampleKnnSearchIT.indexNested.bulkIndexer();
		ExampleKnnSearchIT.datasetNested.accept( exampleKnnSearchNestedIndexer );

		scopeIndexer.join(
				similarityIndexer,
				similarityL2Indexer,
				similarityInnerProductIndexer,
				similarityCosineIndexer,
				exampleKnnSearchIndexer,
				exampleKnnSearchNestedIndexer
		);
	}

	@Nested
	class WrongVectorIT extends WrongVectorConfigured {
		// JDK 11 does not allow static fields in non-static inner class and JUnit does not allow running @Nested tests in static inner classes...
	}

	abstract static class WrongVectorConfigured {
		private static final SimpleMappedIndex<IndexBinding> index =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes ) )
						.name( "index" );


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
		@Disabled
		// TODO : vector : make sure that the vector size for matching matches the vector size of indexed vectors
		void wrongVectorLength(SimpleMappedIndex<IndexBinding> index, VectorFieldTypeDescriptor<?> fieldType) {
			String fieldPath = index.binding().field.get( fieldType ).relativeFieldName;

			assertThatThrownBy( () -> index.createScope( index ).query().select()
					.where( f -> tryPredicateWrongLength( f, fieldPath, fieldType ) )
					.fetchAll() )
					.isInstanceOf( SearchException.class )
					.hasMessageContainingAll(
							"Vector value passed to the knn predicate has incorrect dimension that does not match the dimension of the indexed vectors."
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
		private static final SimpleMappedIndex<IndexBinding> indexInnerProduct =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes, VectorSimilarity.INNER_PRODUCT ) )
						.name( "similarityInnerProduct" );
		private static final SimpleMappedIndex<IndexBinding> indexCosine =
				SimpleMappedIndex.of( root -> new IndexBinding( root, supportedFieldTypes, VectorSimilarity.COSINE ) )
						.name( "similarityCosine" );

		private static final List<DataSet<?>> dataSets = new ArrayList<>();
		private static final List<Arguments> parameters = new ArrayList<>();
		static {
			for ( VectorFieldTypeDescriptor<?> fieldType : supportedFieldTypes ) {
				parameters.add( Arguments.of( fieldType, indexDefault ) );
				parameters.add( Arguments.of( fieldType, indexL2 ) );
				parameters.add( Arguments.of( fieldType, indexInnerProduct ) );
				parameters.add( Arguments.of( fieldType, indexCosine ) );
				dataSets.add( new DataSet<>( fieldType ) );
			}
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
				return f.knn( 2 ).field( fieldPath ).matching( (byte) 1, (byte) 1, (byte) 1, (byte) 1 );
			}
			else {
				return f.knn( 2 ).field( fieldPath ).matching( 1.0f, 1.0f, 1.0f, 1.0f );
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
				for ( F value : fieldType.getNonMatchingValues() ) {
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
							.filter( f.terms().field( "parking" ).matchingAny( true ) )
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
					)
					).toQuery();

			List<float[]> result = query.fetchAll().hits();

			assertThat( result ).hasSize( 9 );
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
									f -> f.nested( "nested" )
											.add( f.knn( 1 ).field( "nested.byteVector" ).matching( bytes( 2, (byte) -120 ) ) )
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

		private static class PredicateIndexBinding {
			final IndexFieldReference<Boolean> parking;
			final IndexFieldReference<Integer> rating;
			final IndexFieldReference<float[]> location;

			PredicateIndexBinding(IndexSchemaElement root) {
				parking = root.field( "parking", f -> f.asBoolean().projectable( Projectable.YES ) ).toReference();
				rating = root.field( "rating", f -> f.asInteger().projectable( Projectable.YES ) ).toReference();
				location = root.field( "location", f -> f.asFloatVector().dimension( 2 ).projectable( Projectable.YES )
						.maxConnections( 16 ).beamWidth( 100 ).vectorSimilarity( VectorSimilarity.L2 ) ).toReference();
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
						"byteVector", f -> f.asByteVector().dimension( 2 ).projectable( Projectable.YES ) )
						.toReference();
				floatVector = nestedField
						.field( "floatVector", f -> f.asFloatVector().dimension( 2 ).projectable( Projectable.YES ) )
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
}
