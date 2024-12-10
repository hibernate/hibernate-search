/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.common.impl;

import java.util.function.Function;

import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.search.common.ValueModel;

public interface LuceneSearchEncodingContext<F> {

	<E, T> Function<T, E> encoder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field,
			LuceneFieldCodec<F, E> codec, Class<T> expectedType,
			ValueModel valueModel);

	<E> E convertAndEncode(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field,
			LuceneFieldCodec<F, E> codec, Object value, ValueModel valueModel);


	boolean isCompatibleWith(LuceneSearchEncodingContext<?> other);
}
