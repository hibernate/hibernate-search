/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;


import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;

/**
 * Defines how a given value will be encoded in the Lucene document and how it will be decoded.
 * <p>
 * Encodes values received from an {@link org.hibernate.search.engine.backend.document.IndexFieldReference} when indexing,
 * and returns decoded values to the hit extractor when projecting in a search query.
 *
 * @param <F> The field type as declared on
 */
public interface LuceneFieldCodec<F> {

	/**
	 * Encode the given value in the document by adding new fields to the Lucene document.
	 *
	 * @param documentBuilder The document builder.
	 * @param absoluteFieldPath The absolute path of the field.
	 * @param value The value to encode.
	 */
	void encode(LuceneDocumentBuilder documentBuilder, String absoluteFieldPath, F value);

	/**
	 * Extract the value from the given stored field.
	 * <p>
	 * Typically used in projections.
	 *
	 * @param field The document field. Never {@code null}.
	 * @return The decoded value.
	 */
	F decode(IndexableField field);

	/**
	 * Create a {@link Query} that will match every document in which the field with the given path appears.
	 *
	 * @param absoluteFieldPath The absolute path of the field.
	 * @return A Lucene {@link Query}.
	 */
	Query createExistsQuery(String absoluteFieldPath);

	/**
	 * Determine whether another codec is compatible with this one, i.e. whether it will encode/decode the information
	 * to/from the document in a compatible way.
	 *
	 * @param other Another {@link LuceneFieldCodec}, never {@code null}.
	 * @return {@code true} if the given codec is compatible. {@code false} otherwise, or when
	 * in doubt.
	 */
	boolean isCompatibleWith(LuceneFieldCodec<?> other);
}
