/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.util.function.BiConsumer;

import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;

import org.apache.lucene.document.Document;
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
	 * Assuming a field is stored, add the absolute paths of (sub-)fields that should be extracted from a document
	 * to later {@link #decode(Document, String)} the value of the field.
	 *
	 * @param absoluteFieldPath The path of the field whose value is assumed to be stored.
	 * @param collector A collector of absolute field paths to be extracted from the document.
	 * First argument is the absolute field path,
	 * second argument is the path of the nested document containing that field (or null if not relevant).
	 */
	default void contributeStoredFields(String absoluteFieldPath, String nestedDocumentPath, BiConsumer<String, String> collector) {
		collector.accept( absoluteFieldPath, nestedDocumentPath );
	}

	/**
	 * Extract the value from the Lucene document, typically used in projections.
	 *
	 * @param document The Lucene document.
	 * @param absoluteFieldPath The absolute path of the field.
	 * @return The decoded value.
	 */
	F decode(Document document, String absoluteFieldPath);

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
