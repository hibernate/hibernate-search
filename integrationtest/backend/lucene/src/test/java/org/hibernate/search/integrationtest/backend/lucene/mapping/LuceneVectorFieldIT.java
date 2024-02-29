/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.lucene.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.VectorSimilarity;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.apache.lucene.util.VectorUtil;
import org.assertj.core.data.Percentage;

class LuceneVectorFieldIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@ParameterizedTest
	@ValueSource(ints = { -1, -1000, 4097, 10000, Integer.MAX_VALUE, Integer.MIN_VALUE })
	void assertDimension(int dimension) {
		test( dimension, 5, 10, "dimension", dimension, 4096 );
	}

	@ParameterizedTest
	@ValueSource(ints = { -1, -1000, 3201, 10000, Integer.MAX_VALUE, Integer.MIN_VALUE })
	void assertEfConstruction(int efConstruction) {
		test( 2, efConstruction, 10, "efConstruction", efConstruction, 3200 );
	}

	@ParameterizedTest
	@ValueSource(ints = { -1, -1000, 513, 10000, Integer.MAX_VALUE, Integer.MIN_VALUE })
	void assertM(int m) {
		test( 2, 2, m, "m", m, 512 );
	}

	void test(int dimension, int efConstruction, int m, String property, int value, int maxValue) {
		assertThatThrownBy( () -> setupHelper.start()
				.withIndex( SimpleMappedIndex
						.of( root -> root
								.field( "vector",
										f -> f.asByteVector().dimension( dimension ).efConstruction( efConstruction )
												.m( m ) )
								.toReference() ) )
				.setup()
		).isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Vector '" + property + "' cannot be equal to '" + value + "'",
						"It must be a positive integer value lesser than or equal to " + maxValue
				);
	}

	@ParameterizedTest
	@MethodSource("byteVectors")
	void byteVectors(VectorSimilarity similarity, float magnitude, byte[] vector, boolean pass) {
		SimpleMappedIndex<IndexFieldReference<byte[]>> index =
				SimpleMappedIndex.of( root -> root
						.field(
								"vector",
								f -> f.asByteVector().dimension( vector != null ? vector.length : 2 )
										.vectorSimilarity( similarity )
										.magnitude( magnitude )
										.searchable( Searchable.YES )
						)
						.toReference() );

		setupHelper.start().withIndex( index ).setup();

		if ( pass ) {
			index.bulkIndexer()
					.add( "ID:1", document -> {
						document.addValue( "vector", vector );
					} )
					.join();

			assertThat(
					index.createScope().query().select()
							.where( f -> f.knn( 1 ).field( "vector" ).matching( vector ) )
							.fetchAllHits()
			).hasSize( 1 );

			byte[] badVector = Arrays.copyOf( vector, vector.length );
			badVector[0] += 50;

			assertThatThrownBy( () -> index.createScope().query().select()
					.where( f -> f.knn( 1 ).field( "vector" ).matching( badVector ) )
					.fetchAllHits()
			).isInstanceOf( SearchException.class )
					.hasMessageContaining( "vector to have the norm value equal to" );
		}
		else {
			assertThatThrownBy( () -> index.bulkIndexer()
					.add( "ID:1", document -> {
						document.addValue( "vector", vector );
					} )
					.join() )
					.isInstanceOf( SearchException.class )
					.hasMessageContaining( "vector to have the norm value equal to" );
		}
	}

	private static List<? extends Arguments> byteVectors() {
		return List.of(
				Arguments.of( VectorSimilarity.L2, 5, new byte[] { 5, 0, 0, 0, 0 }, true ),
				Arguments.of( VectorSimilarity.L2, 5, new byte[] { 0, 5, 0, 0, 0 }, true ),
				Arguments.of( VectorSimilarity.L2, 4, new byte[] { 2, 2, 2, 2, 0 }, true ),
				Arguments.of( VectorSimilarity.L2, 5, new byte[] { 25, 0, 0, 0, 0 }, false ),
				Arguments.of( VectorSimilarity.DOT_PRODUCT, 16, new byte[] { 2, 2, 2, 2, 0 }, true ),
				Arguments.of( VectorSimilarity.DOT_PRODUCT, 25, new byte[] { 5, 0, 0, 0, 0 }, true ),
				Arguments.of( VectorSimilarity.DOT_PRODUCT, 155, new byte[] { 5, 0, 0, 0, 0 }, false )
		);
	}

	@ParameterizedTest
	@MethodSource("floatVectors")
	void floatVectors(VectorSimilarity similarity, float magnitude, float[] vector, float[] foundVector) {
		SimpleMappedIndex<IndexFieldReference<float[]>> index =
				SimpleMappedIndex.of( root -> root
						.field(
								"vector",
								f -> f.asFloatVector().dimension( vector != null ? vector.length : 2 )
										.vectorSimilarity( similarity )
										.magnitude( magnitude )
										.searchable( Searchable.YES )
										.projectable( Projectable.YES )
						)
						.toReference() );

		setupHelper.start().withIndex( index ).setup();

		index.bulkIndexer()
				.add( "ID:1", document -> {
					document.addValue( "vector", vector );
				} )
				.join();

		assertThat(
				index.createScope().query().select( f -> f.field( "vector", float[].class ) )
						.where( f -> f.knn( 1 ).field( "vector" ).matching( vector ) )
						.fetchAllHits()
		).hasSize( 1 )
				.contains( foundVector );

		float[] badVector = Arrays.copyOf( vector, vector.length );
		badVector[0] += 50;

		assertThat(
				index.createScope().query().select( f -> f.field( "vector", float[].class ) )
						.where( f -> f.knn( 1 ).field( "vector" ).matching( badVector ) )
						.fetchAllHits()
		).hasSize( 1 )
				.contains( foundVector );
	}

	private static List<? extends Arguments> floatVectors() {
		return List.of(
				Arguments.of( VectorSimilarity.L2, 5.0f, new float[] { 5.0f, 0, 0, 0, 0 }, new float[] { 5.0f, 0, 0, 0, 0 } ),
				Arguments.of( VectorSimilarity.L2, 5.0f, new float[] { 0, 5.0f, 0, 0, 0 }, new float[] { 0, 5.0f, 0, 0, 0 } ),
				Arguments.of( VectorSimilarity.L2, 4.0f, new float[] { 2.0f, 2.0f, 2.0f, 2.0f, 0 },
						new float[] { 2.0f, 2.0f, 2.0f, 2.0f, 0 } ),
				Arguments.of( VectorSimilarity.L2, 5.0f, new float[] { 25.0f, 0, 0, 0, 0 }, new float[] { 5.0f, 0, 0, 0, 0 } ),
				Arguments.of( VectorSimilarity.DOT_PRODUCT, 16.0f, new float[] { 2.0f, 2.0f, 2.0f, 2.0f, 0 },
						new float[] { 2.0f, 2.0f, 2.0f, 2.0f, 0 } ),
				Arguments.of( VectorSimilarity.DOT_PRODUCT, 25.0f, new float[] { 5.0f, 0, 0, 0, 0 },
						new float[] { 5.0f, 0, 0, 0, 0 } ),
				Arguments.of( VectorSimilarity.DOT_PRODUCT, 155.0f, new float[] { 5.0f, 0, 0, 0, 0 },
						new float[] { 31.0f, 0, 0, 0, 0 } )
		);
	}

	@Test
	void notSureThatItWouldBeOkToNormalizeByteVectorsSeeThisExampleWhy() {
		byte[] vector = { 5, 1, 1, 1, 1 };
		int dotProduct = VectorUtil.dotProduct( vector, vector );
		double l2norm = Math.sqrt( dotProduct );

		float[] vector2 = new float[vector.length];

		float magnitude = 120.0f;

		for ( int i = 0; i < vector2.length; i++ ) {
			vector2[i] = (float) ( vector[i] * magnitude / l2norm );
		}

		double dotProduct2 = VectorUtil.dotProduct( vector2, vector2 );
		double l2norm2 = Math.sqrt( dotProduct2 );
		assertThat( l2norm2 ).isCloseTo( magnitude, Percentage.withPercentage( 0.0001 ) );
		assertThat( vector2 ).contains(
				111.417206f,
				22.28344f,
				22.28344f,
				22.28344f,
				22.28344f
		);

		byte[] vector3 = new byte[vector.length];

		for ( int i = 0; i < vector2.length; i++ ) {
			vector3[i] = (byte) ( vector[i] * magnitude / l2norm );
		}

		double dotProduct3 = VectorUtil.dotProduct( vector3, vector3 );
		double l2norm3 = Math.sqrt( dotProduct3 );
		// l2norm3 == 119.4026800369238
		assertThat( l2norm3 ).isCloseTo( magnitude, Percentage.withPercentage( 1 ) );
		assertThat( vector3 ).contains(
				111,
				22,
				22,
				22,
				22
		);
	}
}
