/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.vector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.backend.lucene.testsupport.util.DocumentAssert.containsDocument;

import java.util.List;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import org.apache.lucene.document.Document;

class LuceneVectorFieldIT {

	private static final byte[] BYTE_VECTOR_1 = new byte[] { 1, 2, 3, 4 };
	private static final byte[] BYTE_VECTOR_2 = new byte[] { 1, 1, 1, 1 };

	private static final float[] FLOAT_VECTOR_1 = new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 1.0f, 2.0f, 3.0f, 4.0f };
	private static final float[] FLOAT_VECTOR_2 = new float[] { 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f };

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new )
			.name( "index" );
	private final SimpleMappedIndex<PredicateIndexBinding> predicateIndex = SimpleMappedIndex.of( PredicateIndexBinding::new )
			.name( "predicateIndex" );

	@BeforeEach
	void setup() {
		setupHelper.start()
				.withIndexes( index, predicateIndex )
				.setup();
		initData();
	}

	@Test
	void simpleVectorSavedAndRetrieved() {
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
	void simpleVectorPredicate() {
		// took the sample data and query from this example https://opensearch.org/docs/latest/search-plugins/knn/filter-search-knn/#using-a-lucene-k-nn-filter
		// to see if we'll get the same results... and looks like we do :smile:
		int k = 3;
		SearchQuery<float[]> query = predicateIndex.createScope().query()
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

	private void initData() {
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

		predicateIndex.bulkIndexer()
				.add( "ID:1", document -> {
					document.addValue( predicateIndex.binding().location, new float[] { 5.2f, 4.4f } );
					document.addValue( predicateIndex.binding().parking, true );
					document.addValue( predicateIndex.binding().rating, 5 );
				} )
				.add( "ID:2", document -> {
					document.addValue( predicateIndex.binding().location, new float[] { 5.2f, 3.9f } );
					document.addValue( predicateIndex.binding().parking, false );
					document.addValue( predicateIndex.binding().rating, 4 );
				} )
				.add( "ID:3", document -> {
					document.addValue( predicateIndex.binding().location, new float[] { 4.9f, 3.4f } );
					document.addValue( predicateIndex.binding().parking, true );
					document.addValue( predicateIndex.binding().rating, 9 );
				} )
				.add( "ID:4", document -> {
					document.addValue( predicateIndex.binding().location, new float[] { 4.2f, 4.6f } );
					document.addValue( predicateIndex.binding().parking, false );
					document.addValue( predicateIndex.binding().rating, 6 );
				} )
				.add( "ID:5", document -> {
					document.addValue( predicateIndex.binding().location, new float[] { 3.3f, 4.5f } );
					document.addValue( predicateIndex.binding().parking, true );
					document.addValue( predicateIndex.binding().rating, 8 );
				} )
				.add( "ID:6", document -> {
					document.addValue( predicateIndex.binding().location, new float[] { 6.4f, 3.4f } );
					document.addValue( predicateIndex.binding().parking, true );
					document.addValue( predicateIndex.binding().rating, 9 );
				} )
				.add( "ID:7", document -> {
					document.addValue( predicateIndex.binding().location, new float[] { 4.2f, 6.2f } );
					document.addValue( predicateIndex.binding().parking, true );
					document.addValue( predicateIndex.binding().rating, 5 );
				} )
				.add( "ID:8", document -> {
					document.addValue( predicateIndex.binding().location, new float[] { 2.4f, 4.0f } );
					document.addValue( predicateIndex.binding().parking, true );
					document.addValue( predicateIndex.binding().rating, 8 );
				} )
				.add( "ID:9", document -> {
					document.addValue( predicateIndex.binding().location, new float[] { 1.4f, 3.2f } );
					document.addValue( predicateIndex.binding().parking, false );
					document.addValue( predicateIndex.binding().rating, 5 );
				} )
				.add( "ID:10", document -> {
					document.addValue( predicateIndex.binding().location, new float[] { 7.0f, 9.9f } );
					document.addValue( predicateIndex.binding().parking, true );
					document.addValue( predicateIndex.binding().rating, 9 );
				} )
				.add( "ID:11", document -> {
					document.addValue( predicateIndex.binding().location, new float[] { 3.0f, 2.3f } );
					document.addValue( predicateIndex.binding().parking, false );
					document.addValue( predicateIndex.binding().rating, 6 );
				} )
				.add( "ID:12", document -> {
					document.addValue( predicateIndex.binding().location, new float[] { 5.0f, 1.0f } );
					document.addValue( predicateIndex.binding().parking, true );
					document.addValue( predicateIndex.binding().rating, 3 );
				} )
				.join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> string;
		final IndexFieldReference<byte[]> byteVector;
		final IndexFieldReference<float[]> floatVector;

		IndexBinding(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().projectable( Projectable.YES ) ).toReference();
			byteVector = root
					.field( "byteVector",
							f -> f.asByteVector().dimension( 4 ).projectable( Projectable.YES ).maxConnections( 16 )
									.vectorSimilarity( VectorSimilarity.L2 ) )
					.toReference();
			floatVector = root
					.field( "floatVector",
							f -> f.asFloatVector().dimension( 8 ).projectable( Projectable.YES ).maxConnections( 48 )
									.beamWidth( 256 ).vectorSimilarity( VectorSimilarity.INNER_PRODUCT ) )
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
