package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.backend.elasticsearch.reporting.impl.ElasticsearchSearchHints;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.util.json.JsonHandler;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

import tools.jackson.databind.node.ObjectNode;

class ElasticsearchJsonHitProjection<T> extends AbstractElasticsearchProjection<T>
		implements ElasticsearchSearchProjection.Extractor<ObjectNode, T> {

	private final JsonHandler<T> handler;
	private final JsonValidation validation;

	ElasticsearchJsonHitProjection(ElasticsearchSearchIndexScope<?> scope, JsonHandler<T> handler) {
		super( scope );
		this.handler = Objects.requireNonNull( handler );
		this.validation = scope.userFacingCodec().validation();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, T> request(ObjectNode requestBody, ProjectionRequestContext context) {
		context.checkNotNested(
				ElasticsearchProjectionTypeKeys.JSON_HIT,
				ElasticsearchSearchHints.INSTANCE.jsonHitProjectionNestingNotSupportedHint()
		);
		return this;
	}

	@Override
	public ObjectNode extract(ProjectionHitMapper<?> projectionHitMapper, ObjectNode hit,
			ObjectNode source, ProjectionExtractContext context) {
		return hit;
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, ObjectNode extractedData,
			ProjectionTransformContext context) {
		return JsonHandlerBridge.exportObject( handler, extractedData, "JSON hit projection", validation );
	}
}
