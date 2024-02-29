/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import static org.apache.lucene.util.VectorUtil.cosine;
import static org.apache.lucene.util.VectorUtil.dotProduct;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchKnnVectorsFormat;
import org.hibernate.search.util.common.AssertionFailure;

import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.VectorEncoding;
import org.apache.lucene.index.VectorSimilarityFunction;

public class LuceneFloatVectorCodec extends AbstractLuceneVectorFieldCodec<float[]> {

	private final Function<float[], float[]> normalize;

	public LuceneFloatVectorCodec(VectorSimilarityFunction vectorSimilarity, int dimension, Storage storage, Indexing indexing,
			float[] indexNullAsValue, Float magnitude, HibernateSearchKnnVectorsFormat knnVectorsFormat) {
		super( vectorSimilarity, dimension, storage, indexing, indexNullAsValue, knnVectorsFormat );
		if ( magnitude != null ) {
			normalize = vector -> normalize( vector, magnitude );
		}
		else {
			normalize = this::normalizeNoop;
		}
	}

	private float[] normalizeNoop(float[] vector) {
		return vector;
	}

	private float[] normalize(float[] vector, float magnitude) {
		if ( vector == null ) {
			return null;
		}
		float k = magnitude / norm( vector, vectorSimilarity );
		if ( Math.abs( 1 - k ) < EPS ) {
			return vector;
		}

		float[] result = Arrays.copyOf( vector, vector.length );

		for ( int i = 0; i < vector.length; i++ ) {
			result[i] = result[i] * k;
		}
		return result;
	}

	private static float norm(float[] vector, VectorSimilarityFunction similarityFunction) {
		switch ( similarityFunction ) {
			case EUCLIDEAN:
				return (float) Math.sqrt( dotProduct( vector, vector ) );
			case MAXIMUM_INNER_PRODUCT:
			case DOT_PRODUCT:
				return dotProduct( vector, vector );
			case COSINE:
				return cosine( vector, vector );
			default:
				throw new AssertionFailure( "Unknown similarity function: " + similarityFunction );
		}
	}

	@Override
	public float[] decode(IndexableField field) {
		float[] result = new float[field.binaryValue().bytes.length / Float.BYTES];

		int index = 0;
		ByteBuffer buffer = ByteBuffer.wrap( field.binaryValue().bytes );
		while ( buffer.hasRemaining() ) {
			result[index++] = buffer.getFloat();
		}

		return result;
	}

	@Override
	public byte[] encode(float[] value) {
		ByteBuffer buffer = ByteBuffer.allocate( Float.BYTES * value.length );
		for ( float element : this.normalize.apply( value ) ) {
			buffer.putFloat( element );
		}
		return buffer.array();
	}

	@Override
	protected IndexableField createIndexField(String absoluteFieldPath, float[] value) {
		return new KnnFloatVectorField( absoluteFieldPath, this.normalize.apply( value ), fieldType );
	}

	@Override
	protected VectorEncoding vectorEncoding() {
		return VectorEncoding.FLOAT32;
	}

	@Override
	public Function<float[], float[]> normalizer() {
		return normalize;
	}

	@Override
	public Class<?> vectorElementsType() {
		return float.class;
	}
}
