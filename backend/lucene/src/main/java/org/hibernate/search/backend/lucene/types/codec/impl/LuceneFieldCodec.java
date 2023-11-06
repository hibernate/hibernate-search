/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.apache.lucene.index.IndexableField;

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
	void addToDocument(LuceneDocumentContent documentBuilder, String absoluteFieldPath, F value);

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
	 * Determine whether the given codec provides an encoding that is compatible with this codec,
	 * i.e. whether its {@link #decode(IndexableField)}
	 * and {@link LuceneStandardFieldCodec#encode(Object)} methods behave the same way.
	 * <p>
	 * NOTE: {@link #addToDocument(LuceneDocumentContent, String, Object)} may behave differently,
	 * e.g. it may add docvalues while this codec does not.
	 * The behavior of {@link #addToDocument(LuceneDocumentContent, String, Object)}
	 * is considered irrelevant when checking the equivalence of encoding,
	 * because such differences should be accounted for through other ways
	 * (fields being assigned incompatible predicate factories, etc.).
	 *
	 * @param other Another {@link LuceneFieldCodec}, never {@code null}.
	 * @return {@code true} if the given codec is compatible. {@code false} otherwise, or when
	 * in doubt.
	 */
	boolean isCompatibleWith(LuceneFieldCodec<?> other);
}
