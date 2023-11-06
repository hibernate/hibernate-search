/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.nio.ByteBuffer;

import org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchKnnVectorsFormat;

import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.VectorEncoding;
import org.apache.lucene.index.VectorSimilarityFunction;

public class LuceneFloatVectorCodec extends AbstractLuceneVectorFieldCodec<float[]> {
	public LuceneFloatVectorCodec(VectorSimilarityFunction vectorSimilarity, int dimension, Storage storage, Indexing indexing,
			float[] indexNullAsValue, HibernateSearchKnnVectorsFormat knnVectorsFormat) {
		super( vectorSimilarity, dimension, storage, indexing, indexNullAsValue, knnVectorsFormat );
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

}
