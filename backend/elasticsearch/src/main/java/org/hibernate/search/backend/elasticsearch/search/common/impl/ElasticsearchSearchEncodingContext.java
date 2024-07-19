/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.common.impl;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.common.ValueModel;

import com.google.gson.JsonElement;

public interface ElasticsearchSearchEncodingContext<F> {

	<T> Function<T, JsonElement> encoder(ElasticsearchSearchIndexScope<?> scope,
			ElasticsearchSearchIndexValueFieldContext<F> field, Class<T> expectedType, ValueModel valueModel);

	JsonElement convertAndEncode(ElasticsearchSearchIndexScope<?> scope,
			ElasticsearchSearchIndexValueFieldContext<F> field,
			Object value, ValueModel valueModel,
			BiFunction<ElasticsearchFieldCodec<F>, F, JsonElement> encodeFunction);

	boolean isCompatibleWith(ElasticsearchSearchEncodingContext<?> other);
}
