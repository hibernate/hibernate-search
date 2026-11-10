package org.hibernate.search.backend.elasticsearch.search.common.impl;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.search.common.spi.SearchIndexValueFieldTypeContext;

import tools.jackson.databind.JsonNode;

public interface ElasticsearchSearchIndexValueFieldTypeContext<F>
		extends
		SearchIndexValueFieldTypeContext<ElasticsearchSearchIndexScope<?>, ElasticsearchSearchIndexValueFieldContext<F>, F> {

	JsonNode elasticsearchTypeAsJson();

	Optional<String> searchAnalyzerName();

	Optional<String> normalizerName();

	boolean hasNormalizerOnAtLeastOneIndex();

	ElasticsearchFieldCodec<F> codec();

}
