/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.util.Objects;

import org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchKnnVectorsFormat;
import org.hibernate.search.engine.backend.types.VectorSimilarity;

import org.apache.lucene.codecs.KnnVectorsFormat;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.VectorEncoding;
import org.apache.lucene.index.VectorSimilarityFunction;

public abstract class AbstractLuceneVectorFieldCodec<F, E> implements LuceneStandardFieldCodec<F, E> {

	protected final FieldType fieldType;
	protected final VectorSimilarity vectorSimilarity;
	private final int dimension;
	private final Storage storage;
	private final Indexing indexing;
	private final F indexNullAsValue;
	private final HibernateSearchKnnVectorsFormat knnVectorsFormat;

	protected AbstractLuceneVectorFieldCodec(VectorSimilarity vectorSimilarity, int dimension, Storage storage,
			Indexing indexing, F indexNullAsValue, HibernateSearchKnnVectorsFormat knnVectorsFormat) {
		this.vectorSimilarity = vectorSimilarity;
		this.dimension = dimension;
		this.storage = storage;
		this.indexing = indexing;
		this.indexNullAsValue = indexNullAsValue;
		this.knnVectorsFormat = knnVectorsFormat;

		this.fieldType = new FieldType();
		this.fieldType.setVectorAttributes( dimension, vectorEncoding(), vectorSimilarityFunction( vectorSimilarity ) );
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

		E encodedValue = encode( value );

		if ( Indexing.ENABLED == indexing ) {
			documentBuilder.addField( createIndexField( absoluteFieldPath, value ) );
		}
		if ( Storage.ENABLED == storage ) {
			documentBuilder.addField( toStoredField( absoluteFieldPath, encodedValue ) );
		}
	}

	// TODO : vector : use byte array to represent data of a stored field instead of a string.
	//  for floats see Float#floatToIntBits or maybe ByteBuffer#putFloat
	protected abstract IndexableField toStoredField(String absoluteFieldPath, E encodedValue);

	@Override
	public boolean isCompatibleWith(LuceneFieldCodec<?> obj) {
		if ( this == obj ) {
			return true;
		}
		if ( getClass() != obj.getClass() ) {
			return false;
		}

		AbstractLuceneVectorFieldCodec<?, ?> other = (AbstractLuceneVectorFieldCodec<?, ?>) obj;

		return dimension == other.dimension
				&& vectorSimilarity == other.vectorSimilarity
				// not sure about this one :
				// TODO : vector : need to test with different formats to see if that'll work ...
				&& Objects.equals( knnVectorsFormat, other.knnVectorsFormat );
	}

	protected abstract IndexableField createIndexField(String absoluteFieldPath, F value);

	private static VectorSimilarityFunction vectorSimilarityFunction(VectorSimilarity vectorSimilarity) {
		switch ( vectorSimilarity ) {
			case L2:
				return VectorSimilarityFunction.EUCLIDEAN;
			case INNER_PRODUCT:
				return VectorSimilarityFunction.DOT_PRODUCT;
			case COSINE:
				return VectorSimilarityFunction.COSINE;
			default:
				throw new IllegalStateException( "unknown similarity function " + vectorSimilarity );
		}

	}

	protected abstract VectorEncoding vectorEncoding();

	@Override
	public KnnVectorsFormat knnVectorFormat() {
		return knnVectorsFormat;
	}
}
