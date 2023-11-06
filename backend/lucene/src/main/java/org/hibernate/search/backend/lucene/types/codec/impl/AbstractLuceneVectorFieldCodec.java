/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.util.Objects;

import org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchKnnVectorsFormat;

import org.apache.lucene.codecs.KnnVectorsFormat;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.VectorEncoding;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.util.BytesRef;

public abstract class AbstractLuceneVectorFieldCodec<F> implements LuceneVectorFieldCodec<F> {

	protected final FieldType fieldType;
	protected final VectorSimilarityFunction vectorSimilarity;
	private final int dimension;
	private final Storage storage;
	private final Indexing indexing;
	private final F indexNullAsValue;
	private final HibernateSearchKnnVectorsFormat knnVectorsFormat;

	protected AbstractLuceneVectorFieldCodec(VectorSimilarityFunction vectorSimilarity, int dimension, Storage storage,
			Indexing indexing, F indexNullAsValue, HibernateSearchKnnVectorsFormat knnVectorsFormat) {
		this.vectorSimilarity = vectorSimilarity;
		this.dimension = dimension;
		this.storage = storage;
		this.indexing = indexing;
		this.indexNullAsValue = indexNullAsValue;
		this.knnVectorsFormat = knnVectorsFormat;

		this.fieldType = new FieldType();
		this.fieldType.setVectorAttributes( dimension, vectorEncoding(), vectorSimilarity );
		this.fieldType.freeze();
	}

	@Override
	public final void addToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, F value) {
		if ( value == null && indexNullAsValue != null ) {
			value = indexNullAsValue;
		}

		if ( value == null ) {
			return;
		}

		byte[] encodedValue = encode( value );

		if ( Indexing.ENABLED == indexing ) {
			documentBuilder.addField( createIndexField( absoluteFieldPath, value ) );
		}
		if ( Storage.ENABLED == storage ) {
			documentBuilder.addField( toStoredField( absoluteFieldPath, encodedValue ) );
		}
	}

	private IndexableField toStoredField(String absoluteFieldPath, byte[] encodedValue) {
		return new StoredField( absoluteFieldPath, new BytesRef( encodedValue ) );
	}

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}

		AbstractLuceneVectorFieldCodec<?> other = (AbstractLuceneVectorFieldCodec<?>) obj;

		return dimension == other.dimension
				&& vectorSimilarity == other.vectorSimilarity
				// not sure about this one :
				// TODO : vector : need to test with different formats to see if that'll work ...
				&& Objects.equals( knnVectorsFormat, other.knnVectorsFormat );
	}

	protected abstract IndexableField createIndexField(String absoluteFieldPath, F value);

	protected abstract VectorEncoding vectorEncoding();

	@Override
	public KnnVectorsFormat knnVectorFormat() {
		return knnVectorsFormat;
	}
}
