/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
