/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Consumer;

import org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchKnnVectorsFormat;

import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.VectorEncoding;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.util.VectorUtil;

public class LuceneFloatVectorCodec extends AbstractLuceneVectorFieldCodec<float[]> {
	private static final float EPS = 1.0e-5f;

	public LuceneFloatVectorCodec(VectorSimilarityFunction vectorSimilarity, int dimension, Storage storage, Indexing indexing,
			float[] indexNullAsValue, HibernateSearchKnnVectorsFormat knnVectorsFormat) {
		super( vectorSimilarity, dimension, storage, indexing, indexNullAsValue, knnVectorsFormat, check( vectorSimilarity ) );
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
	protected byte[] toByteArray(float[] value) {
		ByteBuffer buffer = ByteBuffer.allocate( Float.BYTES * value.length );
		for ( float element : value ) {
			buffer.putFloat( element );
		}
		return buffer.array();
	}

	@Override
	protected IndexableField createIndexField(String absoluteFieldPath, float[] value) {
		return new KnnFloatVectorField( absoluteFieldPath, value, fieldType );
	}

	@Override
	protected VectorEncoding vectorEncoding() {
		return VectorEncoding.FLOAT32;
	}

	@Override
	public Class<?> vectorElementsType() {
		return float.class;
	}

	private static Consumer<float[]> check(VectorSimilarityFunction similarityFunction) {
		switch ( similarityFunction ) {
			case DOT_PRODUCT:
				return LuceneFloatVectorCodec::dotProductCheck;
			case COSINE:
				return LuceneFloatVectorCodec::cosineCheck;
			default:
				return LuceneFloatVectorCodec::noop;
		}
	}

	private static void cosineCheck(float[] vector) {
		float l1 = VectorUtil.dotProduct( vector, vector );
		if ( l1 < EPS ) {
			throw log.vectorCosineZeroMagnitudeNotAcceptable( Arrays.toString( vector ) );
		}
	}

	private static void dotProductCheck(float[] vector) {
		float l1 = VectorUtil.dotProduct( vector, vector );
		if ( Math.abs( 1 - Math.sqrt( l1 ) ) > EPS ) {
			throw log.vectorDotProductNonUnitMagnitudeNotAcceptable( Arrays.toString( vector ) );
		}
	}

	private static void noop(float[] vector) {
		// do nothing
	}
}
