package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.reporting.impl.ElasticsearchSearchHints;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;

import tools.jackson.databind.node.ObjectNode;

class ElasticsearchScoreProjection extends AbstractElasticsearchProjection<Float>
		implements ElasticsearchSearchProjection.Extractor<Float, Float> {

	private static final JsonAccessor<Boolean> TRACK_SCORES_ACCESSOR = JsonAccessor.root().property( "track_scores" )
			.unwrapSingletonArrays().asBoolean();

	ElasticsearchScoreProjection(ElasticsearchSearchIndexScope<?> scope) {
		super( scope );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public Extractor<?, Float> request(ObjectNode requestBody, ProjectionRequestContext context) {
		context.checkNotNested(
				ProjectionTypeKeys.SCORE,
				ElasticsearchSearchHints.INSTANCE.scoreProjectionNestingNotSupportedHint()
		);
		TRACK_SCORES_ACCESSOR.set( requestBody, true );
		return this;
	}

	@Override
	public Float extract(ProjectionHitMapper<?> projectionHitMapper, ObjectNode hit,
			ObjectNode source, ProjectionExtractContext context) {
		return JsonScalarConversions.asFloat( hit.get( "_score" ) );
	}

	@Override
	public Float transform(LoadingResult<?> loadingResult, Float extractedData,
			ProjectionTransformContext context) {
		return extractedData;
	}

}
