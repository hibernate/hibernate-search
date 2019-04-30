/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

import org.apache.lucene.util.BytesRef;

/**
 * @param <F> The field type exposed to the mapper.
 */
public interface LuceneTextFieldCodec<F> extends LuceneStandardFieldCodec<F, String> {

	/**
	 * Normalize the given value.
	 * <p>
	 * Useful for predicates and sorts in particular.
	 *
	 * @param absoluteFieldPath The absolute path of the field.
	 * @param value The value to encode.
	 * @return the BytesRef normalized value
	 */
	BytesRef normalize(String absoluteFieldPath, String value);

}
