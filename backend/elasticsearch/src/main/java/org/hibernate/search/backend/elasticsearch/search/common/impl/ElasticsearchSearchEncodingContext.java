package org.hibernate.search.backend.elasticsearch.search.common.impl;

import java.util.function.BiFunction;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.common.ValueModel;

import tools.jackson.databind.JsonNode;

public interface ElasticsearchSearchEncodingContext<F> {

	<T> Function<T, JsonNode> encoder(ElasticsearchSearchIndexScope<?> scope,
			ElasticsearchSearchIndexValueFieldContext<F> field, Class<T> expectedType, ValueModel valueModel);

	JsonNode convertAndEncode(ElasticsearchSearchIndexScope<?> scope,
			ElasticsearchSearchIndexValueFieldContext<F> field,
			Object value, ValueModel valueModel,
			BiFunction<ElasticsearchFieldCodec<F>, F, JsonNode> encodeFunction);

	boolean isCompatibleWith(ElasticsearchSearchEncodingContext<?> other);
}
