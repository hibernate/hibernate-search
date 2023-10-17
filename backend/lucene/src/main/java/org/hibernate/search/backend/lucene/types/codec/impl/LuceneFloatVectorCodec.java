/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.util.Arrays;

import org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchKnnVectorsFormat;
import org.hibernate.search.engine.backend.types.VectorSimilarity;

import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.VectorEncoding;

public class LuceneFloatVectorCodec extends AbstractLuceneVectorFieldCodec<float[], float[]> {
	public LuceneFloatVectorCodec(VectorSimilarity vectorSimilarity, int dimension, Storage storage, Indexing indexing,
			float[] indexNullAsValue, HibernateSearchKnnVectorsFormat knnVectorsFormat) {
		super( vectorSimilarity, dimension, storage, indexing, indexNullAsValue, knnVectorsFormat );
	}

	@Override
	public float[] decode(IndexableField field) {
		KnnFloatVectorField byteVectorField = (KnnFloatVectorField) field;
		return byteVectorField.vectorValue();
	}

	@Override
	public float[] encode(float[] value) {
		return value;
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
	protected IndexableField toStoredField(String absoluteFieldPath, float[] encodedValue) {
		return new StoredField( absoluteFieldPath, Arrays.toString( encodedValue ) );
	}
}
