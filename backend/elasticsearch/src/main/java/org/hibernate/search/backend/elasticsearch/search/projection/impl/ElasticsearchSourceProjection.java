package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Objects;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.reporting.impl.ElasticsearchSearchHints;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.util.json.JsonHandler;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class ElasticsearchSourceProjection<T> extends AbstractElasticsearchProjection<T>
		implements ElasticsearchSearchProjection.Extractor<ObjectNode, T> {

	private static final JsonArrayAccessor REQUEST_SOURCE_ACCESSOR = JsonAccessor.root().property( "_source" ).asArray();
	private static final JsonNode WILDCARD_ALL = JsonScalarConversions.toNode( "*" );

	private final JsonHandler<T> handler;
	private final JsonValidation validation;

	ElasticsearchSourceProjection(ElasticsearchSearchIndexScope<?> scope, JsonHandler<T> handler) {
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
				ElasticsearchProjectionTypeKeys.SOURCE,
				ElasticsearchSearchHints.INSTANCE.sourceProjectionNestingNotSupportedHint()
		);
		REQUEST_SOURCE_ACCESSOR.addElementIfAbsent( requestBody, WILDCARD_ALL );
		return this;
	}

	@Override
	public ObjectNode extract(ProjectionHitMapper<?> projectionHitMapper, ObjectNode hit,
			ObjectNode source, ProjectionExtractContext context) {
		JsonNode present = hit.get( "_source" );
		return present == null || present.isNull() ? null : JsonHandlerBridge.requireObject( present, "source projection" );
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, ObjectNode extractedData,
			ProjectionTransformContext context) {
		return JsonHandlerBridge.exportObject( handler, extractedData, "source projection", validation );
	}
}
