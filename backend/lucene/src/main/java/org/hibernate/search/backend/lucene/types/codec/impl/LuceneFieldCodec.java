/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import java.util.Collections;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.backend.lucene.document.impl.LuceneDocumentBuilder;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * Defines how a given value will be encoded in the Lucene document and how it will be decoded.
 * <p>
 * Encodes values received from an {@link org.hibernate.search.engine.backend.document.IndexFieldAccessor} when indexing,
 * and returns decoded values to the hit extractor when projecting in a search query.
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
	 * If not empty, override the fields extracted from the document when doing a projection. Typically used to return
	 * the latitude and longitude fields when dealing with {@link GeoPoint}s.
	 *
	 * @return The set of stored fields overriding the default field.
	 */
	default Set<String> getOverriddenStoredFields() {
		return Collections.emptySet();
	}

	/**
	 * Extract the value from the Lucene document, typically used in projections.
	 *
	 * @param document The Lucene document.
	 * @param absoluteFieldPath The absolute path of the field.
	 * @return The decoded value.
	 */
	F decode(Document document, String absoluteFieldPath);

	// equals()/hashCode() needs to be implemented if the codec is not a singleton

	boolean equals(Object obj);

	int hashCode();
}
