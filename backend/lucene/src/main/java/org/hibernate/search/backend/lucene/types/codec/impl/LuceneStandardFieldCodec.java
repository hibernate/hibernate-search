/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.codec.impl;

/**
 * @param <F> The field type exposed to the mapper.
 * @param <E> The encoded type. For example, for a {@code LocalDate} field this will be {@code Long}.
 */
public interface LuceneStandardFieldCodec<F, E> extends LuceneFieldCodec<F> {

	/**
	 * Encode the given value.
	 * <p>
	 * Useful for predicates and sorts in particular.
	 *
	 * @param value The value to encode.
	 */
	E encode(F value);

}
