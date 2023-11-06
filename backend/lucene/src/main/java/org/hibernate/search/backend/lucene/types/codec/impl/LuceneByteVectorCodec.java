/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchKnnVectorsFormat;
import org.hibernate.search.engine.backend.types.VectorSimilarity;

import org.apache.lucene.document.KnnByteVectorField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.VectorEncoding;
import org.apache.lucene.util.BytesRef;

public class LuceneByteVectorCodec extends AbstractLuceneVectorFieldCodec<byte[], byte[]> {
	public LuceneByteVectorCodec(VectorSimilarity vectorSimilarity, int dimension, Storage storage, Indexing indexing,
			byte[] indexNullAsValue, HibernateSearchKnnVectorsFormat knnVectorsFormat) {
		super( vectorSimilarity, dimension, storage, indexing, indexNullAsValue, knnVectorsFormat );
	}

	@Override
	public byte[] decode(IndexableField field) {
		return field.binaryValue().bytes;
	}

	@Override
	public byte[] encode(byte[] value) {
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
	protected IndexableField toStoredField(String absoluteFieldPath, byte[] encodedValue) {
		return new StoredField( absoluteFieldPath, new BytesRef( encodedValue ) );
	}
}
