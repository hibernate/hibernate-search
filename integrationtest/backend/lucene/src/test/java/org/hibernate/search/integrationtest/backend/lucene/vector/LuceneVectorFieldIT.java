/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.vector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.backend.lucene.testsupport.util.DocumentAssert.containsDocument;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatQuery;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.BulkIndexer;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import org.apache.lucene.document.Document;

class LuceneVectorFieldIT {

	private static final byte[] BYTE_VECTOR_1 = new byte[] { 1, 2, 3, 4 };
	private static final byte[] BYTE_VECTOR_2 = new byte[] { 1, 1, 1, 1 };

	private static final float[] FLOAT_VECTOR_1 = new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 1.0f, 2.0f, 3.0f, 4.0f };
	private static final float[] FLOAT_VECTOR_2 = new float[] { 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f };

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@Test
	void simpleVectorSavedAndRetrieved() {
		SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new ).name( "index" );
		setupHelper.start().withIndexes( index ).setup();
		initDataSimple( index );

		SearchQuery<Document> query = index.createScope().query()
				.select(
						f -> f.extension( LuceneExtension.get() ).document()
				)
				.where( f -> f.matchAll() )
				.toQuery();

		List<Document> result = query.fetchAll().hits();
		assertThat( result )
				.hasSize( 2 )
				.satisfies( containsDocument(
						doc -> doc.hasField( "string", "keyword1" )
								.hasVectorField( "byteVector", BYTE_VECTOR_1 )
								.hasVectorField( "floatVector", FLOAT_VECTOR_1 )
								.andOnlyInternalFields()
				) )
				.satisfies( containsDocument(
						doc -> doc.hasField( "string", "keyword2" )
								.hasVectorField( "byteVector", BYTE_VECTOR_2 )
								.hasVectorField( "floatVector", FLOAT_VECTOR_2 )
								.andOnlyInternalFields()
				) );
	}

	@Test
	void simpleVectorSavedAndRetrievedViaProjection() {
		SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new ).name( "index" );
		setupHelper.start().withIndexes( index ).setup();
		initDataSimple( index );

		SearchQuery<Object[]> query = index.createScope().query()
				.select(
						f -> f.composite().from(
								f.field( "string" ),
								f.field( "byteVector" ),
								f.field( "floatVector" )
						).asArray()
				)
				.where( f -> f.matchAll() )
				.toQuery();

		List<Object[]> result = query.fetchAll().hits();
		assertThat( result )
				.hasSize( 2 )
				.containsOnly(
						new Object[] { "keyword1", BYTE_VECTOR_1, FLOAT_VECTOR_1 },
						new Object[] { "keyword2", BYTE_VECTOR_2, FLOAT_VECTOR_2 }
				);
	}

	@Test
	void simpleVectorPredicateNoFilter() {
		SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new ).name( "index" );
		setupHelper.start().withIndexes( index ).setup();
		initDataSimpleNoFilter( index );

		assertThatQuery(
				index.createScope().query()
						.select( f -> f.field( "byteVector" ) )
						.where( f -> f.knn( 3 ).field( "byteVector" )
								.matching( bytes( 4, (byte) 5 ) ) )
		).hasTotalHitCount( 3 )
				.hasHitsExactOrder(
						bytes( 4, (byte) 5 ),
						bytes( 4, (byte) 6 ),
						bytes( 4, (byte) 4 )
				);

		assertThatQuery(
				index.createScope().query()
						.select( f -> f.field( "floatVector" ) )
						.where( f -> f.knn( 3 ).field( "floatVector" )
								.matching( floats( 8, 0.051f ) ) )
		).hasTotalHitCount( 3 )
				.hasHitsExactOrder(
						floats( 8, 0.05f ),
						floats( 8, 0.06f ),
						floats( 8, 0.04f )
				);
	}

	@ParameterizedTest
	@EnumSource(VectorSimilarity.class)
	void similarity(VectorSimilarity similarity) {
		SimpleMappedIndex<SimilarityIndexBinding> index =
				SimpleMappedIndex.of( root -> new SimilarityIndexBinding( similarity, root ) ).name( "index_" + similarity );
		setupHelper.start().withIndexes( index ).setup();
		initSimilarityIndexBinding( index );

		assertThatQuery(
				index.createScope().query()
						.select( f -> f.id() )
						.where( f -> f.knn( 1 ).field( "byteVector" )
								.matching( bytes( 4, (byte) 5 ) ) )
		).hasTotalHitCount( 1 );

		assertThatQuery(
				index.createScope().query()
						.select( f -> f.id() )
						.where( f -> f.knn( 1 ).field( "floatVector" )
								.matching( floats( 8, 0.051f ) ) )
		).hasTotalHitCount( 1 );
	}

	@Test
	void simpleVectorPredicateWithFilter() {
		// took the sample data and query from this example https://opensearch.org/docs/latest/search-plugins/knn/filter-search-knn/#using-a-lucene-k-nn-filter
		// to see if we'll get the same results... and looks like we do :smile:
		SimpleMappedIndex<PredicateIndexBinding> index = SimpleMappedIndex.of( PredicateIndexBinding::new )
				.name( "predicateIndex" );
		setupHelper.start().withIndexes( index ).setup();
		initDataSimplePredicate( index );


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
		SimpleMappedIndex<PredicateIndexBinding> index = SimpleMappedIndex.of( PredicateIndexBinding::new )
				.name( "predicateIndex" );
		setupHelper.start().withIndexes( index ).setup();
		initDataSimplePredicate( index );

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

		assertThat( result )
				.hasSize( k ) // since that is how many neighbors we were asking for in the predicate
				.containsExactly(
						new float[] { 5.2f, 4.4f },
						new float[] { 4.9f, 3.4f },
						new float[] { 7.0f, 9.9f },
						new float[] { 6.4f, 3.4f },
						new float[] { 2.4f, 4.0f },
						new float[] { 3.3f, 4.5f },
						new float[] { 5.0f, 1.0f },
						new float[] { 4.2f, 6.2f },
						new float[] { 5.2f, 3.9f }
				);
	}

	@Test
	void insideOtherPredicate() {
		SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new ).name( "index" );
		setupHelper.start().withIndexes( index ).setup();
		initDataSimple( index );

		SearchQuery<String> query = index.createScope().query()
				.select(
						f -> f.field( "string", String.class )
				)
				.where( f -> f.bool()
						.must( f.knn( 5 ).field( "byteVector" ).matching( bytes( 4, (byte) 5 ) ) )
						.must( f.match().field( "string" ).matching( "keyword1" ) )
				)
				.toQuery();

		List<String> result = query.fetchAll().hits();
		assertThat( result )
				.hasSize( 1 )
				.containsOnly( "keyword1" );
	}

	@Test
	void nestedVector() {
		SimpleMappedIndex<NestedIndexBinding> index = SimpleMappedIndex.of( NestedIndexBinding::new ).name( "index" );
		setupHelper.start().withIndexes( index ).setup();
		initNestedIndex( index );

		assertThat(
				index.createScope().query()
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

	private void initDataSimple(SimpleMappedIndex<IndexBinding> index) {
		index.bulkIndexer()
				.add( "ID:1", document -> {
					document.addValue( index.binding().string, "keyword1" );
					document.addValue( index.binding().byteVector, BYTE_VECTOR_1 );
					document.addValue( index.binding().floatVector, FLOAT_VECTOR_1 );
				} )
				.add( "ID:2", document -> {
					document.addValue( index.binding().string, "keyword2" );
					document.addValue( index.binding().byteVector, BYTE_VECTOR_2 );
					document.addValue( index.binding().floatVector, FLOAT_VECTOR_2 );
				} )
				.join();
	}

	private void initNestedIndex(SimpleMappedIndex<NestedIndexBinding> index) {
		index.bulkIndexer()
				.add( "ID:1", document -> {
					DocumentElement nested = document.addObject( index.binding().nested );
					nested.addValue( index.binding().byteVector, bytes( 2, (byte) 1 ) );
					nested.addValue( index.binding().floatVector, floats( 2, 1.0f ) );

					nested = document.addObject( index.binding().nested );
					nested.addValue( index.binding().byteVector, bytes( 2, (byte) 10 ) );
					nested.addValue( index.binding().floatVector, floats( 2, 10.0f ) );

					nested = document.addObject( index.binding().nested );
					nested.addValue( index.binding().byteVector, bytes( 2, (byte) 100 ) );
					nested.addValue( index.binding().floatVector, floats( 2, 100.0f ) );

					nested = document.addObject( index.binding().nested );
					nested.addValue( index.binding().byteVector, bytes( 2, (byte) 127 ) );
					nested.addValue( index.binding().floatVector, floats( 2, 1000.0f ) );
				} )
				.add( "ID:2", document -> {
					DocumentElement nested = document.addObject( index.binding().nested );
					nested.addValue( index.binding().byteVector, bytes( 2, (byte) -120 ) );
					nested.addValue( index.binding().floatVector, floats( 2, 12345.0f ) );
				} )
				.join();
	}

	private void initDataSimpleNoFilter(SimpleMappedIndex<IndexBinding> index) {
		BulkIndexer bulkIndexer = index.bulkIndexer();

		for ( int i = 0; i < 10; i++ ) {
			int id = i;
			bulkIndexer
					.add( "ID:" + i, document -> {
						document.addValue( index.binding().string, ( "keyword" + id ) );
						document.addValue( index.binding().byteVector, bytes( 4, (byte) id ) );
						document.addValue( index.binding().floatVector, floats( 8, id / 100.0f ) );
					} );
		}

		bulkIndexer.join();
	}

	private void initSimilarityIndexBinding(SimpleMappedIndex<SimilarityIndexBinding> index) {
		BulkIndexer bulkIndexer = index.bulkIndexer();

		for ( int i = 1; i < 11; i++ ) {
			int id = i;
			bulkIndexer
					.add( "ID:" + i, document -> {
						document.addValue( index.binding().byteVector, bytes( 4, (byte) id ) );
						document.addValue( index.binding().floatVector, floats( 8, id / 100.0f ) );
					} );
		}

		bulkIndexer.join();
	}

	private byte[] bytes(int size, byte value) {
		byte[] bytes = new byte[size];
		Arrays.fill( bytes, value );
		return bytes;
	}

	private float[] floats(int size, float value) {
		float[] bytes = new float[size];
		Arrays.fill( bytes, value );
		return bytes;
	}

	private void initDataSimplePredicate(SimpleMappedIndex<PredicateIndexBinding> index) {
		index.bulkIndexer()
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
				} )
				.join();
	}

	private static class SimilarityIndexBinding {
		final IndexFieldReference<byte[]> byteVector;
		final IndexFieldReference<float[]> floatVector;

		SimilarityIndexBinding(VectorSimilarity similarity, IndexSchemaElement root) {
			byteVector = root
					.field(
							"byteVector",
							f -> f.asByteVector().dimension( 4 ).vectorSimilarity( similarity )
					)
					.toReference();
			floatVector = root
					.field(
							"floatVector",
							f -> f.asFloatVector().dimension( 8 ).vectorSimilarity( similarity )
					)
					.toReference();
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

	private static class IndexBinding {
		final IndexFieldReference<String> string;
		final IndexFieldReference<byte[]> byteVector;
		final IndexFieldReference<float[]> floatVector;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) ).toReference();
			byteVector = root
					.field(
							"byteVector",
							f -> f.asByteVector().dimension( 4 ).projectable( Projectable.YES ).maxConnections( 16 )
									.vectorSimilarity( VectorSimilarity.L2 )
					)
					.toReference();
			floatVector = root
					.field(
							"floatVector",
							f -> f.asFloatVector().dimension( 8 ).projectable( Projectable.YES ).maxConnections( 48 )
									.beamWidth( 256 ).vectorSimilarity( VectorSimilarity.L2 )
					)
					.toReference();
		}
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
}
