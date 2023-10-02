/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.util.Arrays;

import org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchKnnVectorsFormat;

import org.apache.lucene.document.KnnByteVectorField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.VectorEncoding;
import org.apache.lucene.index.VectorSimilarityFunction;

public class LuceneByteVectorCodec extends AbstractLuceneVectorFieldCodec<byte[]> {

	public LuceneByteVectorCodec(VectorSimilarityFunction vectorSimilarity, int dimension, Storage storage, Indexing indexing,
			byte[] indexNullAsValue, HibernateSearchKnnVectorsFormat knnVectorsFormat) {
		super( vectorSimilarity, dimension, storage, indexing, indexNullAsValue, knnVectorsFormat,
				VectorSimilarityFunction.COSINE.equals( vectorSimilarity )
						? LuceneByteVectorCodec::cosineCheck
						: LuceneByteVectorCodec::noop );
	}

	@Override
	public byte[] decode(IndexableField field) {
		return field.binaryValue().bytes;
	}

	@Override
	protected byte[] toByteArray(byte[] value) {
		return value;
	}

	@Override
	protected IndexableField createIndexField(String absoluteFieldPath, byte[] value) {
		return new KnnByteVectorField( absoluteFieldPath, value, fieldType );
	}

	@Override
	protected VectorEncoding vectorEncoding() {
		return VectorEncoding.BYTE;
	}

	@Override
	public Class<?> vectorElementsType() {
		return byte.class;
	}

	private static void cosineCheck(byte[] vector) {
		// means we cannot accept zero-vectors:
		for ( byte b : vector ) {
			if ( b != 0 ) {
				return;
			}
		}
		// if we reached here means we had a vector of zeros so let's fail:
		throw log.vectorCosineZeroMagnitudeNotAcceptable( Arrays.toString( vector ) );
	}

	private static void noop(byte[] vector) {
		// do nothing
	}
}
