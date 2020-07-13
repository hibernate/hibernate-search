/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.ScoreProjectionBuilder;

import com.google.gson.JsonObject;

class ElasticsearchScoreProjection extends AbstractElasticsearchProjection<Float, Float> {

	private static final JsonAccessor<Boolean> TRACK_SCORES_ACCESSOR = JsonAccessor.root().property( "track_scores" )
			.asBoolean();

	private ElasticsearchScoreProjection(ElasticsearchSearchContext searchContext) {
		super( searchContext );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	@Override
	public void request(JsonObject requestBody, SearchProjectionRequestContext context) {
		TRACK_SCORES_ACCESSOR.set( requestBody, true );
	}

	@Override
	public Float extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			SearchProjectionExtractContext context) {
		return hit.get( "_score" ).getAsFloat();
	}

	@Override
	public Float transform(LoadingResult<?> loadingResult, Float extractedData,
			SearchProjectionTransformContext context) {
		return extractedData;
	}

	static class Builder extends AbstractElasticsearchProjection.AbstractBuilder<Float>
			implements ScoreProjectionBuilder {

		private final ElasticsearchScoreProjection projection;

		Builder(ElasticsearchSearchContext searchContext) {
			super( searchContext );
			this.projection = new ElasticsearchScoreProjection( searchContext );
		}

		@Override
		public SearchProjection<Float> build() {
			return projection;
		}
	}
}
