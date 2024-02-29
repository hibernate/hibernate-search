/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import static org.apache.lucene.util.VectorUtil.cosine;
import static org.apache.lucene.util.VectorUtil.dotProduct;

import java.lang.invoke.MethodHandles;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchKnnVectorsFormat;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.document.KnnByteVectorField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.VectorEncoding;
import org.apache.lucene.index.VectorSimilarityFunction;

public class LuceneByteVectorCodec extends AbstractLuceneVectorFieldCodec<byte[]> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final Function<byte[], byte[]> normalizer;

	public LuceneByteVectorCodec(VectorSimilarityFunction vectorSimilarity, int dimension, Storage storage, Indexing indexing,
			byte[] indexNullAsValue, Float magnitude, HibernateSearchKnnVectorsFormat knnVectorsFormat) {
		super( vectorSimilarity, dimension, storage, indexing, indexNullAsValue, knnVectorsFormat );
		if ( magnitude != null ) {
			normalizer = vector -> normalize( vector, magnitude );
		}
		else {
			normalizer = this::normalizeNoop;
		}
	}

	private byte[] normalizeNoop(byte[] vector) {
		// do nothing
		return vector;
	}

	private byte[] normalize(byte[] vector, float magnitude) {
		float norm = norm( vector, vectorSimilarity );
		if ( Math.abs( norm - magnitude ) > EPS ) {
			throw log.byteVectorLengthExpectationFailed( vector, magnitude, norm );
		}
		return vector;
	}

	private static float norm(byte[] vector, VectorSimilarityFunction similarityFunction) {
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
	public byte[] decode(IndexableField field) {
		return field.binaryValue().bytes;
	}

	@Override
	public byte[] encode(byte[] value) {
		normalizer.apply( value );
		return value;
	}

	@Override
	protected IndexableField createIndexField(String absoluteFieldPath, byte[] value) {
		return new KnnByteVectorField( absoluteFieldPath, normalizer.apply( value ), fieldType );
	}

	@Override
	protected VectorEncoding vectorEncoding() {
		return VectorEncoding.BYTE;
	}

	@Override
	public Function<byte[], byte[]> normalizer() {
		return normalizer;
	}

	@Override
	public Class<?> vectorElementsType() {
		return byte.class;
	}
}
