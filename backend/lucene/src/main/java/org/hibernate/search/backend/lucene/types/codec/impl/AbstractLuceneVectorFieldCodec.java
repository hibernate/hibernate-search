/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.codec.impl.HibernateSearchKnnVectorsFormat;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.codecs.KnnVectorsFormat;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.VectorEncoding;
import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.util.BytesRef;

public abstract class AbstractLuceneVectorFieldCodec<F> implements LuceneVectorFieldCodec<F> {

	protected static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final FieldType fieldType;
	protected final VectorSimilarityFunction vectorSimilarity;
	private final int dimension;
	private final Storage storage;
	private final Indexing indexing;
	private final F indexNullAsValue;
	private final HibernateSearchKnnVectorsFormat knnVectorsFormat;
	private final Consumer<F> checkVectorConsumer;

	protected AbstractLuceneVectorFieldCodec(VectorSimilarityFunction vectorSimilarity, int dimension, Storage storage,
			Indexing indexing, F indexNullAsValue, HibernateSearchKnnVectorsFormat knnVectorsFormat,
			Consumer<F> checkVectorConsumer) {
		this.vectorSimilarity = vectorSimilarity;
		this.dimension = dimension;
		this.storage = storage;
		this.indexing = indexing;
		this.indexNullAsValue = indexNullAsValue;
		this.knnVectorsFormat = knnVectorsFormat;
		this.checkVectorConsumer = checkVectorConsumer;

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

		F encodedValue = encode( value );

		if ( Indexing.ENABLED == indexing ) {
			documentBuilder.addField( createIndexField( absoluteFieldPath, encodedValue ) );
		}
		if ( Storage.ENABLED == storage ) {
			documentBuilder.addField( toStoredField( absoluteFieldPath, toByteArray( encodedValue ) ) );
		}
	}

	private IndexableField toStoredField(String absoluteFieldPath, byte[] encodedValue) {
		return new StoredField( absoluteFieldPath, new BytesRef( encodedValue ) );
	}

	@Override
	public final F encode(F value) {
		checkVectorConsumer.accept( value );

		return value;
	}

	protected abstract byte[] toByteArray(F value);

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
				// to check ef construction and m
				&& Objects.equals( knnVectorsFormat, other.knnVectorsFormat );
	}

	protected abstract IndexableField createIndexField(String absoluteFieldPath, F value);

	protected abstract VectorEncoding vectorEncoding();

	@Override
	public KnnVectorsFormat knnVectorFormat() {
		return knnVectorsFormat;
	}

	@Override
	public int getConfiguredDimensions() {
		return dimension;
	}

	@Override
	public VectorSimilarityFunction getVectorSimilarity() {
		return vectorSimilarity;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" +
				"vectorSimilarity=" + vectorSimilarity +
				", dimension=" + dimension +
				", knnVectorsFormat=" + knnVectorsFormat +
				'}';
	}
}
